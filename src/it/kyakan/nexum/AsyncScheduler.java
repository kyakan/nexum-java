package it.kyakan.nexum;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Interface for scheduling asynchronous execution of state handlers.
 * Allows handlers to be executed asynchronously using various execution strategies.
 */
public interface AsyncScheduler {

    /**
     * Execute a handler method asynchronously
     *
     * @param <S> The state type
     * @param <E> The event type
     * @param handler The handler to execute
     * @param execution The execution to perform
     * @param context The state machine context
     * @param fromState The previous state (can be null)
     * @param toState The next state (can be null)
     * @param event The event that triggered the transition (can be null)
     * @param eventData Optional data associated with the event (can be null)
     * @return A CompletableFuture that completes when the handler execution finishes
     */
    <S, E> CompletableFuture<Void> executeAsync(
        StateHandler<S, E> handler,
        HandlerExecution execution,
        NexumContext<S> context,
        S fromState,
        S toState,
        E event,
        Object eventData
    );

    /**
     * Shutdown the scheduler and release resources
     */
    void shutdown();

    /**
     * Types of handler executions that can be performed asynchronously
     */
    enum HandlerExecution {
        ON_ENTER,
        ON_EXIT,
        ON_UPDATE,
        HANDLE_EVENT
    }
}
