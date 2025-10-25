package it.kyakan.nexum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ScheduledTransitionTest {

    private enum TestState {
        STATE_A, STATE_B, STATE_C
    }

    private enum TestEvent {
        EVENT_X, EVENT_Y, EVENT_Z
    }

    private Nexum<TestState, TestEvent> nexum;
    private TimerServiceImpl timerService;

    @BeforeEach
    public void setUp() {
        timerService = new TimerServiceImpl();
        nexum = new Nexum<>(TestState.STATE_A, timerService);
    }

    @Test
    public void testScheduledTransition() {
        AtomicBoolean eventTriggered = new AtomicBoolean(false);
        // Add a scheduled transition from STATE_A to STATE_B after 100ms
        nexum.addScheduledTransition(TestState.STATE_A, TestState.STATE_B, TestEvent.EVENT_X, 100, TimeUnit.MILLISECONDS, null, (a,b,c) -> {
                eventTriggered.set(true);
            });

        // Start the state machine
        nexum.start();

        // Verify that the timer service was called to schedule the transition
        assertEquals(timerService.SCHEDULED_COUNT, 1, "Scheduled transition should be scheduled");
        assertEquals(timerService.EXECUTED_COUNT, 0, "Scheduled transition should be not executed");
        assertFalse(eventTriggered.get(), "Scheduled transition should be not executed");

        timerService.triggerPeriod(0);

        assertEquals(timerService.SCHEDULED_COUNT, 1, "Scheduled transition should be scheduled");
        assertEquals(timerService.EXECUTED_COUNT, 1, "Scheduled transition should be executed");
        assertTrue(eventTriggered.get(), "Action transition should be executed");
    }

    @Test
    public void testCancelScheduledTransition() {
        // Flag to check if the event was triggered
        AtomicBoolean eventTriggered = new AtomicBoolean(false);


        // Add a scheduled transition from STATE_A to STATE_B after 100ms
        nexum
            .addScheduledTransition(TestState.STATE_A, TestState.STATE_B, TestEvent.EVENT_X, 100, TimeUnit.MILLISECONDS, null, (a,b,c) -> {
                eventTriggered.set(true);
            })
            .addTransition(TestState.STATE_A, TestState.STATE_C, TestEvent.EVENT_Y)
            .start();

        assertEquals(timerService.SCHEDULED_COUNT, 1, "Scheduled transition should be not scheduled");
        assertEquals(timerService.EXECUTED_COUNT, 0, "Scheduled transition should be not executed");
        assertFalse(eventTriggered.get(), "Scheduled transition should be not executed");

        nexum.fireEvent(TestEvent.EVENT_Y);

        timerService.triggerPeriod(0);

        assertEquals(timerService.SCHEDULED_COUNT, 1, "Scheduled transition should be not scheduled");
        assertEquals(timerService.EXECUTED_COUNT, 1, "Scheduled transition should be not executed");
        assertFalse(eventTriggered.get(), "Action transition should be not executed");
    }


    @Test
    public void testMultipleScheduledTransition() {
        // Flag to check if the event was triggered
        AtomicBoolean eventTriggered = new AtomicBoolean(false);
        AtomicBoolean eventTriggeredB = new AtomicBoolean(false);


        // Add a scheduled transition from STATE_A to STATE_B after 100ms
        nexum
            .addScheduledTransition(TestState.STATE_A, TestState.STATE_A, TestEvent.EVENT_X, 100, TimeUnit.MILLISECONDS, null, (a,b,c) -> {
                eventTriggered.set(true);
            })
            .addScheduledTransition(TestState.STATE_A, TestState.STATE_A, TestEvent.EVENT_Y, 1000, TimeUnit.MILLISECONDS, null, (a,b,c) -> {
                eventTriggeredB.set(true);
            })
            .start();

        assertEquals(timerService.SCHEDULED_COUNT, 2, "Scheduled transition should be not scheduled");
        assertEquals(timerService.EXECUTED_COUNT, 0, "Scheduled transition should be not executed");
        assertFalse(eventTriggered.get(), "Scheduled transition should be not executed");
        assertFalse(eventTriggeredB.get(), "Scheduled transition should be not executed");

        timerService.triggerPeriod(0);
        assertEquals(timerService.SCHEDULED_COUNT, 2, "Scheduled transition should be scheduled");
        assertEquals(timerService.EXECUTED_COUNT, 1, "Scheduled transition should be executed");
        assertTrue(eventTriggered.get(), "Scheduled transition should be executed");
        assertFalse(eventTriggeredB.get(), "Scheduled transition should be not executed");
        timerService.triggerPeriod(0);
        assertEquals(timerService.SCHEDULED_COUNT, 2, "Scheduled transition should be scheduled");
        assertEquals(timerService.EXECUTED_COUNT, 2, "Scheduled transition should be executed");
        assertTrue(eventTriggered.get(), "Scheduled transition should be executed");
        assertFalse(eventTriggeredB.get(), "Scheduled transition should be not executed");
        timerService.triggerPeriod(0);
        assertEquals(timerService.SCHEDULED_COUNT, 2, "Scheduled transition should be scheduled");
        assertEquals(timerService.EXECUTED_COUNT, 3, "Scheduled transition should be executed");
        assertTrue(eventTriggered.get(), "Scheduled transition should be executed");
        assertFalse(eventTriggeredB.get(), "Scheduled transition should be not executed");
        timerService.triggerPeriod(0);
        assertEquals(timerService.SCHEDULED_COUNT, 2, "Scheduled transition should be scheduled");
        assertEquals(timerService.EXECUTED_COUNT, 4, "Scheduled transition should be executed");
        assertTrue(eventTriggered.get(), "Scheduled transition should be executed");
        assertFalse(eventTriggeredB.get(), "Scheduled transition should be not executed");

        eventTriggered.set(false);
        timerService.triggerPeriod(1);
        assertEquals(timerService.SCHEDULED_COUNT, 2, "Scheduled transition should be scheduled");
        assertEquals(timerService.EXECUTED_COUNT, 5, "Scheduled transition should be executed");
        assertFalse(eventTriggered.get(), "Scheduled transition should be not executed");
        assertTrue(eventTriggeredB.get(), "Scheduled transition should be executed");
        timerService.triggerPeriod(0);
        assertEquals(timerService.SCHEDULED_COUNT, 2, "Scheduled transition should be scheduled");
        assertEquals(timerService.EXECUTED_COUNT, 6, "Scheduled transition should be executed");
        assertTrue(eventTriggered.get(), "Scheduled transition should be executed");
        assertTrue(eventTriggeredB.get(), "Scheduled transition should be executed");
    }

    @Test
    @DisplayName("Should add multiple scheduled transitions from array of source states")
    void testArrayScheduledTransitions() throws Exception {
        
        AtomicBoolean eventTriggered = new AtomicBoolean(false);
        AtomicBoolean eventTriggeredB = new AtomicBoolean(false);
        
        nexum
                .addListener((from, to, event) -> {
                })
                .addScheduledTransition(new TestState[]{ TestState.STATE_A, TestState.STATE_B }, TestState.STATE_C, TestEvent.EVENT_X, 100, TimeUnit.MILLISECONDS, null, (a,b,c) -> {
                    if (a.getCurrentState() == TestState.STATE_A) {
                        eventTriggered.set(true);
                    } else if (a.getCurrentState() == TestState.STATE_B) {
                        eventTriggeredB.set(true);
                    }
                })
                .start();

        // Verify scheduled transition was registered
        assertEquals(1, timerService.SCHEDULED_COUNT);
        assertFalse(eventTriggered.get());
        assertFalse(eventTriggeredB.get());
        
        // Manually trigger the scheduled transition
        timerService.triggerPeriod(0);
        
        assertEquals(1, timerService.SCHEDULED_COUNT);
        assertTrue(eventTriggered.get());
        assertFalse(eventTriggeredB.get());
        assertEquals(TestState.STATE_C, nexum.getCurrentState());

        nexum.reset(TestState.STATE_B);
        eventTriggered.set(false);

        assertEquals(2, timerService.SCHEDULED_COUNT);
        assertFalse(eventTriggered.get());
        assertFalse(eventTriggeredB.get());

        timerService.triggerPeriod(1);
        assertEquals(2, timerService.SCHEDULED_COUNT);
        assertFalse(eventTriggered.get());
        assertTrue(eventTriggeredB.get());


    }

    @Test
    @DisplayName("Should add multiple scheduled transitions from array with guard")
    void testArrayScheduledTransitionsWithGuard() throws Exception {
        
        final boolean[] transitioned = { false };
        
        nexum
                .addListener((from, to, event) -> {
                    if (to == TestState.STATE_C) {
                        transitioned[0] = true;
                    }
                })
                .addScheduledTransition(new TestState[]{ TestState.STATE_A, TestState.STATE_B }, TestState.STATE_C, TestEvent.EVENT_X,
                        100, TimeUnit.MILLISECONDS,
                        (ctx, event, data) -> true)
                .start();

        // Verify scheduled transition was registered
        assertEquals(1, timerService.SCHEDULED_COUNT);
        assertFalse(transitioned[0]);
        
        // Manually trigger the scheduled transition
        timerService.triggerPeriod(0);
        
        assertTrue(transitioned[0]);
        assertEquals(TestState.STATE_C, nexum.getCurrentState());
    }

    @Test
    @DisplayName("Should add multiple scheduled transitions from array with guard and action")
    void testArrayScheduledTransitionsWithGuardAndAction() throws Exception {
        
        final boolean[] actionExecuted = { false };
        
        nexum
                .addScheduledTransition(new TestState[]{ TestState.STATE_A, TestState.STATE_B }, TestState.STATE_C, TestEvent.EVENT_X,
                        100, TimeUnit.MILLISECONDS,
                        (ctx, event, data) -> true,
                        (ctx, event, data) -> {
                            actionExecuted[0] = true;
                            ctx.put("scheduled", true);
                        })
                .start();

        // Verify scheduled transition was registered
        assertEquals(1, timerService.SCHEDULED_COUNT);
        assertFalse(actionExecuted[0]);
        
        // Manually trigger the scheduled transition
        timerService.triggerPeriod(0);
        
        assertTrue(actionExecuted[0]);
        assertEquals(TestState.STATE_C, nexum.getCurrentState());
        assertTrue(nexum.getContext().contains("scheduled"));
    }
    @Test
    @DisplayName("Should add multiple scheduled transitions from array with guard and action")
    void testArrayScheduledTransitionsWithGuardFalseAndAction() throws Exception {
        
        final boolean[] actionExecuted = { false };
        
        nexum
            .addScheduledTransition(new TestState[]{ TestState.STATE_A, TestState.STATE_B }, TestState.STATE_C, TestEvent.EVENT_X,
                    100, TimeUnit.MILLISECONDS,
                    (ctx, event, data) -> false,
                    (ctx, event, data) -> {
                        actionExecuted[0] = true;
                        ctx.put("scheduled", true);
                    })
            .addTransition(TestState.STATE_A, TestState.STATE_B, TestEvent.EVENT_X)
            .start();

        // Verify scheduled transition was registered
        assertEquals(1, timerService.SCHEDULED_COUNT);
        assertFalse(actionExecuted[0]);
        
        // Manually trigger the scheduled transition
        timerService.triggerPeriod(0);
        
        assertFalse(actionExecuted[0]);
        assertEquals(TestState.STATE_B, nexum.getCurrentState());
        assertFalse(nexum.getContext().contains("scheduled"));
    }

    @Test
    @DisplayName("Should add scheduled transitions with multiple events from single source state")
    void testScheduledTransitionsWithMultipleEvents() throws Exception {
        AtomicBoolean eventXTriggered = new AtomicBoolean(false);
        AtomicBoolean eventYTriggered = new AtomicBoolean(false);
        
        nexum
                .addScheduledTransition(TestState.STATE_A, TestState.STATE_B, 
                        new TestEvent[]{ TestEvent.EVENT_X, TestEvent.EVENT_Y }, 
                        100, TimeUnit.MILLISECONDS, null, (ctx, event, data) -> {
                            if (event == TestEvent.EVENT_X) {
                                eventXTriggered.set(true);
                            } else if (event == TestEvent.EVENT_Y) {
                                eventYTriggered.set(true);
                            }
                        })
                .start();

        // Verify both scheduled transitions were registered
        assertEquals(2, timerService.SCHEDULED_COUNT);
        assertFalse(eventXTriggered.get());
        assertFalse(eventYTriggered.get());
        
        // Trigger first scheduled transition (EVENT_X)
        timerService.triggerPeriod(0);
        assertTrue(eventXTriggered.get());
        assertFalse(eventYTriggered.get());
        assertEquals(TestState.STATE_B, nexum.getCurrentState());
    }

    @Test
    @DisplayName("Should add scheduled transitions with multiple events and guard")
    void testScheduledTransitionsWithMultipleEventsAndGuard() throws Exception {
        final int[] transitionCount = { 0 };
        
        nexum
                .addScheduledTransition(TestState.STATE_A, TestState.STATE_B, 
                        new TestEvent[]{ TestEvent.EVENT_X, TestEvent.EVENT_Y }, 
                        100, TimeUnit.MILLISECONDS,
                        (ctx, event, data) -> true,
                        (ctx, event, data) -> transitionCount[0]++)
                .start();

        assertEquals(2, timerService.SCHEDULED_COUNT);
        assertEquals(0, transitionCount[0]);
        
        // Trigger first scheduled transition
        timerService.triggerPeriod(0);
        assertEquals(1, transitionCount[0]);
        assertEquals(TestState.STATE_B, nexum.getCurrentState());
    }

    @Test
    @DisplayName("Should add scheduled transitions from multiple states with multiple events")
    void testScheduledTransitionsMultipleStatesMultipleEvents() throws Exception {
        final int[] transitionCount = { 0 };
        
        nexum
                .addScheduledTransition(
                        new TestState[]{ TestState.STATE_A, TestState.STATE_B },
                        TestState.STATE_C,
                        new TestEvent[]{ TestEvent.EVENT_X, TestEvent.EVENT_Y },
                        100, TimeUnit.MILLISECONDS,
                        null,
                        (ctx, event, data) -> transitionCount[0]++)
                .start();

        // Should create 2 states × 2 events = 4 scheduled transitions
        assertEquals(2, timerService.SCHEDULED_COUNT);
        assertEquals(0, transitionCount[0]);
        
        // Trigger the last scheduled transition (most recent one stored)
        timerService.triggerPeriod(1);
        assertEquals(1, transitionCount[0]);
        assertEquals(TestState.STATE_C, nexum.getCurrentState());
    }

    @Test
    @DisplayName("Should add scheduled transitions from multiple states with multiple events and guard")
    void testScheduledTransitionsMultipleStatesMultipleEventsWithGuard() throws Exception {
        final int[] transitionCount = { 0 };
        
        nexum
                .addScheduledTransition(
                        new TestState[]{ TestState.STATE_A, TestState.STATE_B },
                        TestState.STATE_B,
                        new TestEvent[]{ TestEvent.EVENT_X, TestEvent.EVENT_Y },
                        100, TimeUnit.MILLISECONDS,
                        (ctx, event, data) -> true,
                        (ctx, event, data) -> {
                            transitionCount[0]++;
                            ctx.put("lastEvent", event.toString());
                        })
                .start();

        assertEquals(2, timerService.SCHEDULED_COUNT);
        assertEquals(0, transitionCount[0]);
        assertEquals(TestState.STATE_A, nexum.getCurrentState());
        
        timerService.triggerPeriod(0);
        timerService.triggerPeriod(1);

        assertEquals(4, timerService.SCHEDULED_COUNT);
        assertEquals(1, transitionCount[0]);
        assertEquals(TestState.STATE_B, nexum.getCurrentState());
        assertTrue(nexum.getContext().contains("lastEvent"));
        
        timerService.triggerPeriod(0);
        timerService.triggerPeriod(1);

        assertEquals(4, timerService.SCHEDULED_COUNT);
        assertEquals(1, transitionCount[0]);
        assertEquals(TestState.STATE_B, nexum.getCurrentState());
        assertTrue(nexum.getContext().contains("lastEvent"));


        timerService.triggerPeriod(3);

        assertEquals(2, transitionCount[0]);
        assertEquals(TestState.STATE_B, nexum.getCurrentState());
        assertTrue(nexum.getContext().contains("lastEvent"));
    }

    @Test
    @DisplayName("Should cancel scheduled transitions with multiple events when state changes")
    void testCancelScheduledTransitionsWithMultipleEvents() throws Exception {
        AtomicBoolean scheduledTriggered = new AtomicBoolean(false);
        
        nexum
                .addScheduledTransition(TestState.STATE_A, TestState.STATE_B,
                        new TestEvent[]{ TestEvent.EVENT_X, TestEvent.EVENT_Y },
                        100, TimeUnit.MILLISECONDS, null, (ctx, event, data) -> {
                            scheduledTriggered.set(true);
                        })
                .addTransition(TestState.STATE_A, TestState.STATE_C, TestEvent.EVENT_Z)
                .start();

        assertEquals(2, timerService.SCHEDULED_COUNT);
        assertFalse(scheduledTriggered.get());
        assertEquals(TestState.STATE_A, nexum.getCurrentState());
        
        // Fire immediate transition to change state (using EVENT_Y to avoid conflict)
        nexum.fireEvent(TestEvent.EVENT_Z);
        assertEquals(TestState.STATE_C, nexum.getCurrentState());
        assertFalse(scheduledTriggered.get());
        
        // Try to trigger scheduled transitions (should be cancelled)
        timerService.triggerPeriod();
        assertFalse(scheduledTriggered.get());
        assertEquals(TestState.STATE_C, nexum.getCurrentState());
    }

}