package it.kyakan.nexum;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ScheduledTransitionTest {

    private enum TestState {
        STATE_A, STATE_B, STATE_C
    }

    private enum TestEvent {
        EVENT_X, EVENT_Y
    }

    private Nexum<TestState, TestEvent> stateMachine;
    private TimerServiceImpl timerService;

    @BeforeEach
    public void setUp() {
        timerService = new TimerServiceImpl();
        stateMachine = new Nexum<>(TestState.STATE_A, timerService);
    }

    @Test
    public void testScheduledTransition() {
        AtomicBoolean eventTriggered = new AtomicBoolean(false);
        // Add a scheduled transition from STATE_A to STATE_B after 100ms
        stateMachine.addScheduledTransition(TestState.STATE_A, TestState.STATE_B, TestEvent.EVENT_X, 100, TimeUnit.MILLISECONDS, null, (a,b,c) -> {
                eventTriggered.set(true);
            });

        // Start the state machine
        stateMachine.start();

        // Verify that the timer service was called to schedule the transition
        assertTrue(timerService.SCHEDULED_COUNT == 1, "Scheduled transition should be scheduled");
        assertTrue(timerService.EXECUTED_COUNT == 0, "Scheduled transition should be not executed");
        assertFalse(eventTriggered.get(), "Scheduled transition should be not executed");

        timerService.trigger();

        assertTrue(timerService.SCHEDULED_COUNT == 1, "Scheduled transition should be scheduled");
        assertTrue(timerService.EXECUTED_COUNT == 1, "Scheduled transition should be executed");
        assertTrue(eventTriggered.get(), "Action transition should be executed");
    }

    @Test
    public void testCancelScheduledTransition() {
        // Flag to check if the event was triggered
        AtomicBoolean eventTriggered = new AtomicBoolean(false);


        // Add a scheduled transition from STATE_A to STATE_B after 100ms
        stateMachine
            .addScheduledTransition(TestState.STATE_A, TestState.STATE_B, TestEvent.EVENT_X, 100, TimeUnit.MILLISECONDS, null, (a,b,c) -> {
                eventTriggered.set(true);
            })
            .addTransition(TestState.STATE_A, TestState.STATE_C, TestEvent.EVENT_Y)
            .start();

        assertTrue(timerService.SCHEDULED_COUNT == 1, "Scheduled transition should be not scheduled");
        assertTrue(timerService.EXECUTED_COUNT == 0, "Scheduled transition should be not executed");
        assertFalse(eventTriggered.get(), "Scheduled transition should be not executed");

        stateMachine.fireEvent(TestEvent.EVENT_Y);

        timerService.trigger();

        assertTrue(timerService.SCHEDULED_COUNT == 1, "Scheduled transition should be not scheduled");
        assertTrue(timerService.EXECUTED_COUNT == 1, "Scheduled transition should be not executed");
        assertFalse(eventTriggered.get(), "Action transition should be not executed");
    }

}