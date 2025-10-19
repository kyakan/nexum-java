package it.kyakan.nexum;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for fluent API for scheduled transitions
 */
public class FluentScheduledApiTest {

    enum State {
        IDLE, ACTIVE, TIMEOUT, COMPLETED
    }

    enum Event {
        START, STOP, TIMEOUT_EVENT, COMPLETE
    }

    private Nexum<State, Event> sm;
    private TimerServiceImpl timerService;

    @BeforeEach
    public void setUp() {
        timerService = new TimerServiceImpl();
        sm = new Nexum<>(State.IDLE, timerService);
    }

    @Test
    public void testFluentScheduledTransition() {
        AtomicBoolean actionExecuted = new AtomicBoolean(false);
        
        // Use fluent API for scheduled transition
        sm.fromScheduled(State.ACTIVE).to(State.TIMEOUT).on(Event.TIMEOUT_EVENT, 100, TimeUnit.MILLISECONDS)
            .withAction((context, event, data) -> actionExecuted.set(true))
            .from(State.IDLE).to(State.ACTIVE).on(Event.START);
        sm.start();
        
        // Verify scheduled transition was registered
        assertEquals(0, timerService.SCHEDULED_COUNT, "No scheduled transitions yet");
        
        sm.fireEvent(Event.START);
        assertEquals(State.ACTIVE, sm.getCurrentState());
        
        // Now scheduled transition should be active
        // Count is 2 because withAction removes and re-adds the transition
        assertEquals(1, timerService.SCHEDULED_COUNT, "Scheduled transition should be registered");
        assertEquals(0, timerService.EXECUTED_COUNT, "Not executed yet");
        assertFalse(actionExecuted.get());
        
        // Manually trigger the scheduled transition
        timerService.triggerPeriod(0);
        
        assertEquals(State.TIMEOUT, sm.getCurrentState(), "Should have transitioned to TIMEOUT");
        assertEquals(1, timerService.SCHEDULED_COUNT, "Scheduled transition should be registered");
        assertEquals(1, timerService.EXECUTED_COUNT, "Should be executed");
        assertTrue(actionExecuted.get(), "Action should have been executed");
    }

    @Test
    public void testFluentScheduledWithGuard() {
        // Fluent scheduled transition with guard that allows transition
        sm.fromScheduled(State.ACTIVE).to(State.TIMEOUT).on(Event.TIMEOUT_EVENT, 100, TimeUnit.MILLISECONDS)
            .withGuard((context, event, data) -> context.get("canTimeout") != null);
        sm.from(State.IDLE).to(State.ACTIVE).on(Event.START)
            .withAction((context, event, data) -> context.put("canTimeout", true));
        
        sm.start();
        
        sm.fireEvent(Event.START);
        assertEquals(State.ACTIVE, sm.getCurrentState());
        assertTrue((Boolean) sm.getContext().get("canTimeout"));
        
        // Count is 2 because withGuard removes and re-adds the transition
        assertEquals(1, timerService.SCHEDULED_COUNT);
        
        // Trigger - should succeed because guard passes
        timerService.triggerPeriod(0);
        assertEquals(1, timerService.SCHEDULED_COUNT);
        assertEquals(State.TIMEOUT, sm.getCurrentState(), "Guard should have allowed transition");
    }

    @Test
    public void testFluentScheduledWithGuardBlocked() {
        sm.fromScheduled(State.ACTIVE).to(State.TIMEOUT).on(Event.TIMEOUT_EVENT, 100, TimeUnit.MILLISECONDS)
            .withGuard((context, event, data) -> context.get("allowTimeout") != null);
        sm.from(State.IDLE).to(State.ACTIVE).on(Event.START);
        sm.from(State.IDLE).to(State.TIMEOUT).on(Event.STOP);
        sm.from(State.ACTIVE).to(State.ACTIVE).on(Event.COMPLETE).withAction((context, event, data) -> context.put("allowTimeout", true));
        
        sm.start();

        assertEquals(State.IDLE, sm.getCurrentState());
        assertEquals(0, timerService.SCHEDULED_COUNT);
        assertEquals(0, timerService.EXECUTED_COUNT);
        
        sm.fireEvent(Event.START);

        assertEquals(State.ACTIVE, sm.getCurrentState());
        assertEquals(1, timerService.SCHEDULED_COUNT);
        assertEquals(0, timerService.EXECUTED_COUNT);
        
        assertThrows(RuntimeException.class, () -> timerService.triggerPeriod(0));

        assertEquals(State.ACTIVE, sm.getCurrentState());
        assertEquals(1, timerService.SCHEDULED_COUNT);
        assertEquals(0, timerService.EXECUTED_COUNT);

        assertThrows(RuntimeException.class, () -> sm.fireEvent(Event.STOP));

        assertEquals(State.ACTIVE, sm.getCurrentState());
        assertEquals(1, timerService.SCHEDULED_COUNT);
        assertEquals(0, timerService.EXECUTED_COUNT);

        sm.fireEvent(Event.COMPLETE);
        
        assertEquals(State.ACTIVE, sm.getCurrentState());
        assertEquals(1, timerService.SCHEDULED_COUNT);
        assertEquals(0, timerService.EXECUTED_COUNT);

        timerService.triggerPeriod(0);
        
        assertEquals(State.TIMEOUT, sm.getCurrentState());
        assertEquals(1, timerService.SCHEDULED_COUNT);
        assertEquals(1, timerService.EXECUTED_COUNT);
    }

    @Test
    public void testFluentScheduledWithAction() {
        AtomicBoolean actionExecuted = new AtomicBoolean(false);
        
        // Fluent scheduled transition with action
        sm.fromScheduled(State.ACTIVE).to(State.TIMEOUT).on(Event.TIMEOUT_EVENT, 100, TimeUnit.MILLISECONDS)
            .withAction((context, event, data) -> {
                context.put("timedOut", true);
                actionExecuted.set(true);
            });
        sm.from(State.IDLE).to(State.ACTIVE).on(Event.START);
        
        sm.start();
        
        sm.fireEvent(Event.START);
        assertEquals(State.ACTIVE, sm.getCurrentState());
        
        assertFalse(actionExecuted.get());
        
        // Trigger scheduled transition
        timerService.triggerPeriod(0);
        
        assertEquals(State.TIMEOUT, sm.getCurrentState());
        assertTrue(actionExecuted.get(), "Action should have been executed");
        assertTrue((Boolean) sm.getContext().get("timedOut"));
    }

    @Test
    public void testFluentScheduledWithGuardAndAction() {
        AtomicBoolean actionExecuted = new AtomicBoolean(false);
        
        // Fluent scheduled transition with both guard and action
        sm.fromScheduled(State.ACTIVE).to(State.TIMEOUT).on(Event.TIMEOUT_EVENT, 100, TimeUnit.MILLISECONDS)
            .withGuard((context, event, data) -> context.get("active") != null)
            .withAction((context, event, data) -> {
                context.put("timedOut", true);
                actionExecuted.set(true);
            });
        sm.from(State.IDLE).to(State.ACTIVE).on(Event.START)
            .withAction((context, event, data) -> context.put("active", true));
        
        sm.start();
        
        sm.fireEvent(Event.START);
        assertEquals(State.ACTIVE, sm.getCurrentState());
        assertTrue((Boolean) sm.getContext().get("active"));
        
        assertFalse(actionExecuted.get());
        
        // Trigger scheduled transition
        timerService.triggerPeriod(0);
        
        assertEquals(State.TIMEOUT, sm.getCurrentState());
        assertTrue(actionExecuted.get(), "Action should have been executed");
        assertTrue((Boolean) sm.getContext().get("timedOut"));
    }

    @Test
    public void testFluentScheduledChaining() {
        // Chain multiple fluent scheduled transitions
        sm.from(State.IDLE).to(State.ACTIVE).on(Event.START)
            .fromScheduled(State.ACTIVE).to(State.TIMEOUT).on(Event.TIMEOUT_EVENT, 100, TimeUnit.MILLISECONDS)
            .from(State.TIMEOUT).to(State.IDLE).on(Event.STOP);
        
        sm.start();
        
        sm.fireEvent(Event.START);
        assertEquals(State.ACTIVE, sm.getCurrentState());
        
        // Count is 2 because withAction removes and re-adds the transition
        assertEquals(1, timerService.SCHEDULED_COUNT);
        
        // Trigger scheduled transition
        timerService.triggerPeriod(0);
        assertEquals(State.TIMEOUT, sm.getCurrentState());
        
        // Regular transition back
        sm.fireEvent(Event.STOP);
        assertEquals(State.IDLE, sm.getCurrentState());
    }

    @Test
    public void testMixedFluentAndScheduledTransitions() {
        AtomicBoolean timeoutAction = new AtomicBoolean(false);
        AtomicBoolean completeAction = new AtomicBoolean(false);
        
        // Mix regular fluent and scheduled fluent transitions
        sm.from(State.IDLE).to(State.ACTIVE).on(Event.START)
            .withAction((context, event, data) -> context.put("started", true))
            .fromScheduled(State.ACTIVE).to(State.TIMEOUT).on(Event.TIMEOUT_EVENT, 100, TimeUnit.MILLISECONDS)
            .withAction((context, event, data) -> timeoutAction.set(true))
            .from(State.ACTIVE).to(State.COMPLETED).on(Event.COMPLETE)
            .withAction((context, event, data) -> completeAction.set(true));
        
        sm.start();
        
        sm.fireEvent(Event.START);
        assertEquals(State.ACTIVE, sm.getCurrentState());
        assertTrue((Boolean) sm.getContext().get("started"));
        
        // Test regular transition
        sm.fireEvent(Event.COMPLETE);
        assertEquals(State.COMPLETED, sm.getCurrentState());
        assertTrue(completeAction.get());
        assertFalse(timeoutAction.get(), "Timeout should not have triggered");
    }



    @Test
    public void testFluentScheduledBuilds() {
        AtomicBoolean actionExecuted = new AtomicBoolean(false);
        
        // Scheduled transition from multiple states
        sm.fromScheduled(State.ACTIVE, State.COMPLETED).to(State.IDLE).on(Event.TIMEOUT_EVENT, 100, TimeUnit.MILLISECONDS)
            .withGuard((context, event, data) -> true)
            .withAction((context, event, data) -> actionExecuted.set(true));
        sm.start();
        
        assertEquals(2, sm.getTransactionCount());
        assertEquals(2, sm.getScheduledTransactionCount());
    }

    @Test
    public void testFluentScheduledActionsBuilds() {
        AtomicBoolean actionExecuted = new AtomicBoolean(false);
        
        // Scheduled transition from multiple states
        sm.fromScheduled(State.ACTIVE, State.COMPLETED).to(State.IDLE).on(Event.TIMEOUT_EVENT, 100, TimeUnit.MILLISECONDS)
            .withAction((context, event, data) -> actionExecuted.set(true));
        sm.start();
        
        assertEquals(2, sm.getTransactionCount());
        assertEquals(2, sm.getScheduledTransactionCount());
    }


    @Test
    public void testFluentScheduledGuardBuilds() {
        AtomicBoolean actionExecuted = new AtomicBoolean(false);
        
        // Scheduled transition from multiple states
        sm.fromScheduled(State.ACTIVE, State.COMPLETED).to(State.IDLE).on(Event.TIMEOUT_EVENT, 100, TimeUnit.MILLISECONDS)
            .withGuard((context, event, data) -> true);
        sm.start();
        
        assertEquals(2, sm.getTransactionCount());
        assertEquals(2, sm.getScheduledTransactionCount());
    }

    @Test
    public void testFluentScheduledWithMultipleStates() {
        AtomicBoolean actionExecuted = new AtomicBoolean(false);
        
        // Scheduled transition from multiple states
        sm.fromScheduled(State.ACTIVE, State.COMPLETED).to(State.IDLE).on(Event.TIMEOUT_EVENT, 100, TimeUnit.MILLISECONDS)
            .withAction((context, event, data) -> actionExecuted.set(true));
        sm.from(State.IDLE).to(State.ACTIVE).on(Event.START);
        sm.from(State.ACTIVE).to(State.COMPLETED).on(Event.COMPLETE);
        
        sm.start();
        
        assertEquals(0, timerService.SCHEDULED_COUNT);
        assertEquals(0, timerService.EXECUTED_COUNT);
        assertEquals(State.IDLE, sm.getCurrentState());
        assertFalse(actionExecuted.get());

        sm.fireEvent(Event.START);
        
        assertEquals(1, timerService.SCHEDULED_COUNT);
        assertEquals(0, timerService.EXECUTED_COUNT);
        assertEquals(State.ACTIVE, sm.getCurrentState());
        assertFalse(actionExecuted.get());
        
        timerService.triggerPeriod(0);
        
        assertEquals(1, timerService.SCHEDULED_COUNT);
        assertEquals(1, timerService.EXECUTED_COUNT);
        assertEquals(State.IDLE, sm.getCurrentState());
        assertTrue(actionExecuted.get());
        
        actionExecuted.set(false);
        sm.fireEvent(Event.START);
        sm.fireEvent(Event.COMPLETE);


        assertEquals(2, timerService.SCHEDULED_COUNT);
        assertEquals(1, timerService.EXECUTED_COUNT);
        assertEquals(State.COMPLETED, sm.getCurrentState());
        assertFalse(actionExecuted.get());

        timerService.triggerPeriod(1);
        
        assertEquals(2, timerService.SCHEDULED_COUNT);
        assertEquals(2, timerService.EXECUTED_COUNT);
        assertEquals(State.IDLE, sm.getCurrentState());
        assertTrue(actionExecuted.get());
    }

    @Test
    public void testScheduledTransitionCancellation() {
        AtomicBoolean scheduledTriggered = new AtomicBoolean(false);
        
        // Add scheduled transition that should be cancelled
        sm.fromScheduled(State.ACTIVE).to(State.TIMEOUT).on(Event.TIMEOUT_EVENT, 100, TimeUnit.MILLISECONDS)
            .withAction((context, event, data) -> scheduledTriggered.set(true));
        sm.from(State.IDLE).to(State.ACTIVE).on(Event.START);
        sm.from(State.ACTIVE).to(State.COMPLETED).on(Event.COMPLETE);
        sm.start();

        assertEquals(0, timerService.SCHEDULED_COUNT);
        assertEquals(0, timerService.EXECUTED_COUNT);
        assertEquals(State.IDLE, sm.getCurrentState());
        assertFalse(scheduledTriggered.get());
        
        sm.fireEvent(Event.START);
        
        assertEquals(1, timerService.SCHEDULED_COUNT);
        assertEquals(0, timerService.EXECUTED_COUNT);
        assertEquals(State.ACTIVE, sm.getCurrentState());
        assertFalse(scheduledTriggered.get());
                  
        timerService.triggerPeriod(0);
        
        assertEquals(1, timerService.SCHEDULED_COUNT);
        assertEquals(1, timerService.EXECUTED_COUNT);
        assertEquals(State.TIMEOUT, sm.getCurrentState());
        assertTrue(scheduledTriggered.get());
    }
}