package it.kyakan.nexum;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for fluent API compatibility with scheduled transitions
 */
public class FluentApiScheduledTest {

    enum State {
        IDLE, ACTIVE, TIMEOUT
    }

    enum Event {
        START, STOP, TIMEOUT_EVENT
    }

    private Nexum<State, Event> sm;
    private TimerServiceImpl timerService;

    @BeforeEach
    public void setUp() {
        timerService = new TimerServiceImpl();
        sm = new Nexum<>(State.IDLE, timerService);
    }
    
    @Test
    public void testFluentApiWithScheduledTransition() {
        // Use fluent API followed by scheduled transition
        sm.from(State.IDLE).to(State.ACTIVE).on(Event.START)
            .withAction((context, event, data) -> context.put("started", true))
            .addScheduledTransition(State.ACTIVE, State.TIMEOUT, Event.TIMEOUT_EVENT, 100, TimeUnit.MILLISECONDS);
        
        sm.start();
        
        // Verify fluent transition works
        sm.fireEvent(Event.START);
        assertEquals(State.ACTIVE, sm.getCurrentState());
        assertTrue((Boolean) sm.getContext().get("started"));
    }

    @Test
    public void testScheduledTransitionAfterFluentChain() {
        // Chain multiple fluent transitions then add scheduled
        sm.from(State.IDLE).to(State.ACTIVE).on(Event.START)
            .withGuard((context, event, data) -> true)
            .withAction((context, event, data) -> context.put("active", true))
            .from(State.ACTIVE).to(State.IDLE).on(Event.STOP)
            .addScheduledTransition(State.ACTIVE, State.TIMEOUT, Event.TIMEOUT_EVENT, 100, TimeUnit.MILLISECONDS);
        
        sm.start();
        
        sm.fireEvent(Event.START);
        assertEquals(State.ACTIVE, sm.getCurrentState());
        assertTrue((Boolean) sm.getContext().get("active"));
        
        sm.fireEvent(Event.STOP);
        assertEquals(State.IDLE, sm.getCurrentState());
    }

    @Test
    public void testMixedFluentAndRegularTransitions() {
        // Mix fluent API with regular addTransition and scheduled transitions
        sm.from(State.IDLE).to(State.ACTIVE).on(Event.START)
            .withAction((context, event, data) -> context.put("method", "fluent"))
            .addTransition(State.ACTIVE, State.IDLE, Event.STOP)
            .addScheduledTransition(State.ACTIVE, State.TIMEOUT, Event.TIMEOUT_EVENT, 100, TimeUnit.MILLISECONDS);
        
        sm.start();
        
        sm.fireEvent(Event.START);
        assertEquals(State.ACTIVE, sm.getCurrentState());
        assertEquals("fluent", sm.getContext().get("method"));
        
        sm.fireEvent(Event.STOP);
        assertEquals(State.IDLE, sm.getCurrentState());
    }

    @Test
    public void testFluentApiWithStateHandlerAndScheduled() {
        // Use fluent API, register handler, and add scheduled transition
        sm.from(State.IDLE).to(State.ACTIVE).on(Event.START)
            .registerStateHandler(new AbstractStateHandler<State, Event>(State.ACTIVE) {
                @Override
                public void onEnter(NexumContext<State> context, State fromState, Event event) {
                    context.put("handler", "called");
                }
            })
            .addScheduledTransition(State.ACTIVE, State.TIMEOUT, Event.TIMEOUT_EVENT, 100, TimeUnit.MILLISECONDS);
        
        sm.start();
        
        sm.fireEvent(Event.START);
        assertEquals(State.ACTIVE, sm.getCurrentState());
        assertEquals("called", sm.getContext().get("handler"));
    }

    @Test
    public void testFluentApiWithListenerAndScheduled() {
        final boolean[] listenerCalled = {false};
        
        // Use fluent API, add listener, and add scheduled transition
        sm.from(State.IDLE).to(State.ACTIVE).on(Event.START)
            .addListener(new Nexum.NexumListener<State, Event>() {
                @Override
                public void onStateChanged(State fromState, State toState, Event event) {
                    listenerCalled[0] = true;
                }
            })
            .addScheduledTransition(State.ACTIVE, State.TIMEOUT, Event.TIMEOUT_EVENT, 100, TimeUnit.MILLISECONDS);
        
        sm.start();
        
        sm.fireEvent(Event.START);
        assertEquals(State.ACTIVE, sm.getCurrentState());
        assertTrue(listenerCalled[0]);
    }
}