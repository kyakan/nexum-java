package it.kyakan.nexum;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Transition
 */
@DisplayName("Transition Tests")
class TransitionTest {
    private enum TestState {
        IDLE, RUNNING, STOPPED
    }

    private enum TestEvent {
        START, STOP, PAUSE
    }

    private NexumContext<TestState> context;

    @BeforeEach
    void setUp() {
        context = new NexumContext<>(TestState.IDLE);
    }

    @Test
    @DisplayName("Should create simple transition")
    void testSimpleTransition() {
        Transition<TestState, TestEvent> transition = new Transition<>(TestState.IDLE, TestState.RUNNING,
                TestEvent.START);
        assertEquals(TestState.IDLE, transition.getFromState());
        assertEquals(TestState.RUNNING, transition.getToState());
        assertEquals(TestEvent.START, transition.getEvent());
    }

    @Test
    @DisplayName("Should match correct state and event")
    void testMatches() {
        Transition<TestState, TestEvent> transition = new Transition<>(TestState.IDLE, TestState.RUNNING,
                TestEvent.START);
        assertTrue(transition.matches(TestState.IDLE, TestEvent.START));
        assertFalse(transition.matches(TestState.RUNNING, TestEvent.START));
        assertFalse(transition.matches(TestState.IDLE, TestEvent.STOP));
    }

    @Test
    @DisplayName("Should allow transition without guard")
    void testCanTransitionWithoutGuard() {
        Transition<TestState, TestEvent> transition = new Transition<>(TestState.IDLE, TestState.RUNNING,
                TestEvent.START);
        assertTrue(transition.canTransition(context, null));
    }

    @Test
    @DisplayName("Should respect guard condition")
    void testCanTransitionWithGuard() {
        Transition.TransitionGuard<TestState, TestEvent> guard = (ctx, event, data) -> {
            Integer value = (Integer) data;
            return value != null && value > 0;
        };
        Transition<TestState, TestEvent> transition = new Transition<>(TestState.IDLE, TestState.RUNNING,
                TestEvent.START, guard);
        assertTrue(transition.canTransition(context, 10));
        assertFalse(transition.canTransition(context, -5));
        assertFalse(transition.canTransition(context, null));
    }

    @Test
    @DisplayName("Should execute action when present")
    void testExecuteAction() {
        final boolean[] actionExecuted = { false };
        Transition.TransitionAction<TestState, TestEvent> action = (ctx, event, data) -> {
            actionExecuted[0] = true;
            ctx.put("executed", true);
        };
        Transition<TestState, TestEvent> transition = new Transition<>(TestState.IDLE, TestState.RUNNING,
                TestEvent.START, null, action);
        transition.executeAction(context, null);
        assertTrue(actionExecuted[0]);
        assertTrue(context.contains("executed"));
    }

    @Test
    @DisplayName("Should not fail when action is null")
    void testExecuteActionNull() {
        Transition<TestState, TestEvent> transition = new Transition<>(TestState.IDLE, TestState.RUNNING,
                TestEvent.START);
        assertDoesNotThrow(() -> transition.executeAction(context, null));
    }

    @Test
    @DisplayName("Should pass event data to action")
    void testActionReceivesEventData() {
        final Object[] receivedData = { null };
        Transition.TransitionAction<TestState, TestEvent> action = (ctx, event, data) -> {
            receivedData[0] = data;
        };
        Transition<TestState, TestEvent> transition = new Transition<>(TestState.IDLE, TestState.RUNNING,
                TestEvent.START, null, action);
        String testData = "test data";
        transition.executeAction(context, testData);
        assertEquals(testData, receivedData[0]);
    }

    @Test
    @DisplayName("Should pass event to guard")
    void testGuardReceivesEvent() {
        final TestEvent[] receivedEvent = { null };
        Transition.TransitionGuard<TestState, TestEvent> guard = (ctx, event, data) -> {
            receivedEvent[0] = event;
            return true;
        };
        Transition<TestState, TestEvent> transition = new Transition<>(TestState.IDLE, TestState.RUNNING,
                TestEvent.START, guard);
        transition.canTransition(context, null);
        assertEquals(TestEvent.START, receivedEvent[0]);
    }

    @Test
    @DisplayName("Should generate meaningful toString")
    void testToString() {
        Transition<TestState, TestEvent> transition = new Transition<>(TestState.IDLE, TestState.RUNNING,
                TestEvent.START);
        String str = transition.toString();
        assertTrue(str.contains("from=IDLE"));
        assertTrue(str.contains("to=RUNNING"));
        assertTrue(str.contains("event=START"));
        assertTrue(str.contains("hasGuard=false"));
        assertTrue(str.contains("hasAction=false"));
    }

    @Test
    @DisplayName("Should indicate guard presence in toString")
    void testToStringWithGuard() {
        Transition.TransitionGuard<TestState, TestEvent> guard = (ctx, event, data) -> true;
        Transition<TestState, TestEvent> transition = new Transition<>(TestState.IDLE, TestState.RUNNING,
                TestEvent.START, guard);
        String str = transition.toString();
        assertTrue(str.contains("hasGuard=true"));
    }

    @Test
    @DisplayName("Should indicate action presence in toString")
    void testToStringWithAction() {
        Transition.TransitionAction<TestState, TestEvent> action = (ctx, event, data) -> {
        };
        Transition<TestState, TestEvent> transition = new Transition<>(TestState.IDLE, TestState.RUNNING,
                TestEvent.START, null, action);
        String str = transition.toString();
        assertTrue(str.contains("hasAction=true"));
    }

    @Test
    @DisplayName("Should allow guard to access context data")
    void testGuardAccessesContext() {
        context.put("allowed", true);
        Transition.TransitionGuard<TestState, TestEvent> guard = (ctx, event, data) -> {
            Boolean allowed = ctx.get("allowed", Boolean.class);
            return allowed != null && allowed;
        };
        Transition<TestState, TestEvent> transition = new Transition<>(TestState.IDLE, TestState.RUNNING,
                TestEvent.START, guard);
        assertTrue(transition.canTransition(context, null));
        context.put("allowed", false);
        assertFalse(transition.canTransition(context, null));
    }

    @Test
    @DisplayName("Should allow action to modify context")
    void testActionModifiesContext() {
        Transition.TransitionAction<TestState, TestEvent> action = (ctx, event, data) -> {
            ctx.put("counter", 1);
            ctx.put("event", event.toString());
        };
        Transition<TestState, TestEvent> transition = new Transition<>(TestState.IDLE, TestState.RUNNING,
                TestEvent.START, null, action);
        transition.executeAction(context, null);
        assertEquals(1, context.get("counter"));
        assertEquals("START", context.get("event"));
    }
}