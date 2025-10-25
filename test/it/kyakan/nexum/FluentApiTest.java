package it.kyakan.nexum;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Test for the fluent API for creating transitions
 */
public class FluentApiTest {

    enum State {
        UNPLUGGED, PLUGGED, CHARGING, FULL
    }

    enum Event {
        PLUG, UNPLUG, CHARGE_COMPLETE
    }
    private Nexum<State, Event> sm;

    @BeforeEach
    public void setUp() {
        sm = new Nexum<>(State.UNPLUGGED);
    }

    @Test
    public void testFluentApiSingleState() {
        
        // Test fluent API with single state
        sm.from(State.UNPLUGGED).to(State.PLUGGED).on(Event.PLUG);
        sm.start();
        
        sm.fireEvent(Event.PLUG);
        assertEquals(State.PLUGGED, sm.getCurrentState());
    }

    @Test
    public void testFluentApiMultipleStates() {
        
        // Test fluent API with multiple states - this is the main use case
        sm.from(State.UNPLUGGED, State.PLUGGED).to(State.UNPLUGGED).on(Event.UNPLUG);
        sm.from(State.PLUGGED).to(State.CHARGING).on(Event.PLUG);
        sm.start();
        
        // From PLUGGED, UNPLUG should go to UNPLUGGED
        sm.fireEvent(Event.UNPLUG);
        assertEquals(State.UNPLUGGED, sm.getCurrentState());
        
        // From UNPLUGGED, UNPLUG should also go to UNPLUGGED (stays same)
        sm.fireEvent(Event.UNPLUG);
        assertEquals(State.UNPLUGGED, sm.getCurrentState());
    }

    @Test
    public void testFluentApiWithGuard() {
        sm.reset(State.PLUGGED);
        // Add transition with guard
        sm.from(State.PLUGGED).to(State.CHARGING).on(Event.PLUG,
            (context, event, eventData) -> context.get("battery") != null);
        sm.start();
        
        // Should not transition without battery data
        assertThrows(NexumException.class, () -> sm.fireEvent(Event.PLUG));
        assertEquals(State.PLUGGED, sm.getCurrentState());
        
        // Should transition with battery data
        sm.getContext().put("battery", 50);
        sm.fireEvent(Event.PLUG);
        assertEquals(State.CHARGING, sm.getCurrentState());
    }

    @Test
    public void testFluentApiWithGuardAndAction() {
        sm.reset(State.CHARGING);
        
        // Add transition with guard and action
        sm.from(State.CHARGING).to(State.FULL).on(Event.CHARGE_COMPLETE,
            (context, event, eventData) -> true,
            (context, event, eventData) -> context.put("charged", true));
        sm.start();
        
        sm.fireEvent(Event.CHARGE_COMPLETE);
        assertEquals(State.FULL, sm.getCurrentState());
        assertTrue((Boolean) sm.getContext().get("charged"));
    }

    @Test
    public void testFluentApiWithMultipleEvents() {
        sm.reset(State.PLUGGED);
        
        // Test onAny with multiple events
        sm.from(State.PLUGGED, State.CHARGING).to(State.UNPLUGGED)
            .onAny(Event.UNPLUG, Event.CHARGE_COMPLETE);
        sm.start();
        
        // UNPLUG should work
        sm.fireEvent(Event.UNPLUG);
        assertEquals(State.UNPLUGGED, sm.getCurrentState());
    }

    @Test
    public void testFluentApiChaining() {
        
        // Test method chaining
        sm.from(State.UNPLUGGED).to(State.PLUGGED).on(Event.PLUG)
          .from(State.PLUGGED).to(State.CHARGING).on(Event.PLUG)
          .from(State.CHARGING).to(State.FULL).on(Event.CHARGE_COMPLETE)
          .from(State.PLUGGED, State.CHARGING, State.FULL).to(State.UNPLUGGED).on(Event.UNPLUG);
        
        sm.start();
        
        // Test the chain
        sm.fireEvent(Event.PLUG);
        assertEquals(State.PLUGGED, sm.getCurrentState());
        
        sm.fireEvent(Event.PLUG);
        assertEquals(State.CHARGING, sm.getCurrentState());
        
        sm.fireEvent(Event.CHARGE_COMPLETE);
        assertEquals(State.FULL, sm.getCurrentState());
        
        sm.fireEvent(Event.UNPLUG);
        assertEquals(State.UNPLUGGED, sm.getCurrentState());
    }

    @Test
    public void testComparisonWithOldApi() {
        sm.reset(State.PLUGGED);
        sm.from(State.UNPLUGGED, State.PLUGGED).to(State.UNPLUGGED).on(Event.UNPLUG);

        // New fluent API style
        Nexum<State, Event> sm2 = new Nexum<>(State.UNPLUGGED);
        sm2.from(State.UNPLUGGED, State.PLUGGED).to(State.UNPLUGGED).on(Event.UNPLUG);
        
        // Both should work the same way
        sm.start();
        sm2.start();
        
        sm.fireEvent(Event.UNPLUG);
        sm2.fireEvent(Event.UNPLUG);
        
        assertEquals(sm.getCurrentState(), sm2.getCurrentState());
    }

    @Test
    public void testFluentBuilds() {
        AtomicBoolean actionExecuted = new AtomicBoolean(false);
        sm.reset(State.UNPLUGGED);
        
        // Scheduled transition from multiple states
        sm.from(State.UNPLUGGED, State.CHARGING).to(State.PLUGGED).on(Event.PLUG)
            .withGuard((context, event, data) -> true)
            .withAction((context, event, data) -> actionExecuted.set(true));
        sm.start();
        
        assertEquals(2, sm.getTransactionCount());
        assertEquals(0, sm.getScheduledTransactionCount());
    }

    @Test
    public void testFluentActionsBuilds() {
        AtomicBoolean actionExecuted = new AtomicBoolean(false);
        
        // Scheduled transition from multiple states
        sm.from(State.UNPLUGGED, State.CHARGING).to(State.PLUGGED).on(Event.PLUG)
            .withAction((context, event, data) -> actionExecuted.set(true));
        sm.start();
        
        assertEquals(2, sm.getTransactionCount());
        assertEquals(0, sm.getScheduledTransactionCount());
    }


    @Test
    public void testFluentGuardBuilds() {
        // Scheduled transition from multiple states
        sm.from(State.UNPLUGGED, State.CHARGING).to(State.PLUGGED).on(Event.PLUG)
            .withGuard((context, event, data) -> true);
        sm.start();
        
        assertEquals(2, sm.getTransactionCount());
        assertEquals(0, sm.getScheduledTransactionCount());
    }


    @Test
    public void testFluentApiWithGuardMethod() {
        Nexum<State, Event> sm = new Nexum<>(State.PLUGGED);
        
        // New fluent style with withGuard - no build() needed!
        sm.from(State.PLUGGED).to(State.CHARGING).on(Event.PLUG)
            .withGuard((context, event, eventData) -> context.get("battery") != null);
        sm.start();
        
        // Should not transition without battery data
        assertThrows(NexumException.class, () -> sm.fireEvent(Event.PLUG));
        assertEquals(State.PLUGGED, sm.getCurrentState());
        
        // Should transition with battery data
        sm.getContext().put("battery", 50);
        sm.fireEvent(Event.PLUG);
        assertEquals(State.CHARGING, sm.getCurrentState());
    }

    @Test
    public void testFluentApiWithActionMethod() {
        Nexum<State, Event> sm = new Nexum<>(State.CHARGING);
        
        // New fluent style with withAction - no build() needed!
        sm.from(State.CHARGING).to(State.FULL).on(Event.CHARGE_COMPLETE)
            .withAction((context, event, eventData) -> context.put("charged", true));
        sm.start();
        
        sm.fireEvent(Event.CHARGE_COMPLETE);
        assertEquals(State.FULL, sm.getCurrentState());
        assertTrue((Boolean) sm.getContext().get("charged"));
    }

    @Test
    public void testFluentApiWithGuardAndActionMethods() {
        Nexum<State, Event> sm = new Nexum<>(State.PLUGGED);
        
        // New fluent style with both withGuard and withAction - no build() needed!
        sm.from(State.PLUGGED).to(State.CHARGING).on(Event.PLUG)
            .withGuard((context, event, eventData) -> context.get("battery") != null)
            .withAction((context, event, eventData) -> {
                context.put("chargingStarted", true);
                context.put("startTime", System.currentTimeMillis());
            });
        sm.start();
        
        sm.getContext().put("battery", 50);
        sm.fireEvent(Event.PLUG);
        
        assertEquals(State.CHARGING, sm.getCurrentState());
        assertTrue((Boolean) sm.getContext().get("chargingStarted"));
        assertNotNull(sm.getContext().get("startTime"));
    }

    @Test
    public void testFluentApiChainingWithBuilder() {
        Nexum<State, Event> sm = new Nexum<>(State.UNPLUGGED);
        
        // Test chaining with the new builder pattern - no build() needed!
        sm.from(State.UNPLUGGED).to(State.PLUGGED).on(Event.PLUG)
            .withAction((context, event, eventData) -> context.put("plugged", true))
            .from(State.PLUGGED).to(State.CHARGING).on(Event.PLUG)
            .withGuard((context, event, eventData) -> context.get("plugged") != null)
            .withAction((context, event, eventData) -> context.put("charging", true));
        
        sm.start();
        
        sm.fireEvent(Event.PLUG);
        assertEquals(State.PLUGGED, sm.getCurrentState());
        assertTrue((Boolean) sm.getContext().get("plugged"));
        
        sm.fireEvent(Event.PLUG);
        assertEquals(State.CHARGING, sm.getCurrentState());
        assertTrue((Boolean) sm.getContext().get("charging"));
    }

    @Test
    public void testFluentApiWithoutBuild() {
        Nexum<State, Event> sm = new Nexum<>(State.UNPLUGGED);
        
        // Test that build() is not needed at all!
        sm.from(State.UNPLUGGED).to(State.PLUGGED).on(Event.PLUG)
            .withAction((context, event, eventData) -> context.put("action1", true))
            .from(State.PLUGGED).to(State.UNPLUGGED).on(Event.UNPLUG)
            .withAction((context, event, eventData) -> context.put("action2", true));
        
        sm.start();
        
        sm.fireEvent(Event.PLUG);
        assertTrue((Boolean) sm.getContext().get("action1"));
        
        sm.fireEvent(Event.UNPLUG);
        assertTrue((Boolean) sm.getContext().get("action2"));
    }

    @Test
    public void testOnAnyWithGuardAndActionChaining() {
        sm.reset(State.PLUGGED);

        AtomicBoolean actionExecuted = new AtomicBoolean(false);
        AtomicBoolean guardChecked = new AtomicBoolean(false);

        sm.from(State.PLUGGED, State.CHARGING).to(State.UNPLUGGED)
            .onAny(Event.UNPLUG, Event.CHARGE_COMPLETE)
            .withGuard((context, event, data) -> {
                guardChecked.set(true);
                return true;
            })
            .withAction((context, event, data) -> {
                actionExecuted.set(true);
                context.put("eventFired", event);
            });

        sm.start();

        sm.fireEvent(Event.UNPLUG);
        
        assertEquals(State.UNPLUGGED, sm.getCurrentState());
        assertTrue(guardChecked.get(), "Guard should have been checked");
        assertTrue(actionExecuted.get(), "Action should have been executed");
        assertEquals(Event.UNPLUG, sm.getContext().get("eventFired"));
    }
}