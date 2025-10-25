package it.kyakan.nexum;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Test class for loop transitions functionality
 */
public class LoopTransitionTest {

    private enum State {
        IDLE, RUNNING, PAUSED, ERROR
    }

    private enum Event {
        TICK, REFRESH, LOG, VALIDATE
    }

    private Nexum<State, Event> stateMachine;
    private List<String> executionLog;

    @BeforeEach
    public void setUp() {
        stateMachine = new Nexum<>(State.IDLE);
        executionLog = new ArrayList<>();
    }

    @Test
    public void testBasicLoopTransition() {
        // Add a loop transition for IDLE state on TICK event
        stateMachine.loop(State.IDLE).on(Event.TICK);
        stateMachine.start();

        assertEquals(State.IDLE, stateMachine.getCurrentState());

        // Fire TICK event - should remain in IDLE
        stateMachine.fireEvent(Event.TICK);
        assertEquals(State.IDLE, stateMachine.getCurrentState());

        // Fire TICK again - should still remain in IDLE
        stateMachine.fireEvent(Event.TICK);
        assertEquals(State.IDLE, stateMachine.getCurrentState());
    }

    @Test
    public void testLoopTransitionWithAction() {
        // Add a loop transition with action
        stateMachine.loop(State.IDLE)
            .on(Event.TICK)
            .withAction((ctx, event, data) -> executionLog.add("TICK action executed"));

        stateMachine.start();

        stateMachine.fireEvent(Event.TICK);
        assertEquals(State.IDLE, stateMachine.getCurrentState());
        assertEquals(1, executionLog.size());
        assertEquals("TICK action executed", executionLog.get(0));

        stateMachine.fireEvent(Event.TICK);
        assertEquals(2, executionLog.size());
    }

    @Test
    public void testLoopTransitionWithGuard() {
        final int[] counter = {0};

        // Add a loop transition with guard - only allow when counter < 3
        stateMachine.loop(State.IDLE)
            .on(Event.TICK)
            .withGuard((ctx, event, data) -> counter[0] < 3)
            .withAction((ctx, event, data) -> {
                counter[0]++;
                executionLog.add("Counter: " + counter[0]);
            });

        stateMachine.start();

        // First 3 ticks should work
        stateMachine.fireEvent(Event.TICK);
        assertEquals(1, counter[0]);
        stateMachine.fireEvent(Event.TICK);
        assertEquals(2, counter[0]);
        stateMachine.fireEvent(Event.TICK);
        assertEquals(3, counter[0]);

        // Fourth tick should fail (guard returns false)
        assertThrows(NexumException.class, () -> stateMachine.fireEvent(Event.TICK));
        assertEquals(3, counter[0]); // Counter should not increment
    }

    @Test
    public void testMultipleLoopTransitionsOnDifferentStates() {
        // Add loop transitions for multiple states
        stateMachine.loop(State.IDLE)
            .on(Event.TICK)
            .withAction((ctx, event, data) -> executionLog.add("IDLE TICK"));

        stateMachine.loop(State.RUNNING)
            .on(Event.TICK)
            .withAction((ctx, event, data) -> executionLog.add("RUNNING TICK"));

        // Add regular transition to move between states
        stateMachine.from(State.IDLE).to(State.RUNNING).on(Event.REFRESH);
        stateMachine.from(State.RUNNING).to(State.IDLE).on(Event.REFRESH);

        stateMachine.start();

        // Test loop in IDLE
        stateMachine.fireEvent(Event.TICK);
        assertEquals(State.IDLE, stateMachine.getCurrentState());
        assertEquals("IDLE TICK", executionLog.get(0));

        // Move to RUNNING
        stateMachine.fireEvent(Event.REFRESH);
        assertEquals(State.RUNNING, stateMachine.getCurrentState());

        // Test loop in RUNNING
        executionLog.clear();
        stateMachine.fireEvent(Event.TICK);
        assertEquals(State.RUNNING, stateMachine.getCurrentState());
        assertEquals("RUNNING TICK", executionLog.get(0));
    }

    @Test
    public void testLoopTransitionWithMultipleEvents() {
        // Add loop transition for multiple events
        stateMachine.loop(State.IDLE)
            .onAny(Event.TICK, Event.REFRESH, Event.LOG)
            .withAction((ctx, event, data) -> executionLog.add("Event: " + event));

        stateMachine.start();

        stateMachine.fireEvent(Event.TICK);
        assertEquals(State.IDLE, stateMachine.getCurrentState());
        assertEquals("Event: TICK", executionLog.get(0));

        stateMachine.fireEvent(Event.REFRESH);
        assertEquals(State.IDLE, stateMachine.getCurrentState());
        assertEquals("Event: REFRESH", executionLog.get(1));

        stateMachine.fireEvent(Event.LOG);
        assertEquals(State.IDLE, stateMachine.getCurrentState());
        assertEquals("Event: LOG", executionLog.get(2));
    }

    @Test
    public void testLoopTransitionForAllStates() {
        // Register state handlers for all states
        for (State state : State.values()) {
            stateMachine.registerStateHandler(new AbstractStateHandler<State, Event>(state) {});
        }

        // Add loop transition for all states (empty array means all registered states)
        stateMachine.loop()
            .on(Event.LOG)
            .withAction((ctx, event, data) -> executionLog.add("LOG in " + ctx.getCurrentState()));

        // Add regular transitions to move between states
        stateMachine.from(State.IDLE).to(State.RUNNING).on(Event.REFRESH);
        stateMachine.from(State.RUNNING).to(State.PAUSED).on(Event.REFRESH);
        stateMachine.from(State.PAUSED).to(State.ERROR).on(Event.REFRESH);

        stateMachine.start();

        // Test loop in IDLE
        stateMachine.fireEvent(Event.LOG);
        assertEquals(State.IDLE, stateMachine.getCurrentState());
        assertEquals("LOG in IDLE", executionLog.get(0));

        // Move to RUNNING and test loop
        stateMachine.fireEvent(Event.REFRESH);
        stateMachine.fireEvent(Event.LOG);
        assertEquals(State.RUNNING, stateMachine.getCurrentState());
        assertEquals("LOG in RUNNING", executionLog.get(1));

        // Move to PAUSED and test loop
        stateMachine.fireEvent(Event.REFRESH);
        stateMachine.fireEvent(Event.LOG);
        assertEquals(State.PAUSED, stateMachine.getCurrentState());
        assertEquals("LOG in PAUSED", executionLog.get(2));

        // Move to ERROR and test loop
        stateMachine.fireEvent(Event.REFRESH);
        stateMachine.fireEvent(Event.LOG);
        assertEquals(State.ERROR, stateMachine.getCurrentState());
        assertEquals("LOG in ERROR", executionLog.get(3));
    }

    @Test
    public void testLoopTransitionUsingLoopEventList() {
        // Create loop events using LoopEvent class
        List<LoopEvent<State, Event>> loopEvents = new ArrayList<>();
        loopEvents.add(new LoopEvent<>(Event.TICK, null, 
            (ctx, event, data) -> executionLog.add("TICK")));
        loopEvents.add(new LoopEvent<>(Event.REFRESH, 
            (ctx, event, data) -> executionLog.size() < 5, 
            (ctx, event, data) -> executionLog.add("REFRESH")));

        // Add loop transitions using the list
        stateMachine.addLoopTransitions(List.of(State.IDLE, State.RUNNING), loopEvents);

        stateMachine.start();

        // Test TICK (no guard)
        stateMachine.fireEvent(Event.TICK);
        assertEquals(State.IDLE, stateMachine.getCurrentState());
        assertEquals("TICK", executionLog.get(0));

        // Test REFRESH (with guard)
        stateMachine.fireEvent(Event.REFRESH);
        assertEquals("REFRESH", executionLog.get(1));
    }

    @Test
    public void testLoopTransitionChaining() {
        // Test fluent API chaining
        stateMachine
            .loop(State.IDLE)
                .on(Event.TICK)
                .withAction((ctx, event, data) -> executionLog.add("IDLE TICK"))
            .loop(State.RUNNING)
                .on(Event.TICK)
                .withAction((ctx, event, data) -> executionLog.add("RUNNING TICK"))
            .from(State.IDLE).to(State.RUNNING).on(Event.REFRESH);

        stateMachine.start();

        stateMachine.fireEvent(Event.TICK);
        assertEquals("IDLE TICK", executionLog.get(0));

        stateMachine.fireEvent(Event.REFRESH);
        stateMachine.fireEvent(Event.TICK);
        assertEquals("RUNNING TICK", executionLog.get(1));
    }

    @Test
    public void testLoopTransitionWithStateHandlers() {
        final int[] enterCount = {0};
        final int[] exitCount = {0};

        stateMachine.registerStateHandler(new AbstractStateHandler<State, Event>(State.IDLE) {
            @Override
            public void onEnter(NexumContext<State> context, State fromState, Event event) {
                enterCount[0]++;
                executionLog.add("onEnter from " + fromState + " via " + event);
            }

            @Override
            public void onExit(NexumContext<State> context, State toState, Event event) {
                exitCount[0]++;
                executionLog.add("onExit to " + toState + " via " + event);
            }
        });

        stateMachine.loop(State.IDLE)
            .on(Event.TICK)
            .withAction((ctx, event, data) -> executionLog.add("Loop action executed"));
        
        stateMachine.start();

        // After start, onEnter should have been called once
        assertEquals(1, enterCount[0]);
        assertEquals(0, exitCount[0]);

        // Fire loop transition
        stateMachine.fireEvent(Event.TICK);

        // Loop transitions trigger exit and enter handlers since they go through executeTransition
        assertEquals(1, exitCount[0], "onExit should be called once for loop transition");
        assertEquals(2, enterCount[0], "onEnter should be called twice (start + loop)");
        assertEquals(State.IDLE, stateMachine.getCurrentState());
        
        // Verify the action was executed
        assertTrue(executionLog.contains("Loop action executed"));
        
        // Verify the handlers were called with correct parameters
        assertTrue(executionLog.contains("onExit to IDLE via TICK"));
        assertTrue(executionLog.contains("onEnter from IDLE via TICK"));
    }

    @Test
    public void testLoopTransitionAll() {
        stateMachine
            .loop(State.IDLE)
                .on(Event.TICK)
                .withAction((ctx, event, data) -> executionLog.add("IDLE TICK"))
            .loop()
                .onAny(Event.REFRESH, Event.LOG)
                .withAction((ctx, event, data) -> executionLog.add("RUNNING ALL"))
            ;
        stateMachine.start();

        stateMachine.fireEvent(Event.TICK);
        assertEquals("IDLE TICK", executionLog.get(0));

        stateMachine.fireEvent(Event.REFRESH);
        assertEquals("RUNNING ALL", executionLog.get(1));
        stateMachine.fireEvent(Event.LOG);
        assertEquals("RUNNING ALL", executionLog.get(1));

        assertThrows(NexumException.class, () -> stateMachine.fireEvent(Event.VALIDATE));

    }

}