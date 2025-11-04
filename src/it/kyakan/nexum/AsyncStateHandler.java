package it.kyakan.nexum;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for state handlers that support asynchronous execution.
 * Extends StateHandler with async versions of handler methods.
 * 
 * @param <S> The type of the state
 * @param <E> The type of the event
 */
public interface AsyncStateHandler<S, E> extends StateHandler<S, E> {

    /**
     * Called when entering this state asynchronously
     * @param context The state machine context
     * @param fromState The previous state (can be null for initial state)
     * @param event The event that triggered the transition (can be null)
     * @return A CompletableFuture that completes when the async onEnter finishes
     */
    default CompletableFuture<Void> onEnterAsync(NexumContext<S> context, S fromState, E event) {
        // Default implementation executes synchronously and returns completed future
        onEnter(context, fromState, event);
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Called when exiting this state asynchronously
     * @param context The state machine context
     * @param toState The next state
     * @param event The event that triggered the transition
     * @return A CompletableFuture that completes when the async onExit finishes
     */
    default CompletableFuture<Void> onExitAsync(NexumContext<S> context, S toState, E event) {
        // Default implementation executes synchronously and returns completed future
        onExit(context, toState, event);
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Called while in this state asynchronously (can be used for periodic checks or updates)
     * @param context The state machine context
     * @return A CompletableFuture that completes when the async onUpdate finishes
     */
    default CompletableFuture<Void> onUpdateAsync(NexumContext<S> context) {
        // Default implementation executes synchronously and returns completed future
        onUpdate(context);
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Handle an event while in this state asynchronously
     * This can be used for state-specific event handling that doesn't trigger transitions
     * @param context The state machine context
     * @param event The event
     * @param eventData The event data
     * @return A CompletableFuture that resolves to true if the event was handled, false otherwise
     */
    default CompletableFuture<Boolean> handleEventAsync(NexumContext<S> context, E event, Object eventData) {
        // Default implementation executes synchronously and returns completed future
        boolean handled = handleEvent(context, event, eventData);
        return CompletableFuture.completedFuture(handled);
    }
}