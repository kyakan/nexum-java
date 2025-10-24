package it.kyakan.nexum;

import it.kyakan.nexum.Transition;

/**
 * Helper class to represent an event with optional guard and action for loop transitions.
 *
 * @param <S> The type of the state
 * @param <E> The type of the event
 */
public class LoopEvent<S, E> {
    public final E event;
    public final Transition.TransitionGuard<S, E> guard;
    public final Transition.TransitionAction<S, E> action;

    /**
     * Create a loop event with event, guard, and action
     *
     * @param event The event that triggers the loop transition
     * @param guard The guard condition (can be null)
     * @param action The action to execute (can be null)
     */
    public LoopEvent(E event, Transition.TransitionGuard<S, E> guard, Transition.TransitionAction<S, E> action) {
        this.event = event;
        this.guard = guard;
        this.action = action;
    }

    /**
     * Create a loop event with only an event (no guard or action)
     *
     * @param event The event that triggers the loop transition
     */
    public LoopEvent(E event) {
        this(event, null, null);
    }

    /**
     * Create a loop event with event and guard
     *
     * @param event The event that triggers the loop transition
     * @param guard The guard condition
     */
    public LoopEvent(E event, Transition.TransitionGuard<S, E> guard) {
        this(event, guard, null);
    }
}