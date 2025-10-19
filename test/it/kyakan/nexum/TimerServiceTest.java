package it.kyakan.nexum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TimerServiceTest {

    private enum TestState {
        IDLE, RUNNING, STOPPED
    }

    private enum TestEvent {
        START, STOP, TIMEOUT
    }

    private Nexum<TestState, TestEvent> stateMachine;
    private TimerServiceImpl testTimerService;

    @BeforeEach
    public void setUp() {
        testTimerService = new TimerServiceImpl();
        stateMachine = new Nexum<>(TestState.IDLE, testTimerService);

        // Define transitions
        stateMachine.addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START);
        stateMachine.addTransition(TestState.RUNNING, TestState.STOPPED, TestEvent.STOP);
        stateMachine.addTransition(TestState.RUNNING, TestState.STOPPED, TestEvent.TIMEOUT);

        stateMachine.start();
    }

    @Test
    public void testScheduleEventWithoutTimerService() {
        Nexum<TestState, TestEvent> smWithoutTimer = new Nexum<>(TestState.IDLE);
        assertThrows(IllegalStateException.class, () -> {
            smWithoutTimer.scheduleEvent(TestEvent.START, 1, TimeUnit.SECONDS);
        });
    }

    @Test
    public void testScheduleEventWithTimerService() {
        stateMachine.scheduleEvent(TestEvent.START, 1, TimeUnit.SECONDS);

        // Execute the scheduled task immediately
        testTimerService.trigger(0);

        // Verify the state changed to RUNNING
        assertEquals(TestState.RUNNING, stateMachine.getCurrentState());
    }

    @Test
    public void testSchedulePeriodicEventWithTimerService() {
        // First, transition to RUNNING state
        stateMachine.fireEvent(TestEvent.START);

        // Schedule the periodic event
        stateMachine.schedulePeriodicEvent(TestEvent.TIMEOUT, 1, 1, TimeUnit.SECONDS);

        // Execute the scheduled task immediately
        testTimerService.triggerPeriod(0);

        // Verify the state changed to STOPPED
        assertEquals(TestState.STOPPED, stateMachine.getCurrentState());
    }

    @Test
    public void testScheduleEventWithData() {
        stateMachine.scheduleEvent(TestEvent.START, "test data", 1, TimeUnit.SECONDS);

        // Execute the scheduled task immediately
        testTimerService.trigger(0);

        // Verify the state changed to RUNNING
        assertEquals(TestState.RUNNING, stateMachine.getCurrentState());
    }

}