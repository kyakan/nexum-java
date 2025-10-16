package it.kyakan.nexum;

/**
 * Represents a state transition in the state machine.
 * A transition defines how to move from one state to another based on an event.
 * 
 * @param <S> The type of the state
 * @param <E> The type of the event
 */
public class Transition<S, E> {
    private final S fromState;
    private final S toState;
    private final E event;
    private final TransitionGuard<S, E> guard;
    private final TransitionAction<S, E> action;
    
    /**
     * Create a new transition
     * @param fromState The source state
     * @param toState The target state
     * @param event The event that triggers this transition
     */
    public Transition(S fromState, S toState, E event) {
        this(fromState, toState, event, null, null);
    }
    
    /**
     * Create a new transition with a guard condition
     * @param fromState The source state
     * @param toState The target state
     * @param event The event that triggers this transition
     * @param guard The guard condition (can be null)
     */
    public Transition(S fromState, S toState, E event, TransitionGuard<S, E> guard) {
        this(fromState, toState, event, guard, null);
    }
    
    /**
     * Create a new transition with a guard and action
     * @param fromState The source state
     * @param toState The target state
     * @param event The event that triggers this transition
     * @param guard The guard condition (can be null)
     * @param action The action to execute during transition (can be null)
     */
    public Transition(S fromState, S toState, E event, TransitionGuard<S, E> guard, TransitionAction<S, E> action) {
        this.fromState = fromState;
        this.toState = toState;
        this.event = event;
        this.guard = guard;
        this.action = action;
    }
    
    /**
     * Get the source state
     * @return The source state
     */
    public S getFromState() {
        return fromState;
    }
    
    /**
     * Get the target state
     * @return The target state
     */
    public S getToState() {
        return toState;
    }
    
    /**
     * Get the event that triggers this transition
     * @return The event
     */
    public E getEvent() {
        return event;
    }
    
    /**
     * Check if this transition can be executed based on the guard condition
     * @param context The state machine context
     * @param eventData The event data
     * @return true if the transition can be executed
     */
    public boolean canTransition(NexumContext<S> context, Object eventData) {
        if (guard == null) {
            return true;
        }
        return guard.evaluate(context, event, eventData);
    }
    
    /**
     * Execute the transition action if present
     * @param context The state machine context
     * @param eventData The event data
     */
    public void executeAction(NexumContext<S> context, Object eventData) {
        if (action != null) {
            action.execute(context, event, eventData);
        }
    }
    
    /**
     * Check if this transition matches the given state and event
     * @param state The current state
     * @param event The event
     * @return true if this transition matches
     */
    public boolean matches(S state, E event) {
        return this.fromState.equals(state) && this.event.equals(event);
    }
    
    @Override
    public String toString() {
        return "Transition{" +
                "from=" + fromState +
                ", to=" + toState +
                ", event=" + event +
                ", hasGuard=" + (guard != null) +
                ", hasAction=" + (action != null) +
                '}';
    }
    
    /**
     * Functional interface for transition guard conditions
     * @param <S> The state type
     * @param <E> The event type
     */
    @FunctionalInterface
    public interface TransitionGuard<S, E> {
        /**
         * Evaluate if the transition should be allowed
         * @param context The state machine context
         * @param event The event
         * @param eventData The event data
         * @return true if the transition should proceed
         */
        boolean evaluate(NexumContext<S> context, E event, Object eventData);
    }
    
    /**
     * Functional interface for transition actions
     * @param <S> The state type
     * @param <E> The event type
     */
    @FunctionalInterface
    public interface TransitionAction<S, E> {
        /**
         * Execute an action during the transition
         * @param context The state machine context
         * @param event The event
         * @param eventData The event data
         */
        void execute(NexumContext<S> context, E event, Object eventData);
    }
}