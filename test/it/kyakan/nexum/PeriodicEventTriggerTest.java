package it.kyakan.nexum;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for PeriodicEventTrigger functionality
 */
class PeriodicEventTriggerTest {

    private enum State {
        IDLE, MONITORING, PROCESSING, DONE
    }

    private enum Event {
        START_MONITORING, POLL, PROCESS, STOP, COMPLETE
    }

    private Nexum<State, Event> stateMachine;
    private TimerServiceImpl timerService;
    private List<Event> firedEvents;
    private AtomicInteger pollCount;

    @BeforeEach
    void setUp() {
        timerService = new TimerServiceImpl();
        stateMachine = new Nexum<>(State.IDLE, timerService);
        firedEvents = new ArrayList<>();
        pollCount = new AtomicInteger(0);

        // Add a listener to track fired events
        stateMachine.addListener(new Nexum.NexumListener<State, Event>() {
            @Override
            public void onStateChanged(State fromState, State toState, Event event) {
                if (event != null) {
                    firedEvents.add(event);
                }
            }
        });
    }

    @Test
    void testBasicPeriodicEventTrigger() throws Exception {
        // Setup: Add transitions
        stateMachine
            .addTransition(State.IDLE, State.MONITORING, Event.START_MONITORING)
            .addTransition(State.MONITORING, State.DONE, Event.STOP);

        // Add periodic event trigger that fires POLL event periodically while in MONITORING state
        stateMachine.addPeriodicEventTrigger(State.MONITORING, Event.POLL, 100, 100, TimeUnit.MILLISECONDS);

        // Add state handler to count POLL events
        stateMachine.registerStateHandler(new AbstractStateHandler<State, Event>(State.MONITORING) {
            @Override
            public boolean handleEvent(NexumContext<State> context, Event event, Object eventData) {
                if (event == Event.POLL) {
                    pollCount.incrementAndGet();
                    return true; // Event handled, no state transition
                }
                return false;
            }
        });

        stateMachine.start();

        // Transition to MONITORING state
        stateMachine.fireEvent(Event.START_MONITORING);
        assertEquals(State.MONITORING, stateMachine.getCurrentState());

        // Trigger periodic events 3 times
        timerService.triggerPeriod(); // First POLL
        assertEquals(1, pollCount.get());
        
        timerService.triggerPeriod(); // Second POLL
        assertEquals(2, pollCount.get());
        
        timerService.triggerPeriod(); // Third POLL
        assertEquals(3, pollCount.get());

        // Stop monitoring
        stateMachine.fireEvent(Event.STOP);
        assertEquals(State.DONE, stateMachine.getCurrentState());

        // Trigger again - should not fire because we're no longer in MONITORING state
        int countBeforeTrigger = pollCount.get();
        timerService.triggerPeriod();
        assertEquals(countBeforeTrigger, pollCount.get(), "No more POLL events should be triggered after leaving MONITORING state");
    }

    @Test
    void testPeriodicEventTriggerWithGuard() throws Exception {
        AtomicInteger guardCheckCount = new AtomicInteger(0);

        // Setup transitions
        stateMachine
            .addTransition(State.IDLE, State.MONITORING, Event.START_MONITORING)
            .addTransition(State.MONITORING, State.DONE, Event.STOP);

        // Add periodic event trigger with guard
        stateMachine.addPeriodicEventTrigger(
            State.MONITORING,
            Event.POLL,
            50,
            50,
            TimeUnit.MILLISECONDS,
            (context, event, eventData) -> {
                guardCheckCount.incrementAndGet();
                // Only allow POLL events for the first 3 times
                return pollCount.get() < 3;
            }
        );

        // Add state handler to count POLL events
        stateMachine.registerStateHandler(new AbstractStateHandler<State, Event>(State.MONITORING) {
            @Override
            public boolean handleEvent(NexumContext<State> context, Event event, Object eventData) {
                if (event == Event.POLL) {
                    pollCount.incrementAndGet();
                    return true;
                }
                return false;
            }
        });

        stateMachine.start();
        stateMachine.fireEvent(Event.START_MONITORING);

        // Trigger periodic events - guard should allow first 3
        timerService.triggerPeriod(); // 1st - allowed
        assertEquals(1, pollCount.get());
        assertEquals(1, guardCheckCount.get());

        timerService.triggerPeriod(); // 2nd - allowed
        assertEquals(2, pollCount.get());
        assertEquals(2, guardCheckCount.get());

        timerService.triggerPeriod(); // 3rd - allowed
        assertEquals(3, pollCount.get());
        assertEquals(3, guardCheckCount.get());

        timerService.triggerPeriod(); // 4th - blocked by guard
        assertEquals(3, pollCount.get(), "Guard should have blocked the 4th POLL event");
        assertEquals(4, guardCheckCount.get(), "Guard should have been checked");

        timerService.triggerPeriod(); // 5th - blocked by guard
        assertEquals(3, pollCount.get(), "Guard should have blocked the 5th POLL event");
        assertEquals(5, guardCheckCount.get(), "Guard should have been checked again");
    }

    @Test
    void testPeriodicEventTriggerWithMaxOccurrences() throws Exception {
        // Setup transitions
        stateMachine
            .addTransition(State.IDLE, State.MONITORING, Event.START_MONITORING)
            .addTransition(State.MONITORING, State.DONE, Event.STOP);

        // Add periodic event trigger with max occurrences of 3
        stateMachine.addPeriodicEventTrigger(
            State.MONITORING,
            Event.POLL,
            50,
            50,
            TimeUnit.MILLISECONDS,
            null,
            3  // Max 3 occurrences
        );

        // Add state handler to count POLL events
        stateMachine.registerStateHandler(new AbstractStateHandler<State, Event>(State.MONITORING) {
            @Override
            public boolean handleEvent(NexumContext<State> context, Event event, Object eventData) {
                if (event == Event.POLL) {
                    pollCount.incrementAndGet();
                    return true;
                }
                return false;
            }
        });

        stateMachine.start();
        stateMachine.fireEvent(Event.START_MONITORING);

        // Trigger periodic events - should stop after 3
        timerService.triggerPeriod(); // 1st
        assertEquals(1, pollCount.get());

        timerService.triggerPeriod(); // 2nd
        assertEquals(2, pollCount.get());

        timerService.triggerPeriod(); // 3rd
        assertEquals(3, pollCount.get());

        timerService.triggerPeriod(); // 4th - should be blocked by maxOccurrences
        assertEquals(3, pollCount.get(), "Should have exactly 3 POLL events due to maxOccurrences");

        timerService.triggerPeriod(); // 5th - should still be blocked
        assertEquals(3, pollCount.get(), "Should still have exactly 3 POLL events");
    }

    @Test
    void testFluentApiPeriodicEventTrigger() throws Exception {
        // Setup using fluent API
        stateMachine
            .from(State.IDLE).to(State.MONITORING).on(Event.START_MONITORING)
            .from(State.MONITORING).to(State.DONE).on(Event.STOP)
            .inState(State.MONITORING)
                .trigger(Event.POLL)
                .every(100, TimeUnit.MILLISECONDS);

        // Add state handler
        stateMachine.registerStateHandler(new AbstractStateHandler<State, Event>(State.MONITORING) {
            @Override
            public boolean handleEvent(NexumContext<State> context, Event event, Object eventData) {
                if (event == Event.POLL) {
                    pollCount.incrementAndGet();
                    return true;
                }
                return false;
            }
        });

        stateMachine.start();
        stateMachine.fireEvent(Event.START_MONITORING);

        // Trigger periodic events
        timerService.triggerPeriod();
        assertEquals(1, pollCount.get());

        timerService.triggerPeriod();
        assertEquals(2, pollCount.get());

        timerService.triggerPeriod();
        assertEquals(3, pollCount.get());
    }

    @Test
    void testFluentApiWithGuardAndMaxOccurrences() throws Exception {
        AtomicInteger guardCheckCount = new AtomicInteger(0);

        // Setup using fluent API with guard and max occurrences
        stateMachine
            .from(State.IDLE).to(State.MONITORING).on(Event.START_MONITORING)
            .from(State.MONITORING).to(State.DONE).on(Event.STOP)
            .inState(State.MONITORING)
                .trigger(Event.POLL)
                .every(50, TimeUnit.MILLISECONDS)
                .withGuard((context, event, eventData) -> {
                    guardCheckCount.incrementAndGet();
                    return true;
                })
                .withMaxOccurrences(5);

        // Add state handler
        stateMachine.registerStateHandler(new AbstractStateHandler<State, Event>(State.MONITORING) {
            @Override
            public boolean handleEvent(NexumContext<State> context, Event event, Object eventData) {
                if (event == Event.POLL) {
                    pollCount.incrementAndGet();
                    return true;
                }
                return false;
            }
        });

        stateMachine.start();
        stateMachine.fireEvent(Event.START_MONITORING);

        // Trigger 5 times - all should succeed
        for (int i = 0; i < 5; i++) {
            timerService.triggerPeriod();
        }
        assertEquals(5, pollCount.get(), "Should have exactly 5 POLL events");
        assertEquals(5, guardCheckCount.get(), "Guard should have been checked 5 times");

        // Trigger again - should be blocked by maxOccurrences
        timerService.triggerPeriod();
        assertEquals(5, pollCount.get(), "Should still have exactly 5 POLL events");
    }

    @Test
    void testMultipleStatesWithPeriodicTriggers() throws Exception {
        AtomicInteger monitoringPolls = new AtomicInteger(0);
        AtomicInteger processingPolls = new AtomicInteger(0);

        // Setup transitions
        stateMachine
            .addTransition(State.IDLE, State.MONITORING, Event.START_MONITORING)
            .addTransition(State.MONITORING, State.PROCESSING, Event.PROCESS)
            .addTransition(State.PROCESSING, State.DONE, Event.COMPLETE);

        // Add periodic triggers for different states
        stateMachine
            .addPeriodicEventTrigger(State.MONITORING, Event.POLL, 50, 50, TimeUnit.MILLISECONDS)
            .addPeriodicEventTrigger(State.PROCESSING, Event.POLL, 50, 50, TimeUnit.MILLISECONDS);

        // Add state handlers
        stateMachine.registerStateHandler(new AbstractStateHandler<State, Event>(State.MONITORING) {
            @Override
            public boolean handleEvent(NexumContext<State> context, Event event, Object eventData) {
                if (event == Event.POLL) {
                    monitoringPolls.incrementAndGet();
                    return true;
                }
                return false;
            }
        });

        stateMachine.registerStateHandler(new AbstractStateHandler<State, Event>(State.PROCESSING) {
            @Override
            public boolean handleEvent(NexumContext<State> context, Event event, Object eventData) {
                if (event == Event.POLL) {
                    processingPolls.incrementAndGet();
                    return true;
                }
                return false;
            }
        });

        stateMachine.start();

        // Enter MONITORING state
        stateMachine.fireEvent(Event.START_MONITORING);
        assertEquals(State.MONITORING, stateMachine.getCurrentState());

        // Trigger periodic events in MONITORING state
        // Note: index 0 is for MONITORING trigger, index 1 is for PROCESSING trigger
        timerService.triggerPeriod(0);
        assertEquals(1, monitoringPolls.get());
        assertEquals(0, processingPolls.get());

        timerService.triggerPeriod(0);
        assertEquals(2, monitoringPolls.get());
        assertEquals(0, processingPolls.get());

        // Transition to PROCESSING state
        stateMachine.fireEvent(Event.PROCESS);
        assertEquals(State.PROCESSING, stateMachine.getCurrentState());

        // Trigger MONITORING periodic event - should not fire because we're not in MONITORING state
        timerService.triggerPeriod(0);
        assertEquals(2, monitoringPolls.get(), "MONITORING polls should not fire in PROCESSING state");

        // Trigger PROCESSING periodic event - should fire
        timerService.triggerPeriod(1);
        assertEquals(1, processingPolls.get());

        timerService.triggerPeriod(1);
        assertEquals(2, processingPolls.get());
    }

    @Test
    void testFluentApiWithMultipleEvents() throws Exception {
        AtomicInteger event1Count = new AtomicInteger(0);
        AtomicInteger event2Count = new AtomicInteger(0);

        // Setup using fluent API with multiple events
        stateMachine
            .from(State.IDLE).to(State.MONITORING).on(Event.START_MONITORING)
            .inState(State.MONITORING)
                .triggerAny(Event.POLL, Event.PROCESS)
                .every(100, TimeUnit.MILLISECONDS);

        // Add state handler
        stateMachine.registerStateHandler(new AbstractStateHandler<State, Event>(State.MONITORING) {
            @Override
            public boolean handleEvent(NexumContext<State> context, Event event, Object eventData) {
                if (event == Event.POLL) {
                    event1Count.incrementAndGet();
                    return true;
                } else if (event == Event.PROCESS) {
                    event2Count.incrementAndGet();
                    return true;
                }
                return false;
            }
        });

        stateMachine.start();
        stateMachine.fireEvent(Event.START_MONITORING);

        // Trigger first periodic event (POLL)
        timerService.triggerPeriod(0);
        assertEquals(1, event1Count.get());
        assertEquals(0, event2Count.get());

        // Trigger second periodic event (PROCESS)
        timerService.triggerPeriod(1);
        assertEquals(1, event1Count.get());
        assertEquals(1, event2Count.get());
    }
}