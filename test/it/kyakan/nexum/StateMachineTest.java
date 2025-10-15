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
 * Unit tests for StateMachine
 */
@DisplayName("StateMachine Tests")
class StateMachineTest {
    
    private enum TestState {
        IDLE, RUNNING, PAUSED, STOPPED
    }
    
    private enum TestEvent {
        START, PAUSE, RESUME, STOP
    }
    
    private StateMachine<TestState, TestEvent> stateMachine;
    
    @BeforeEach
    void setUp() {
        stateMachine = new StateMachine<>(TestState.IDLE);
    }
    
    @Test
    @DisplayName("Should initialize with correct initial state")
    void testInitialState() {
        assertEquals(TestState.IDLE, stateMachine.getCurrentState());
        assertFalse(stateMachine.isStarted());
    }
    
    @Test
    @DisplayName("Should start state machine")
    void testStart() {
        stateMachine.start();
        
        assertTrue(stateMachine.isStarted());
        assertEquals(TestState.IDLE, stateMachine.getCurrentState());
    }
    
    @Test
    @DisplayName("Should not start twice")
    void testStartTwice() {
        stateMachine.start();
        stateMachine.start(); // Should be idempotent
        
        assertTrue(stateMachine.isStarted());
    }
    
    @Test
    @DisplayName("Should add and execute simple transition")
    void testSimpleTransition() throws StateMachineException {
        stateMachine
            .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START)
            .start();
        
        stateMachine.fireEvent(TestEvent.START);
        
        assertEquals(TestState.RUNNING, stateMachine.getCurrentState());
    }
    
    @Test
    @DisplayName("Should throw exception when not started")
    void testFireEventBeforeStart() {
        stateMachine.addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START);
        
        assertThrows(StateMachineException.class, () -> {
            stateMachine.fireEvent(TestEvent.START);
        });
    }
    
    @Test
    @DisplayName("Should throw exception for invalid transition")
    void testInvalidTransition() {
        stateMachine
            .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START)
            .start();
        
        assertThrows(StateMachineException.class, () -> {
            stateMachine.fireEvent(TestEvent.STOP); // No transition defined
        });
    }
    
    @Test
    @DisplayName("Should execute transition with guard")
    void testTransitionWithGuard() throws StateMachineException {
        stateMachine
            .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START,
                (ctx, event, data) -> {
                    Integer value = (Integer) data;
                    return value != null && value > 0;
                })
            .start();
        
        // Should succeed with valid data
        stateMachine.fireEvent(TestEvent.START, 10);
        assertEquals(TestState.RUNNING, stateMachine.getCurrentState());
    }
    
    @Test
    @DisplayName("Should reject transition when guard fails")
    void testTransitionGuardFails() {
        stateMachine
            .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START,
                (ctx, event, data) -> {
                    Integer value = (Integer) data;
                    return value != null && value > 0;
                })
            .start();
        
        assertThrows(StateMachineException.class, () -> {
            stateMachine.fireEvent(TestEvent.START, -5);
        });
    }
    
    @Test
    @DisplayName("Should execute transition action")
    void testTransitionAction() throws StateMachineException {
        final boolean[] actionExecuted = {false};
        
        stateMachine
            .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START,
                null,
                (ctx, event, data) -> {
                    actionExecuted[0] = true;
                    ctx.put("started", true);
                })
            .start();
        
        stateMachine.fireEvent(TestEvent.START);
        
        assertTrue(actionExecuted[0]);
        assertTrue(stateMachine.getContext().contains("started"));
    }
    
    @Test
    @DisplayName("Should call state handlers on enter and exit")
    void testStateHandlers() throws StateMachineException {
        final List<String> events = new ArrayList<>();
        
        StateHandler<TestState, TestEvent> idleHandler = new StateHandler<TestState, TestEvent>() {
            @Override
            public TestState getState() {
                return TestState.IDLE;
            }
            
            @Override
            public void onExit(StateMachineContext<TestState> context, TestState toState, TestEvent event) {
                events.add("IDLE_EXIT");
            }
        };
        
        StateHandler<TestState, TestEvent> runningHandler = new StateHandler<TestState, TestEvent>() {
            @Override
            public TestState getState() {
                return TestState.RUNNING;
            }
            
            @Override
            public void onEnter(StateMachineContext<TestState> context, TestState fromState, TestEvent event) {
                events.add("RUNNING_ENTER");
            }
        };
        
        stateMachine
            .registerStateHandler(idleHandler)
            .registerStateHandler(runningHandler)
            .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START)
            .start();
        
        stateMachine.fireEvent(TestEvent.START);
        
        assertTrue(events.contains("IDLE_EXIT"));
        assertTrue(events.contains("RUNNING_ENTER"));
    }
    
    @Test
    @DisplayName("Should call onEnter for initial state on start")
    void testInitialStateHandler() {
        final boolean[] enterCalled = {false};
        
        StateHandler<TestState, TestEvent> handler = new StateHandler<TestState, TestEvent>() {
            @Override
            public TestState getState() {
                return TestState.IDLE;
            }
            
            @Override
            public void onEnter(StateMachineContext<TestState> context, TestState fromState, TestEvent event) {
                enterCalled[0] = true;
            }
        };
        
        stateMachine.registerStateHandler(handler);
        stateMachine.start();
        
        assertTrue(enterCalled[0]);
    }
    
    @Test
    @DisplayName("Should allow state handler to handle event")
    void testStateHandlerHandlesEvent() throws StateMachineException {
        final boolean[] eventHandled = {false};
        
        StateHandler<TestState, TestEvent> handler = new StateHandler<TestState, TestEvent>() {
            @Override
            public TestState getState() {
                return TestState.IDLE;
            }
            
            @Override
            public boolean handleEvent(StateMachineContext<TestState> context, TestEvent event, Object eventData) {
                if (event == TestEvent.PAUSE) {
                    eventHandled[0] = true;
                    return true; // Event handled, no transition
                }
                return false;
            }
        };
        
        stateMachine
            .registerStateHandler(handler)
            .start();
        
        stateMachine.fireEvent(TestEvent.PAUSE);
        
        assertTrue(eventHandled[0]);
        assertEquals(TestState.IDLE, stateMachine.getCurrentState()); // No transition
    }
    
    @Test
    @DisplayName("Should notify listeners on state change")
    void testListeners() throws StateMachineException {
        final List<String> notifications = new ArrayList<>();
        
        StateMachine.StateMachineListener<TestState, TestEvent> listener = 
            (fromState, toState, event) -> {
                notifications.add(fromState + "->" + toState + " via " + event);
            };
        
        stateMachine
            .addListener(listener)
            .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START)
            .start();
        
        stateMachine.fireEvent(TestEvent.START);
        
        assertTrue(notifications.stream().anyMatch(n -> n.contains("IDLE->RUNNING")));
    }
    
    @Test
    @DisplayName("Should notify listener on start")
    void testListenerOnStart() {
        final boolean[] notified = {false};
        
        StateMachine.StateMachineListener<TestState, TestEvent> listener = 
            (fromState, toState, event) -> {
                if (fromState == null && toState == TestState.IDLE) {
                    notified[0] = true;
                }
            };
        
        stateMachine.addListener(listener);
        stateMachine.start();
        
        assertTrue(notified[0]);
    }
    
    @Test
    @DisplayName("Should remove listener")
    void testRemoveListener() throws StateMachineException {
        final int[] callCount = {0};
        
        StateMachine.StateMachineListener<TestState, TestEvent> listener = 
            (fromState, toState, event) -> callCount[0]++;
        
        stateMachine
            .addListener(listener)
            .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START)
            .start();
        
        stateMachine.fireEvent(TestEvent.START);
        int countAfterFirst = callCount[0];
        
        stateMachine.removeListener(listener);
        stateMachine.addTransition(TestState.RUNNING, TestState.STOPPED, TestEvent.STOP);
        stateMachine.fireEvent(TestEvent.STOP);
        
        assertEquals(countAfterFirst, callCount[0]); // Count should not increase
    }
    
    @Test
    @DisplayName("Should notify error listener")
    void testErrorListener() {
        final Exception[] caughtError = {null};
        
        StateMachine.StateMachineListener<TestState, TestEvent> listener = 
            new StateMachine.StateMachineListener<TestState, TestEvent>() {
                @Override
                public void onStateChanged(TestState fromState, TestState toState, TestEvent event) {
                }
                
                @Override
                public void onError(Exception error) {
                    caughtError[0] = error;
                }
            };
        
        stateMachine
            .addListener(listener)
            .start();
        
        try {
            stateMachine.fireEvent(TestEvent.START); // No transition defined
        } catch (StateMachineException e) {
            // Expected
        }
        
        assertNotNull(caughtError[0]);
    }
    
    @Test
    @DisplayName("Should reset to new state")
    void testReset() {
        stateMachine
            .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START)
            .start();
        
        try {
            stateMachine.fireEvent(TestEvent.START);
        } catch (StateMachineException e) {
            fail("Should not throw exception");
        }
        
        assertEquals(TestState.RUNNING, stateMachine.getCurrentState());
        
        stateMachine.reset(TestState.IDLE);
        
        assertEquals(TestState.IDLE, stateMachine.getCurrentState());
        assertFalse(stateMachine.getContext().contains("any-data"));
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
            public void onExit(StateMachineContext<TestState> context, TestState toState, TestEvent event) {
                events.add("RUNNING_EXIT");
            }
        };
        
        StateHandler<TestState, TestEvent> idleHandler = new StateHandler<TestState, TestEvent>() {
            @Override
            public TestState getState() {
                return TestState.IDLE;
            }
            
            @Override
            public void onEnter(StateMachineContext<TestState> context, TestState fromState, TestEvent event) {
                events.add("IDLE_ENTER");
            }
        };
        
        stateMachine
            .registerStateHandler(runningHandler)
            .registerStateHandler(idleHandler)
            .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START)
            .start();
        
        try {
            stateMachine.fireEvent(TestEvent.START);
        } catch (StateMachineException e) {
            fail("Should not throw exception");
        }
        
        events.clear();
        stateMachine.reset(TestState.IDLE);
        
        assertTrue(events.contains("RUNNING_EXIT"));
        assertTrue(events.contains("IDLE_ENTER"));
    }
    
    @Test
    @DisplayName("Should support method chaining")
    void testMethodChaining() {
        StateMachine<TestState, TestEvent> result = stateMachine
            .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START)
            .addTransition(TestState.RUNNING, TestState.PAUSED, TestEvent.PAUSE)
            .addListener((from, to, event) -> {})
            .registerStateHandler(new StateHandler<TestState, TestEvent>() {
                @Override
                public TestState getState() {
                    return TestState.IDLE;
                }
            });
        
        assertSame(stateMachine, result);
    }
    
    @Test
    @DisplayName("Should handle multiple transitions from same state")
    void testMultipleTransitionsFromSameState() throws StateMachineException {
        stateMachine
            .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START)
            .addTransition(TestState.IDLE, TestState.STOPPED, TestEvent.STOP)
            .start();
        
        stateMachine.fireEvent(TestEvent.START);
        assertEquals(TestState.RUNNING, stateMachine.getCurrentState());
        
        stateMachine.reset(TestState.IDLE);
        stateMachine.fireEvent(TestEvent.STOP);
        assertEquals(TestState.STOPPED, stateMachine.getCurrentState());
    }
    
    @Test
    @DisplayName("Should handle multiple transitions from to different ones, using same event with different guards")
    void testMultipleTransitionsFromSameStateDifferntGuards() throws StateMachineException {
        stateMachine
            .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START, (ctx, event, data) -> {
                return true;
            })
            .addTransition(TestState.IDLE, TestState.STOPPED, TestEvent.START, (ctx, event, data) -> {
                return false;
            })
            .start();
        
        stateMachine.fireEvent(TestEvent.START);
        assertEquals(TestState.RUNNING, stateMachine.getCurrentState());
    }
    
    @Test
    @DisplayName("Should handle multiple transitions from to different ones, using same event with different guards")
    void testMultipleTransitionsFromSameStateDifferntGuards2() throws StateMachineException {
        stateMachine
            .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START, (ctx, event, data) -> {
                return false;
            })
            .addTransition(TestState.IDLE, TestState.STOPPED, TestEvent.START, (ctx, event, data) -> {
                return true;
            })
            .start();
        
        stateMachine.fireEvent(TestEvent.START);
        assertEquals(TestState.STOPPED, stateMachine.getCurrentState());
    }
    
    @Test
    @DisplayName("Should handle multiple transitions from to different ones, using same event with different guards")
    void testMultipleTransitionsFromSameStateDifferntGuards3() throws StateMachineException {
        stateMachine
            .addTransition(TestState.IDLE, TestState.STOPPED, TestEvent.START, (ctx, event, data) -> {
                return true;
            })
            .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START, (ctx, event, data) -> {
                return true;
            })
            .start();
        
        stateMachine.fireEvent(TestEvent.START);
        assertEquals(TestState.STOPPED, stateMachine.getCurrentState());
    }
    
    
    @Test
    @DisplayName("Should store last error in context")
    void testLastErrorInContext() {
        stateMachine.start();
        
        try {
            stateMachine.fireEvent(TestEvent.START);
        } catch (StateMachineException e) {
            // Expected
        }
        
        assertNotNull(stateMachine.getContext().getLastError());
    }
    
    @Test
    @DisplayName("Should be thread-safe for concurrent transitions")
    void testThreadSafety() throws InterruptedException {
        final int threadCount = 10;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final List<Exception> exceptions = new ArrayList<>();
        
        stateMachine
            .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START)
            .start();
        
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    stateMachine.fireEvent(TestEvent.START);
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
        assertEquals(TestState.RUNNING, stateMachine.getCurrentState());
    }
    
    @Test
    @DisplayName("Should pass event data through transition")
    void testEventDataPropagation() throws StateMachineException {
        final Object[] receivedData = {null};
        
        stateMachine
            .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START,
                null,
                (ctx, event, data) -> receivedData[0] = data)
            .start();
        
        String testData = "test-data";
        stateMachine.fireEvent(TestEvent.START, testData);
        
        assertEquals(testData, receivedData[0]);
    }
}