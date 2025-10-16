package it.kyakan.nexum;

/**
 * Interface for handling state-specific logic.
 * Implementations can define what happens when entering, exiting, or while in a state.
 * 
 * @param <S> The type of the state
 * @param <E> The type of the event
 */
public interface StateHandler<S, E> {
    
    /**
     * Called when entering this state
     * @param context The state machine context
     * @param fromState The previous state (can be null for initial state)
     * @param event The event that triggered the transition (can be null)
     */
    default void onEnter(NexumContext<S> context, S fromState, E event) {
        // Default implementation does nothing
    }
    
    /**
     * Called when exiting this state
     * @param context The state machine context
     * @param toState The next state
     * @param event The event that triggered the transition
     */
    default void onExit(NexumContext<S> context, S toState, E event) {
        // Default implementation does nothing
    }
    
    /**
     * Called while in this state (can be used for periodic checks or updates)
     * @param context The state machine context
     */
    default void onUpdate(NexumContext<S> context) {
        // Default implementation does nothing
    }
    
    /**
     * Handle an event while in this state
     * This can be used for state-specific event handling that doesn't trigger transitions
     * @param context The state machine context
     * @param event The event
     * @param eventData The event data
     * @return true if the event was handled, false otherwise
     */
    default boolean handleEvent(NexumContext<S> context, E event, Object eventData) {
        // Default implementation does not handle events
        return false;
    }
    
    /**
     * Get the state this handler is for
     * @return The state
     */
    S getState();
}