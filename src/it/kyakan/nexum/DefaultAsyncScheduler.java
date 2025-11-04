package it.kyakan.nexum;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;


/**
 * Default implementation of AsyncScheduler that uses a provided Executor
 */
public class DefaultAsyncScheduler implements AsyncScheduler {
    private final Executor executor;

    /**
     * Create a DefaultAsyncScheduler with the given executor
     *
     * @param executor The executor to use for asynchronous execution
     */
    public DefaultAsyncScheduler(Executor executor) {
        this.executor = executor;
    }

    @Override
    public <S, E> CompletableFuture<Void> executeAsync(
        StateHandler<S, E> handler,
        HandlerExecution execution,
        NexumContext<S> context,
        S fromState,
        S toState,
        E event,
        Object eventData
    ) {
        return CompletableFuture.runAsync(() -> {
            try {
                switch (execution) {
                    case ON_ENTER:
                        handler.onEnter(context, fromState, event);
                        break;
                    case ON_EXIT:
                        handler.onExit(context, toState, event);
                        break;
                    case ON_UPDATE:
                        handler.onUpdate(context);
                        break;
                    case HANDLE_EVENT:
                        handler.handleEvent(context, event, eventData);
                        break;
                }
            } catch (Exception e) {
                // Log the error but don't propagate to avoid breaking the state machine
                System.err.println("Error in async handler execution: " + e.getMessage());
                e.printStackTrace();
            }
        }, executor);
    }

    @Override
    public void shutdown() {
        // Default implementation does nothing - override if executor needs cleanup
    }
}
