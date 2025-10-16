package it.kyakan.nexum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Generic state machine implementation.
 * This class provides a flexible state machine that can work with any state and
 * event types.
 * 
 * @param <S> The type of the state (typically an Enum or String)
 * @param <E> The type of the event (typically an Enum or String)
 */
public class Nexum<S, E> {

    private final NexumContext<S> context;
    private final List<Transition<S, E>> transitions;
    private final Map<S, StateHandler<S, E>> stateHandlers;
    private final List<NexumListener<S, E>> listeners;
    private final ReentrantLock lock;
    private boolean started;
    private StateHandler<S, E> defaultHandler = null;

    /**
     * Create a new state machine with the given initial state
     * 
     * @param initialState The initial state
     */
    public Nexum(S initialState) {
        this.context = new NexumContext<>(initialState);
        this.transitions = new ArrayList<>();
        this.stateHandlers = new HashMap<>();
        this.listeners = new CopyOnWriteArrayList<>();
        this.lock = new ReentrantLock();
        this.started = false;
    }

    /**
     * Add a transition to the state machine
     * 
     * @param transition The transition to add
     * @return This state machine for method chaining
     */
    public Nexum<S, E> addTransition(Transition<S, E> transition) {
        lock.lock();
        try {
            transitions.add(transition);
            return this;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Add a simple transition without guards or actions
     * 
     * @param fromState The source state
     * @param toState   The target state
     * @param event     The event that triggers the transition
     * @return This state machine for method chaining
     */
    public Nexum<S, E> addTransition(S fromState, S toState, E event) {
        return addTransition(new Transition<>(fromState, toState, event));
    }

    /**
     * Add a transition with a guard condition
     * 
     * @param fromState The source state
     * @param toState   The target state
     * @param event     The event that triggers the transition
     * @param guard     The guard condition
     * @return This state machine for method chaining
     */
    public Nexum<S, E> addTransition(S fromState, S toState, E event,
            Transition.TransitionGuard<S, E> guard) {
        return addTransition(new Transition<>(fromState, toState, event, guard));
    }

    /**
     * Add a transition with a guard and action
     * 
     * @param fromState The source state
     * @param toState   The target state
     * @param event     The event that triggers the transition
     * @param guard     The guard condition
     * @param action    The action to execute
     * @return This state machine for method chaining
     */
    public Nexum<S, E> addTransition(S fromState, S toState, E event,
            Transition.TransitionGuard<S, E> guard,
            Transition.TransitionAction<S, E> action) {
        return addTransition(new Transition<>(fromState, toState, event, guard, action));
    }

    /**
     * Register a state handler
     * 
     * @param handler The state handler
     * @return This state machine for method chaining
     */
    public Nexum<S, E> registerStateHandler(StateHandler<S, E> handler) {
        lock.lock();
        try {
            stateHandlers.put(handler.getState(), handler);
            return this;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Set a default handler for undefined transitions.
     * If no transition is found for an event, this handler will be invoked.
     * If no default handler is set, a NexumException will be thrown (default
     * behavior).
     *
     * @param handler The default handler to use for undefined transitions
     * @return This state machine for method chaining
     */
    public Nexum<S, E> setDefaultHandler(StateHandler<S, E> handler) {
        lock.lock();
        try {
            this.defaultHandler = handler;
            return this;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Add a listener to receive state machine events
     * 
     * @param listener The listener to add
     * @return This state machine for method chaining
     */
    public Nexum<S, E> addListener(NexumListener<S, E> listener) {
        listeners.add(listener);
        return this;
    }

    /**
     * Remove a listener
     * 
     * @param listener The listener to remove
     * @return This state machine for method chaining
     */
    public Nexum<S, E> removeListener(NexumListener<S, E> listener) {
        listeners.remove(listener);
        return this;
    }

    /**
     * Start the state machine
     * This will call the onEnter handler for the initial state
     */
    public void start() {
        lock.lock();
        try {
            if (started) {
                return;
            }
            started = true;

            S currentState = context.getCurrentState();
            StateHandler<S, E> handler = stateHandlers.get(currentState);
            if (handler != null) {
                handler.onEnter(context, null, null);
            }

            notifyStateChanged(null, currentState, null);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Fire an event to trigger a state transition
     * 
     * @param event The event to fire
     * @throws NexumException If the transition fails
     */
    public void fireEvent(E event) throws NexumException {
        fireEvent(event, null);
    }

    /**
     * Fire an event with associated data
     * 
     * @param event     The event to fire
     * @param eventData Optional data associated with the event
     * @throws NexumException If the transition fails
     */
    public void fireEvent(E event, Object eventData) throws NexumException {
        lock.lock();
        try {
            if (!started) {
                throw new NexumException("State machine not started");
            }

            S currentState = context.getCurrentState();

            // First, let the current state handler try to handle the event
            StateHandler<S, E> currentHandler = stateHandlers.get(currentState);
            if (currentHandler != null && currentHandler.handleEvent(context, event, eventData)) {
                // Event was handled by the state handler, no transition needed
                return;
            }

            // Find a matching transition
            Transition<S, E> matchingTransition = null;
            for (Transition<S, E> transition : transitions) {
                if (transition.matches(currentState, event)) {
                    if (transition.canTransition(context, eventData)) {
                        matchingTransition = transition;
                        break;
                    }
                }
            }

            if (matchingTransition == null) {
                if (defaultHandler != null) {
                    defaultHandler.handleEvent(context, event, eventData);
                    return;
                } else {
                    throw new NexumException(
                            "No valid transition found for event: " + event,
                            currentState,
                            event);
                }
            }

            // Execute the transition
            executeTransition(matchingTransition, event, eventData);

        } catch (Exception e) {
            context.setLastError(e);
            notifyError(e);
            if (e instanceof NexumException) {
                throw (NexumException) e;
            }
            throw new NexumException("Error during state transition", e,
                    context.getCurrentState(), event);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Execute a transition
     */
    private void executeTransition(Transition<S, E> transition, E event, Object eventData) {
        S fromState = context.getCurrentState();
        S toState = transition.getToState();

        // Call onExit for the current state
        StateHandler<S, E> fromHandler = stateHandlers.get(fromState);
        if (fromHandler != null) {
            fromHandler.onExit(context, toState, event);
        }

        // Execute transition action
        transition.executeAction(context, eventData);

        // Update the state
        context.setCurrentState(toState);

        // Call onEnter for the new state
        StateHandler<S, E> toHandler = stateHandlers.get(toState);
        if (toHandler != null) {
            toHandler.onEnter(context, fromState, event);
        }

        // Notify listeners
        notifyStateChanged(fromState, toState, event);
    }

    /**
     * Get the current state
     * 
     * @return The current state
     */
    public S getCurrentState() {
        return context.getCurrentState();
    }

    /**
     * Get the state machine context
     * 
     * @return The context
     */
    public NexumContext<S> getContext() {
        return context;
    }

    /**
     * Check if the state machine has been started
     * 
     * @return true if started
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * Reset the state machine to a new state
     * This will call exit handlers for the current state and enter handlers for the
     * new state
     * 
     * @param newState The new state to reset to
     */
    public void reset(S newState) {
        lock.lock();
        try {
            S oldState = context.getCurrentState();

            // Call onExit for current state
            StateHandler<S, E> oldHandler = stateHandlers.get(oldState);
            if (oldHandler != null) {
                oldHandler.onExit(context, newState, null);
            }

            // Clear context data
            context.clear();
            context.clearLastError();

            // Set new state
            context.setCurrentState(newState);

            // Call onEnter for new state
            StateHandler<S, E> newHandler = stateHandlers.get(newState);
            if (newHandler != null) {
                newHandler.onEnter(context, oldState, null);
            }

            notifyStateChanged(oldState, newState, null);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Notify listeners of a state change
     */
    private void notifyStateChanged(S fromState, S toState, E event) {
        for (NexumListener<S, E> listener : listeners) {
            try {
                listener.onStateChanged(fromState, toState, event);
            } catch (Exception e) {
                // Log but don't propagate listener exceptions
                System.err.println("Error in state machine listener: " + e.getMessage());
            }
        }
    }

    /**
     * Notify listeners of an error
     */
    private void notifyError(Exception error) {
        for (NexumListener<S, E> listener : listeners) {
            try {
                listener.onError(error);
            } catch (Exception e) {
                // Log but don't propagate listener exceptions
                System.err.println("Error in state machine listener: " + e.getMessage());
            }
        }
    }

    /**
     * Interface for listening to state machine events
     * 
     * @param <S> The state type
     * @param <E> The event type
     */
    public interface NexumListener<S, E> {
        /**
         * Called when the state changes
         * 
         * @param fromState The previous state (can be null for initial state)
         * @param toState   The new state
         * @param event     The event that triggered the change (can be null)
         */
        void onStateChanged(S fromState, S toState, E event);

        /**
         * Called when an error occurs
         * 
         * @param error The error that occurred
         */
        default void onError(Exception error) {
            // Default implementation does nothing
        }
    }
}