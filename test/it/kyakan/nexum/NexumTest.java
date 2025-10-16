package it.kyakan.nexum;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Nexum
 */
@DisplayName("Nexum Tests")
class NexumTest {

    private enum TestState {
        IDLE, RUNNING, PAUSED, STOPPED
    }

    private enum TestEvent {
        START, PAUSE, RESUME, STOP
    }

    private Nexum<TestState, TestEvent> nexum;

    @BeforeEach
    void setUp() {
        nexum = new Nexum<>(TestState.IDLE);
    }

    @Test
    @DisplayName("Should initialize with correct initial state")
    void testInitialState() {
        assertEquals(TestState.IDLE, nexum.getCurrentState());
        assertFalse(nexum.isStarted());
    }

    @Test
    @DisplayName("Should start state machine")
    void testStart() {
        nexum.start();

        assertTrue(nexum.isStarted());
        assertEquals(TestState.IDLE, nexum.getCurrentState());
    }

    @Test
    @DisplayName("Should not start twice")
    void testStartTwice() {
        nexum.start();
        nexum.start(); // Should be idempotent

        assertTrue(nexum.isStarted());
    }

    @Test
    @DisplayName("Should add and execute simple transition")
    void testSimpleTransition() throws NexumException {
        nexum
                .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START)
                .start();

        nexum.fireEvent(TestEvent.START);

        assertEquals(TestState.RUNNING, nexum.getCurrentState());
    }

    @Test
    @DisplayName("Should throw exception when not started")
    void testFireEventBeforeStart() {
        nexum.addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START);

        assertThrows(NexumException.class, () -> {
            nexum.fireEvent(TestEvent.START);
        });
    }

    @Test
    @DisplayName("Should throw exception for invalid transition")
    void testInvalidTransition() {
        nexum
                .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START)
                .start();

        assertThrows(NexumException.class, () -> {
            nexum.fireEvent(TestEvent.STOP); // No transition defined
        });
    }

    @Test
    @DisplayName("Should execute transition with guard")
    void testTransitionWithGuard() throws NexumException {
        nexum
                .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START,
                        (ctx, event, data) -> {
                            Integer value = (Integer) data;
                            return value != null && value > 0;
                        })
                .start();

        // Should succeed with valid data
        nexum.fireEvent(TestEvent.START, 10);
        assertEquals(TestState.RUNNING, nexum.getCurrentState());
    }

    @Test
    @DisplayName("Should reject transition when guard fails")
    void testTransitionGuardFails() {
        nexum
                .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START,
                        (ctx, event, data) -> {
                            Integer value = (Integer) data;
                            return value != null && value > 0;
                        })
                .start();

        assertThrows(NexumException.class, () -> {
            nexum.fireEvent(TestEvent.START, -5);
        });
    }

    @Test
    @DisplayName("Should execute transition action")
    void testTransitionAction() throws NexumException {
        final boolean[] actionExecuted = { false };

        nexum
                .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START,
                        null,
                        (ctx, event, data) -> {
                            actionExecuted[0] = true;
                            ctx.put("started", true);
                        })
                .start();

        nexum.fireEvent(TestEvent.START);

        assertTrue(actionExecuted[0]);
        assertTrue(nexum.getContext().contains("started"));
    }

    @Test
    @DisplayName("Should call state handlers on enter and exit")
    void testStateHandlers() throws NexumException {
        final List<String> events = new ArrayList<>();

        StateHandler<TestState, TestEvent> idleHandler = new StateHandler<TestState, TestEvent>() {
            @Override
            public TestState getState() {
                return TestState.IDLE;
            }

            @Override
            public void onExit(NexumContext<TestState> context, TestState toState, TestEvent event) {
                events.add("IDLE_EXIT");
            }
        };

        StateHandler<TestState, TestEvent> runningHandler = new StateHandler<TestState, TestEvent>() {
            @Override
            public TestState getState() {
                return TestState.RUNNING;
            }

            @Override
            public void onEnter(NexumContext<TestState> context, TestState fromState, TestEvent event) {
                events.add("RUNNING_ENTER");
            }
        };

        nexum
                .registerStateHandler(idleHandler)
                .registerStateHandler(runningHandler)
                .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START)
                .start();

        nexum.fireEvent(TestEvent.START);

        assertTrue(events.contains("IDLE_EXIT"));
        assertTrue(events.contains("RUNNING_ENTER"));
    }

    @Test
    @DisplayName("Should call onEnter for initial state on start")
    void testInitialStateHandler() {
        final boolean[] enterCalled = { false };

        StateHandler<TestState, TestEvent> handler = new StateHandler<TestState, TestEvent>() {
            @Override
            public TestState getState() {
                return TestState.IDLE;
            }

            @Override
            public void onEnter(NexumContext<TestState> context, TestState fromState, TestEvent event) {
                enterCalled[0] = true;
            }
        };

        nexum.registerStateHandler(handler);
        nexum.start();

        assertTrue(enterCalled[0]);
    }

    @Test
    @DisplayName("Should allow state handler to handle event")
    void testStateHandlerHandlesEvent() throws NexumException {
        final boolean[] eventHandled = { false };

        StateHandler<TestState, TestEvent> handler = new StateHandler<TestState, TestEvent>() {
            @Override
            public TestState getState() {
                return TestState.IDLE;
            }

            @Override
            public boolean handleEvent(NexumContext<TestState> context, TestEvent event, Object eventData) {
                if (event == TestEvent.PAUSE) {
                    eventHandled[0] = true;
                    return true; // Event handled, no transition
                }
                return false;
            }
        };

        nexum
                .registerStateHandler(handler)
                .start();

        nexum.fireEvent(TestEvent.PAUSE);

        assertTrue(eventHandled[0]);
        assertEquals(TestState.IDLE, nexum.getCurrentState()); // No transition
    }

    @Test
    @DisplayName("Should notify listeners on state change")
    void testListeners() throws NexumException {
        final List<String> notifications = new ArrayList<>();

        Nexum.NexumListener<TestState, TestEvent> listener = (fromState, toState, event) -> {
            notifications.add(fromState + "->" + toState + " via " + event);
        };

        nexum
                .addListener(listener)
                .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START)
                .start();

        nexum.fireEvent(TestEvent.START);

        assertTrue(notifications.stream().anyMatch(n -> n.contains("IDLE->RUNNING")));
    }

    @Test
    @DisplayName("Should notify listener on start")
    void testListenerOnStart() {
        final boolean[] notified = { false };

        Nexum.NexumListener<TestState, TestEvent> listener = (fromState, toState, event) -> {
            if (fromState == null && toState == TestState.IDLE) {
                notified[0] = true;
            }
        };

        nexum.addListener(listener);
        nexum.start();

        assertTrue(notified[0]);
    }

    @Test
    @DisplayName("Should remove listener")
    void testRemoveListener() throws NexumException {
        final int[] callCount = { 0 };

        Nexum.NexumListener<TestState, TestEvent> listener = (fromState, toState, event) -> callCount[0]++;

        nexum
                .addListener(listener)
                .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START)
                .start();

        nexum.fireEvent(TestEvent.START);
        int countAfterFirst = callCount[0];

        nexum.removeListener(listener);
        nexum.addTransition(TestState.RUNNING, TestState.STOPPED, TestEvent.STOP);
        nexum.fireEvent(TestEvent.STOP);

        assertEquals(countAfterFirst, callCount[0]); // Count should not increase
    }

    @Test
    @DisplayName("Should notify error listener")
    void testErrorListener() {
        final Exception[] caughtError = { null };

        Nexum.NexumListener<TestState, TestEvent> listener = new Nexum.NexumListener<TestState, TestEvent>() {
            @Override
            public void onStateChanged(TestState fromState, TestState toState, TestEvent event) {
            }

            @Override
            public void onError(Exception error) {
                caughtError[0] = error;
            }
        };

        nexum
                .addListener(listener)
                .start();

        try {
            nexum.fireEvent(TestEvent.START); // No transition defined
        } catch (NexumException e) {
            // Expected
        }

        assertNotNull(caughtError[0]);
    }

    @Test
    @DisplayName("Should reset to new state")
    void testReset() {
        nexum
                .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START)
                .start();

        try {
            nexum.fireEvent(TestEvent.START);
        } catch (NexumException e) {
            fail("Should not throw exception");
        }

        assertEquals(TestState.RUNNING, nexum.getCurrentState());

        nexum.reset(TestState.IDLE);

        assertEquals(TestState.IDLE, nexum.getCurrentState());
        assertFalse(nexum.getContext().contains("any-data"));
    }

    @Test
    @DisplayName("Should call handlers on reset")
    void testResetCallsHandlers() {
        final List<String> events = new ArrayList<>();

        StateHandler<TestState, TestEvent> runningHandler = new StateHandler<TestState, TestEvent>() {
            @Override
            public TestState getState() {
                return TestState.RUNNING;
            }

            @Override
            public void onExit(NexumContext<TestState> context, TestState toState, TestEvent event) {
                events.add("RUNNING_EXIT");
            }
        };

        StateHandler<TestState, TestEvent> idleHandler = new StateHandler<TestState, TestEvent>() {
            @Override
            public TestState getState() {
                return TestState.IDLE;
            }

            @Override
            public void onEnter(NexumContext<TestState> context, TestState fromState, TestEvent event) {
                events.add("IDLE_ENTER");
            }
        };

        nexum
                .registerStateHandler(runningHandler)
                .registerStateHandler(idleHandler)
                .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START)
                .start();

        try {
            nexum.fireEvent(TestEvent.START);
        } catch (NexumException e) {
            fail("Should not throw exception");
        }

        events.clear();
        nexum.reset(TestState.IDLE);

        assertTrue(events.contains("RUNNING_EXIT"));
        assertTrue(events.contains("IDLE_ENTER"));
    }

    @Test
    @DisplayName("Should support method chaining")
    void testMethodChaining() {
        Nexum<TestState, TestEvent> result = nexum
                .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START)
                .addTransition(TestState.RUNNING, TestState.PAUSED, TestEvent.PAUSE)
                .addListener((from, to, event) -> {
                })
                .registerStateHandler(new StateHandler<TestState, TestEvent>() {
                    @Override
                    public TestState getState() {
                        return TestState.IDLE;
                    }
                });

        assertSame(nexum, result);
    }

    @Test
    @DisplayName("Should handle multiple transitions from same state")
    void testMultipleTransitionsFromSameState() throws NexumException {
        nexum
                .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START)
                .addTransition(TestState.IDLE, TestState.STOPPED, TestEvent.STOP)
                .start();

        nexum.fireEvent(TestEvent.START);
        assertEquals(TestState.RUNNING, nexum.getCurrentState());

        nexum.reset(TestState.IDLE);
        nexum.fireEvent(TestEvent.STOP);
        assertEquals(TestState.STOPPED, nexum.getCurrentState());
    }

    @Test
    @DisplayName("Should handle multiple transitions from to different ones, using same event with different guards")
    void testMultipleTransitionsFromSameStateDifferntGuards() throws NexumException {
        nexum
                .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START, (ctx, event, data) -> {
                    return true;
                })
                .addTransition(TestState.IDLE, TestState.STOPPED, TestEvent.START, (ctx, event, data) -> {
                    return false;
                })
                .start();

        nexum.fireEvent(TestEvent.START);
        assertEquals(TestState.RUNNING, nexum.getCurrentState());
    }

    @Test
    @DisplayName("Should handle multiple transitions from to different ones, using same event with different guards")
    void testMultipleTransitionsFromSameStateDifferntGuards2() throws NexumException {
        nexum
                .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START, (ctx, event, data) -> {
                    return false;
                })
                .addTransition(TestState.IDLE, TestState.STOPPED, TestEvent.START, (ctx, event, data) -> {
                    return true;
                })
                .start();

        nexum.fireEvent(TestEvent.START);
        assertEquals(TestState.STOPPED, nexum.getCurrentState());
    }

    @Test
    @DisplayName("Should handle multiple transitions from to different ones, using same event with different guards")
    void testMultipleTransitionsFromSameStateDifferntGuards3() throws NexumException {
        nexum
                .addTransition(TestState.IDLE, TestState.STOPPED, TestEvent.START, (ctx, event, data) -> {
                    return true;
                })
                .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START, (ctx, event, data) -> {
                    return true;
                })
                .start();

        nexum.fireEvent(TestEvent.START);
        assertEquals(TestState.STOPPED, nexum.getCurrentState());
    }

    @Test
    @DisplayName("Should store last error in context")
    void testLastErrorInContext() {
        nexum.start();

        try {
            nexum.fireEvent(TestEvent.START);
        } catch (NexumException e) {
            // Expected
        }

        assertNotNull(nexum.getContext().getLastError());
    }

    @Test
    @DisplayName("Should be thread-safe for concurrent transitions")
    void testThreadSafety() throws InterruptedException {
        final int threadCount = 10;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final List<Exception> exceptions = new ArrayList<>();

        nexum
                .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START)
                .start();

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    nexum.fireEvent(TestEvent.START);
                } catch (Exception e) {
                    synchronized (exceptions) {
                        exceptions.add(e);
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        // Only one thread should succeed, others should get exceptions
        assertEquals(threadCount - 1, exceptions.size());
        assertEquals(TestState.RUNNING, nexum.getCurrentState());
    }

    @Test
    @DisplayName("Should pass event data through transition")
    void testEventDataPropagation() throws NexumException {
        final Object[] receivedData = { null };

        nexum
                .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START,
                        null,
                        (ctx, event, data) -> receivedData[0] = data)
                .start();

        String testData = "test-data";
        nexum.fireEvent(TestEvent.START, testData);

        assertEquals(testData, receivedData[0]);
    }

    @Test
    @DisplayName("Transition not expected raise exception")
    void testUnespectedTransition() {
        nexum
                .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START)
                .start();

        assertThrows(NexumException.class, () -> {
            nexum.fireEvent(TestEvent.PAUSE); // No transition defined
        });
    }

    @Test
    @DisplayName("Should invoke default handler for undefined transitions")
    void testDefaultHandlerForUndefinedTransition() {
        final boolean[] handlerInvoked = { false };

        StateHandler<TestState, TestEvent> defaultHandler = new StateHandler<TestState, TestEvent>() {
            @Override
            public TestState getState() {
                return null; // Not used for default handler
            }

            @Override
            public boolean handleEvent(NexumContext<TestState> context, TestEvent event, Object eventData) {
                handlerInvoked[0] = true;
                return true; // Event handled
            }
        };

        nexum
                .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START)
                .setDefaultHandler(defaultHandler)
                .start();

        // This should invoke the default handler instead of throwing an exception
        try {
            nexum.fireEvent(TestEvent.PAUSE);
        } catch (NexumException e) {
            fail("Default handler should prevent NexumException");
        }

        assertTrue(handlerInvoked[0]);
        assertEquals(TestState.IDLE, nexum.getCurrentState()); // State should remain unchanged
    }
}