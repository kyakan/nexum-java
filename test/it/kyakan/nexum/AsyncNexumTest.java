package it.kyakan.nexum;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Nexum async functionality
 */
@DisplayName("Async Nexum Tests")
class AsyncNexumTest {

    private enum TestState {
        IDLE, RUNNING, PAUSED, STOPPED
    }

    private enum TestEvent {
        START, PAUSE, RESUME, STOP
    }

    private Nexum<TestState, TestEvent> nexum;
    private Executor executor;
    private AsyncScheduler asyncScheduler;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(4);
        asyncScheduler = new DefaultAsyncScheduler(executor);
        nexum = new Nexum<>(TestState.IDLE, asyncScheduler);
    }

    @Test
    @DisplayName("Should execute state handlers asynchronously with AsyncScheduler")
    void testAsyncStateHandlersWithScheduler() throws Exception {
        final List<String> events = new ArrayList<>();
        final CountDownLatch latch = new CountDownLatch(2);

        StateHandler<TestState, TestEvent> idleHandler = new StateHandler<TestState, TestEvent>() {
            @Override
            public TestState getState() {
                return TestState.IDLE;
            }

            @Override
            public void onExit(NexumContext<TestState> context, TestState toState, TestEvent event) {
                events.add("IDLE_EXIT");
                latch.countDown();
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
                latch.countDown();
            }
        };

        nexum
                .registerStateHandler(idleHandler)
                .registerStateHandler(runningHandler)
                .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START)
                .start();

        nexum.fireEvent(TestEvent.START);

        // Wait for async handlers to complete
        assertTrue(latch.await(5, TimeUnit.SECONDS));

        assertTrue(events.contains("IDLE_EXIT"));
        assertTrue(events.contains("RUNNING_ENTER"));
        assertEquals(TestState.RUNNING, nexum.getCurrentState());
    }

    @Test
    @DisplayName("Should execute AsyncStateHandler methods asynchronously")
    void testAsyncStateHandlerInterface() throws Exception {
        final List<String> events = new ArrayList<>();
        final CountDownLatch latch = new CountDownLatch(2);

        AsyncStateHandler<TestState, TestEvent> idleHandler = new AsyncStateHandler<TestState, TestEvent>() {
            @Override
            public TestState getState() {
                return TestState.IDLE;
            }

            @Override
            public CompletableFuture<Void> onExitAsync(NexumContext<TestState> context, TestState toState, TestEvent event) {
                return CompletableFuture.runAsync(() -> {
                    events.add("IDLE_EXIT_ASYNC");
                    latch.countDown();
                }, executor);
            }
        };

        AsyncStateHandler<TestState, TestEvent> runningHandler = new AsyncStateHandler<TestState, TestEvent>() {
            @Override
            public TestState getState() {
                return TestState.RUNNING;
            }

            @Override
            public CompletableFuture<Void> onEnterAsync(NexumContext<TestState> context, TestState fromState, TestEvent event) {
                return CompletableFuture.runAsync(() -> {
                    events.add("RUNNING_ENTER_ASYNC");
                    latch.countDown();
                }, executor);
            }
        };

        nexum
                .registerStateHandler(idleHandler)
                .registerStateHandler(runningHandler)
                .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START)
                .start();

        nexum.fireEvent(TestEvent.START);

        // Wait for async handlers to complete
        assertTrue(latch.await(5, TimeUnit.SECONDS));

        assertTrue(events.contains("IDLE_EXIT_ASYNC"));
        assertTrue(events.contains("RUNNING_ENTER_ASYNC"));
        assertEquals(TestState.RUNNING, nexum.getCurrentState());
    }

    @Test
    @DisplayName("Should handle async start with AsyncStateHandler")
    void testAsyncStart() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        AsyncStateHandler<TestState, TestEvent> idleHandler = new AsyncStateHandler<TestState, TestEvent>() {
            @Override
            public TestState getState() {
                return TestState.IDLE;
            }

            @Override
            public CompletableFuture<Void> onEnterAsync(NexumContext<TestState> context, TestState fromState, TestEvent event) {
                return CompletableFuture.runAsync(() -> {
                    context.put("asyncStarted", true);
                    latch.countDown();
                }, executor);
            }
        };

        nexum
                .registerStateHandler(idleHandler)
                .start();

        // Wait for async start to complete
        assertTrue(latch.await(5, TimeUnit.SECONDS));

        assertTrue(nexum.getContext().contains("asyncStarted"));
        assertTrue(nexum.isStarted());
    }

    @Test
    @DisplayName("Should handle async reset")
    void testAsyncReset() throws Exception {
        final CountDownLatch latch = new CountDownLatch(2);

        AsyncStateHandler<TestState, TestEvent> runningHandler = new AsyncStateHandler<TestState, TestEvent>() {
            @Override
            public TestState getState() {
                return TestState.RUNNING;
            }

            @Override
            public CompletableFuture<Void> onExitAsync(NexumContext<TestState> context, TestState toState, TestEvent event) {
                return CompletableFuture.runAsync(() -> {
                    context.put("asyncExited", true);
                    latch.countDown();
                }, executor);
            }
        };

        AsyncStateHandler<TestState, TestEvent> idleHandler = new AsyncStateHandler<TestState, TestEvent>() {
            @Override
            public TestState getState() {
                return TestState.IDLE;
            }

            @Override
            public CompletableFuture<Void> onEnterAsync(NexumContext<TestState> context, TestState fromState, TestEvent event) {
                return CompletableFuture.runAsync(() -> {
                    context.put("asyncEntered", true);
                    latch.countDown();
                }, executor);
            }
        };

        nexum
                .registerStateHandler(runningHandler)
                .registerStateHandler(idleHandler)
                .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START)
                .start();

        // First transition to RUNNING
        nexum.fireEvent(TestEvent.START);
        assertEquals(TestState.RUNNING, nexum.getCurrentState());

        // Reset asynchronously
        nexum.reset(TestState.IDLE);

        // Wait for async reset to complete
        assertTrue(latch.await(5, TimeUnit.SECONDS));

        // The context is cleared during reset, so we need to check the state instead
        assertEquals(TestState.IDLE, nexum.getCurrentState());
    }

    @Test
    @DisplayName("Should handle async event handling")
    void testAsyncEventHandling() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        AsyncStateHandler<TestState, TestEvent> idleHandler = new AsyncStateHandler<TestState, TestEvent>() {
            @Override
            public TestState getState() {
                return TestState.IDLE;
            }

            @Override
            public CompletableFuture<Boolean> handleEventAsync(NexumContext<TestState> context, TestEvent event, Object eventData) {
                return CompletableFuture.supplyAsync(() -> {
                    if (event == TestEvent.PAUSE) {
                        context.put("asyncHandled", true);
                        latch.countDown();
                        return true;
                    }
                    return false;
                }, executor);
            }
        };

        nexum
                .registerStateHandler(idleHandler)
                .start();

        // Fire event that should be handled asynchronously
        nexum.fireEvent(TestEvent.PAUSE);

        // Wait for async handling to complete
        assertTrue(latch.await(5, TimeUnit.SECONDS));

        assertTrue(nexum.getContext().contains("asyncHandled"));
        assertEquals(TestState.IDLE, nexum.getCurrentState()); // No transition occurred
    }

    @Test
    @DisplayName("Should support enabling/disabling async execution")
    void testAsyncEnabledToggle() throws Exception {
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
                .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START);

        // Test with async disabled
        nexum.setAsyncEnabled(false);
        nexum.start();
        nexum.fireEvent(TestEvent.START);

        assertTrue(events.contains("IDLE_EXIT"));
        assertTrue(events.contains("RUNNING_ENTER"));
        assertEquals(TestState.RUNNING, nexum.getCurrentState());

        // Reset and test with async enabled
        events.clear();
        nexum.reset(TestState.IDLE);
        nexum.setAsyncEnabled(true);
        nexum.fireEvent(TestEvent.START);

        // Wait a bit for async execution
        Thread.sleep(100);

        assertTrue(events.contains("IDLE_EXIT"));
        assertTrue(events.contains("RUNNING_ENTER"));
        assertEquals(TestState.RUNNING, nexum.getCurrentState());
    }

    @Test
    @DisplayName("Should work with mixed sync and async handlers")
    void testMixedSyncAndAsyncHandlers() throws Exception {
        final List<String> events = new ArrayList<>();
        final CountDownLatch latch = new CountDownLatch(2);

        // Sync handler
        StateHandler<TestState, TestEvent> idleHandler = new StateHandler<TestState, TestEvent>() {
            @Override
            public TestState getState() {
                return TestState.IDLE;
            }

            @Override
            public void onExit(NexumContext<TestState> context, TestState toState, TestEvent event) {
                events.add("IDLE_EXIT_SYNC");
                latch.countDown();
            }
        };

        // Async handler
        AsyncStateHandler<TestState, TestEvent> runningHandler = new AsyncStateHandler<TestState, TestEvent>() {
            @Override
            public TestState getState() {
                return TestState.RUNNING;
            }

            @Override
            public CompletableFuture<Void> onEnterAsync(NexumContext<TestState> context, TestState fromState, TestEvent event) {
                return CompletableFuture.runAsync(() -> {
                    events.add("RUNNING_ENTER_ASYNC");
                    latch.countDown();
                }, executor);
            }
        };

        nexum
                .registerStateHandler(idleHandler)
                .registerStateHandler(runningHandler)
                .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START)
                .start();

        nexum.fireEvent(TestEvent.START);

        // Wait for async handlers to complete
        assertTrue(latch.await(5, TimeUnit.SECONDS));

        assertTrue(events.contains("IDLE_EXIT_SYNC"));
        assertTrue(events.contains("RUNNING_ENTER_ASYNC"));
        assertEquals(TestState.RUNNING, nexum.getCurrentState());
    }

    @Test
    @DisplayName("Should handle async execution without AsyncScheduler set")
    void testAsyncWithoutScheduler() throws Exception {
        // Create Nexum without async scheduler
        Nexum<TestState, TestEvent> syncNexum = new Nexum<>(TestState.IDLE);
        
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

        syncNexum
                .registerStateHandler(idleHandler)
                .registerStateHandler(runningHandler)
                .addTransition(TestState.IDLE, TestState.RUNNING, TestEvent.START)
                .start();

        syncNexum.fireEvent(TestEvent.START);

        assertTrue(events.contains("IDLE_EXIT"));
        assertTrue(events.contains("RUNNING_ENTER"));
        assertEquals(TestState.RUNNING, syncNexum.getCurrentState());
        assertFalse(syncNexum.isAsyncEnabled());
    }
}