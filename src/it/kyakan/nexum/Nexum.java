package it.kyakan.nexum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
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
    private final List<ScheduledTransition<S, E>> scheduledTransitions;
    private final Map<S, StateHandler<S, E>> stateHandlers;
    private final List<NexumListener<S, E>> listeners;
    private final ReentrantLock lock;
    private boolean started;
    private StateHandler<S, E> defaultHandler = null;
    private TimerService timerService;

    /**
     * Create a new state machine with the given initial state
     *
     * @param initialState The initial state
     */
    public Nexum(S initialState) {
        this(initialState, null);
    }

    /**
     * Create a new state machine with the given initial state and timer service
     *
     * @param initialState The initial state
     * @param timerService The timer service to use for scheduling events
     */
    public Nexum(S initialState, TimerService timerService) {
        this.context = new NexumContext<>(initialState);
        this.transitions = new ArrayList<>();
        this.scheduledTransitions = new ArrayList<>();
        this.stateHandlers = new HashMap<>();
        this.listeners = new CopyOnWriteArrayList<>();
        this.lock = new ReentrantLock();
        this.started = false;
        this.timerService = timerService;
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
     * Add multiple transitions from an array of source states to a single target state
     * This creates one transition for each source state to the target state with the same event
     *
     * @param fromStates Array of source states
     * @param toState    The target state
     * @param event      The event that triggers the transitions
     * @return This state machine for method chaining
     */
    public Nexum<S, E> addTransition(S[] fromStates, S toState, E event) {
        for (S fromState : fromStates) {
            addTransition(fromState, toState, event);
        }
        return this;
    }

    /**
     * Add multiple transitions from an array of source states to a single target state with a guard
     *
     * @param fromStates Array of source states
     * @param toState    The target state
     * @param event      The event that triggers the transitions
     * @param guard      The guard condition
     * @return This state machine for method chaining
     */
    public Nexum<S, E> addTransition(S[] fromStates, S toState, E event,
            Transition.TransitionGuard<S, E> guard) {
        for (S fromState : fromStates) {
            addTransition(fromState, toState, event, guard);
        }
        return this;
    }

    /**
     * Add multiple transitions from an array of source states to a single target state with a guard and action
     *
     * @param fromStates Array of source states
     * @param toState    The target state
     * @param event      The event that triggers the transitions
     * @param guard      The guard condition
     * @param action     The action to execute
     * @return This state machine for method chaining
     */
    public Nexum<S, E> addTransition(S[] fromStates, S toState, E event,
            Transition.TransitionGuard<S, E> guard,
            Transition.TransitionAction<S, E> action) {
        for (S fromState : fromStates) {
            addTransition(fromState, toState, event, guard, action);
        }
        return this;
    }

    /**
     * Add multiple transitions from a single source state to a single target state with multiple events
     * This creates one transition for each event from the source state to the target state
     *
     * @param fromState The source state
     * @param toState   The target state
     * @param events    Array of events that trigger the transitions
     * @return This state machine for method chaining
     */
    public Nexum<S, E> addTransition(S fromState, S toState, E[] events) {
        for (E event : events) {
            addTransition(fromState, toState, event);
        }
        return this;
    }

    /**
     * Add multiple transitions from a single source state to a single target state with multiple events and a guard
     *
     * @param fromState The source state
     * @param toState   The target state
     * @param events    Array of events that trigger the transitions
     * @param guard     The guard condition
     * @return This state machine for method chaining
     */
    public Nexum<S, E> addTransition(S fromState, S toState, E[] events,
            Transition.TransitionGuard<S, E> guard) {
        for (E event : events) {
            addTransition(fromState, toState, event, guard);
        }
        return this;
    }

    /**
     * Add multiple transitions from a single source state to a single target state with multiple events, guard and action
     *
     * @param fromState The source state
     * @param toState   The target state
     * @param events    Array of events that trigger the transitions
     * @param guard     The guard condition
     * @param action    The action to execute
     * @return This state machine for method chaining
     */
    public Nexum<S, E> addTransition(S fromState, S toState, E[] events,
            Transition.TransitionGuard<S, E> guard,
            Transition.TransitionAction<S, E> action) {
        for (E event : events) {
            addTransition(fromState, toState, event, guard, action);
        }
        return this;
    }

    /**
     * Add multiple transitions from multiple source states to a single target state with multiple events
     * This creates one transition for each combination of source state and event
     *
     * @param fromStates Array of source states
     * @param toState    The target state
     * @param events     Array of events that trigger the transitions
     * @return This state machine for method chaining
     */
    public Nexum<S, E> addTransition(S[] fromStates, S toState, E[] events) {
        for (S fromState : fromStates) {
            for (E event : events) {
                addTransition(fromState, toState, event);
            }
        }
        return this;
    }

    /**
     * Add multiple transitions from multiple source states to a single target state with multiple events and a guard
     *
     * @param fromStates Array of source states
     * @param toState    The target state
     * @param events     Array of events that trigger the transitions
     * @param guard      The guard condition
     * @return This state machine for method chaining
     */
    public Nexum<S, E> addTransition(S[] fromStates, S toState, E[] events,
            Transition.TransitionGuard<S, E> guard) {
        for (S fromState : fromStates) {
            for (E event : events) {
                addTransition(fromState, toState, event, guard);
            }
        }
        return this;
    }

    /**
     * Add multiple transitions from multiple source states to a single target state with multiple events, guard and action
     *
     * @param fromStates Array of source states
     * @param toState    The target state
     * @param events     Array of events that trigger the transitions
     * @param guard      The guard condition
     * @param action     The action to execute
     * @return This state machine for method chaining
     */
    public Nexum<S, E> addTransition(S[] fromStates, S toState, E[] events,
            Transition.TransitionGuard<S, E> guard,
            Transition.TransitionAction<S, E> action) {
        for (S fromState : fromStates) {
            for (E event : events) {
                addTransition(fromState, toState, event, guard, action);
            }
        }
        return this;
    }

    /**
     * Start building a transition from one or more source states
     * This provides a fluent API for creating transitions
     *
     * @param fromStates One or more source states
     * @return A TransitionBuilder for method chaining
     */
    @SafeVarargs
    public final TransitionBuilder from(S... fromStates) {
        return new TransitionBuilder(fromStates);
    }

    /**
     * Start building a scheduled transition from one or more source states
     * This provides a fluent API for creating scheduled transitions
     *
     * @param fromStates One or more source states
     * @return A ScheduledTransitionBuilder for method chaining
     */
    @SafeVarargs
    public final ScheduledTransitionBuilder fromScheduled(S... fromStates) {
        return new ScheduledTransitionBuilder(fromStates);
    }

    /**
     * Fluent builder for creating transitions with a more readable syntax
     */
    public class TransitionBuilder {
        private final S[] fromStates;

        @SafeVarargs
        private TransitionBuilder(S... fromStates) {
            this.fromStates = fromStates;
        }

        /**
         * Specify the target state for the transition
         *
         * @param toState The target state
         * @return A ToStateBuilder for method chaining
         */
        public ToStateBuilder to(S toState) {
            return new ToStateBuilder(fromStates, toState);
        }
    }

    /**
     * Builder for specifying the event that triggers the transition
     */
    public class ToStateBuilder {
        private final S[] fromStates;
        private final S toState;

        private ToStateBuilder(S[] fromStates, S toState) {
            this.fromStates = fromStates;
            this.toState = toState;
        }

        /**
         * Specify the event that triggers the transition
         * Returns a Nexum instance that can be used for chaining or for adding guard/action
         *
         * @param event The event
         * @return A NexumWithModifier that allows optional guard and action configuration
         */
        public NexumWithModifier on(E event) {
            // Add the basic transition immediately
            addTransition(fromStates, toState, event);
            // Return a wrapper that allows modifying the last added transition
            return new NexumWithModifier(fromStates, toState, event);
        }

        /**
         * Specify the event and guard condition (legacy method)
         *
         * @param event The event
         * @param guard The guard condition
         * @return The Nexum instance for method chaining
         */
        public Nexum<S, E> on(E event, Transition.TransitionGuard<S, E> guard) {
            return addTransition(fromStates, toState, event, guard);
        }

        /**
         * Specify the event, guard condition, and action (legacy method)
         *
         * @param event  The event
         * @param guard  The guard condition
         * @param action The action to execute
         * @return The Nexum instance for method chaining
         */
        public Nexum<S, E> on(E event, Transition.TransitionGuard<S, E> guard,
                Transition.TransitionAction<S, E> action) {
            return addTransition(fromStates, toState, event, guard, action);
        }

        /**
         * Specify multiple events that trigger the transition
         *
         * @param events The events
         * @return The Nexum instance for method chaining
         */
        @SafeVarargs
        public final Nexum<S, E> onAny(E... events) {
            return addTransition(fromStates, toState, events);
        }

        /**
         * Specify multiple events with a guard condition
         *
         * @param guard  The guard condition
         * @param events The events
         * @return The Nexum instance for method chaining
         */
        @SafeVarargs
        public final Nexum<S, E> onAny(Transition.TransitionGuard<S, E> guard, E... events) {
            return addTransition(fromStates, toState, events, guard);
        }

        /**
         * Specify multiple events with guard and action
         *
         * @param guard  The guard condition
         * @param action The action to execute
         * @param events The events
         * @return The Nexum instance for method chaining
         */
        @SafeVarargs
        public final Nexum<S, E> onAny(Transition.TransitionGuard<S, E> guard,
                Transition.TransitionAction<S, E> action, E... events) {
            return addTransition(fromStates, toState, events, guard, action);
        }
    }

    /**
     * Builder that allows modifying the last added transition with guard and action
     * This class acts as a proxy to Nexum, allowing seamless chaining without requiring build()
     */
    public class NexumWithModifier {
        private final S[] fromStates;
        private final S toState;
        private final E event;
        private Transition.TransitionGuard<S, E> guard;
        private Transition.TransitionAction<S, E> action;

        private NexumWithModifier(S[] fromStates, S toState, E event) {
            this.fromStates = fromStates;
            this.toState = toState;
            this.event = event;
        }

        /**
         * Replace the last added transition with one that includes a guard
         *
         * @param guard The guard condition
         * @return This instance for method chaining
         */
        public NexumWithModifier withGuard(Transition.TransitionGuard<S, E> guard) {
            this.guard = guard;
            // Remove the last added transitions without guard/action
            lock.lock();
            try {
                for(@SuppressWarnings("unused") var __ : fromStates) {
                    transitions.removeLast();
                }
                Nexum.this.addTransition(fromStates, toState, event, guard, action);
            } finally {
                lock.unlock();
            }
            return this;
        }

        /**
         * Replace the last added transition with one that includes an action
         *
         * @param action The action to execute
         * @return This instance for method chaining
         */
        public NexumWithModifier withAction(Transition.TransitionAction<S, E> action) {
            this.action = action;
            // Remove the last added transitions
            lock.lock();
            try {
                for(@SuppressWarnings("unused") var __ : fromStates) {
                    transitions.removeLast();
                }
                Nexum.this.addTransition(fromStates, toState, event, guard, action);
            } finally {
                lock.unlock();
            }
            return this;
        }

        /**
         * Start building a new transition
         * No need to call build() - this method automatically finalizes the current transition
         *
         * @param fromStates The source states for the next transition
         * @return A TransitionBuilder for the next transition
         */
        @SafeVarargs
        public final TransitionBuilder from(S... fromStates) {
            return Nexum.this.from(fromStates);
        }

        /**
         * Start building a new scheduled transition
         * Proxy method to allow seamless chaining
         *
         * @param fromStates The source states for the next scheduled transition
         * @return A ScheduledTransitionBuilder for the next transition
         */
        @SafeVarargs
        public final ScheduledTransitionBuilder fromScheduled(S... fromStates) {
            return Nexum.this.fromScheduled(fromStates);
        }

        /**
         * Register a state handler
         * Proxy method to allow seamless chaining
         *
         * @param handler The state handler
         * @return The parent Nexum instance for method chaining
         */
        public Nexum<S, E> registerStateHandler(StateHandler<S, E> handler) {
            return Nexum.this.registerStateHandler(handler);
        }

        /**
         * Set a default handler for undefined transitions
         * Proxy method to allow seamless chaining
         *
         * @param handler The default handler
         * @return The parent Nexum instance for method chaining
         */
        public Nexum<S, E> setDefaultHandler(StateHandler<S, E> handler) {
            return Nexum.this.setDefaultHandler(handler);
        }

        /**
         * Add a listener to receive state machine events
         * Proxy method to allow seamless chaining
         *
         * @param listener The listener to add
         * @return The parent Nexum instance for method chaining
         */
        public Nexum<S, E> addListener(NexumListener<S, E> listener) {
            return Nexum.this.addListener(listener);
        }

        /**
         * Add a scheduled transition
         * Proxy method to allow seamless chaining
         *
         * @param fromState The source state
         * @param toState   The target state
         * @param event     The event that triggers this transition
         * @param delay     The delay before triggering the transition
         * @param unit      The time unit of the delay
         * @return The parent Nexum instance for method chaining
         */
        public Nexum<S, E> addScheduledTransition(S fromState, S toState, E event, long delay, TimeUnit unit) {
            return Nexum.this.addScheduledTransition(fromState, toState, event, delay, unit);
        }

        /**
         * Add a scheduled transition with a guard condition
         * Proxy method to allow seamless chaining
         *
         * @param fromState The source state
         * @param toState   The target state
         * @param event     The event that triggers this transition
         * @param delay     The delay before triggering the transition
         * @param unit      The time unit of the delay
         * @param guard     The guard condition
         * @return The parent Nexum instance for method chaining
         */
        public Nexum<S, E> addScheduledTransition(S fromState, S toState, E event, long delay, TimeUnit unit,
                Transition.TransitionGuard<S, E> guard) {
            return Nexum.this.addScheduledTransition(fromState, toState, event, delay, unit, guard);
        }

        /**
         * Add a scheduled transition with a guard and action
         * Proxy method to allow seamless chaining
         *
         * @param fromState The source state
         * @param toState   The target state
         * @param event     The event that triggers this transition
         * @param delay     The delay before triggering the transition
         * @param unit      The time unit of the delay
         * @param guard     The guard condition
         * @param action    The action to execute
         * @return The parent Nexum instance for method chaining
         */
        public Nexum<S, E> addScheduledTransition(S fromState, S toState, E event, long delay, TimeUnit unit,
                Transition.TransitionGuard<S, E> guard, Transition.TransitionAction<S, E> action) {
            return Nexum.this.addScheduledTransition(fromState, toState, event, delay, unit, guard, action);
        }

        /**
         * Add a regular transition (non-fluent style)
         * Proxy method to allow seamless chaining
         *
         * @param fromState The source state
         * @param toState   The target state
         * @param event     The event that triggers the transition
         * @return The parent Nexum instance for method chaining
         */
        public Nexum<S, E> addTransition(S fromState, S toState, E event) {
            return Nexum.this.addTransition(fromState, toState, event);
        }

        /**
         * Start the state machine
         * Proxy method to allow seamless chaining
         */
        public void start() {
            Nexum.this.start();
        }

        /**
         * Fire an event to trigger a state transition
         * Proxy method to allow seamless chaining
         *
         * @param event The event to fire
         * @throws NexumException If the transition fails
         */
        public void fireEvent(E event) throws NexumException {
            Nexum.this.fireEvent(event);
        }

        /**
         * Fire an event with associated data
         * Proxy method to allow seamless chaining
         *
         * @param event     The event to fire
         * @param eventData Optional data associated with the event
         * @throws NexumException If the transition fails
         */
        public void fireEvent(E event, Object eventData) throws NexumException {
            Nexum.this.fireEvent(event, eventData);
        }
    }

    /**
     * Fluent builder for creating scheduled transitions
     */
    public class ScheduledTransitionBuilder {
        private final S[] fromStates;

        @SafeVarargs
        private ScheduledTransitionBuilder(S... fromStates) {
            this.fromStates = fromStates;
        }

        /**
         * Specify the target state for the scheduled transition
         *
         * @param toState The target state
         * @return A ScheduledToStateBuilder for method chaining
         */
        public ScheduledToStateBuilder to(S toState) {
            return new ScheduledToStateBuilder(fromStates, toState);
        }
    }

    /**
     * Builder for specifying the event and timing for scheduled transitions
     */
    public class ScheduledToStateBuilder {
        private final S[] fromStates;
        private final S toState;

        private ScheduledToStateBuilder(S[] fromStates, S toState) {
            this.fromStates = fromStates;
            this.toState = toState;
        }

        /**
         * Specify the event and delay for the scheduled transition
         *
         * @param event The event that triggers the transition
         * @param delay The delay before triggering the transition
         * @param unit  The time unit of the delay
         * @return A ScheduledEventBuilder for adding optional guard and action
         */
        public ScheduledEventBuilder on(E event, long delay, TimeUnit unit) {
            return new ScheduledEventBuilder(fromStates, toState, event, delay, unit);
        }
    }

    /**
     * Builder for adding optional guard and action to a scheduled transition
     */
    public class ScheduledEventBuilder {
        private final S[] fromStates;
        private final S toState;
        private final E event;
        private final long delay;
        private final TimeUnit unit;
        private Transition.TransitionGuard<S, E> guard;
        private Transition.TransitionAction<S, E> action;

        private ScheduledEventBuilder(S[] fromStates, S toState, E event, long delay, TimeUnit unit) {
            this.fromStates = fromStates;
            this.toState = toState;
            this.event = event;
            this.delay = delay;
            this.unit = unit;
            // Add basic scheduled transition immediately
            for (S fromState : fromStates) {
                addScheduledTransition(fromState, toState, event, delay, unit);
            }
        }

        /**
         * Add a guard condition to the scheduled transition
         *
         * @param guard The guard condition
         * @return This instance for method chaining
         */
        public ScheduledEventBuilder withGuard(Transition.TransitionGuard<S, E> guard) {
            this.guard = guard;
            // Remove and re-add with guard
            lock.lock();
            try {
                for (@SuppressWarnings("unused") var unused : fromStates) {
                    transitions.removeLast();
                    scheduledTransitions.removeLast();
                }              
                for (S fromState : fromStates) {
                    addScheduledTransition(fromState, toState, event, delay, unit, guard, action);
                }
            } finally {
                lock.unlock();
            }
            return this;
        }

        /**
         * Add an action to the scheduled transition
         *
         * @param action The action to execute
         * @return This instance for method chaining
         */
        public ScheduledEventBuilder withAction(Transition.TransitionAction<S, E> action) {
            this.action = action;
            // Remove and re-add with action
            lock.lock();
            try {
                for (@SuppressWarnings("unused") var unused : fromStates) {
                    transitions.removeLast();
                    scheduledTransitions.removeLast();
                }
                for (S fromState : fromStates) {
                    addScheduledTransition(fromState, toState, event, delay, unit, guard, action);
                }
            } finally {
                lock.unlock();
            }
            return this;
        }

        /**
         * Start building a new transition
         *
         * @param fromStates The source states for the next transition
         * @return A TransitionBuilder for the next transition
         */
        @SafeVarargs
        public final TransitionBuilder from(S... fromStates) {
            return Nexum.this.from(fromStates);
        }

        /**
         * Start building a new scheduled transition
         *
         * @param fromStates The source states for the next scheduled transition
         * @return A ScheduledTransitionBuilder for the next transition
         */
        @SafeVarargs
        public final ScheduledTransitionBuilder fromScheduled(S... fromStates) {
            return Nexum.this.fromScheduled(fromStates);
        }

        /**
         * Register a state handler
         *
         * @param handler The state handler
         * @return The parent Nexum instance for method chaining
         */
        public Nexum<S, E> registerStateHandler(StateHandler<S, E> handler) {
            return Nexum.this.registerStateHandler(handler);
        }

        /**
         * Add a listener
         *
         * @param listener The listener to add
         * @return The parent Nexum instance for method chaining
         */
        public Nexum<S, E> addListener(NexumListener<S, E> listener) {
            return Nexum.this.addListener(listener);
        }

        /**
         * Start the state machine
         */
        public void start() {
            Nexum.this.start();
        }
    }

    /**
     * Add a scheduled transition to the state machine
     *
     * @param fromState The source state
     * @param toState   The target state
     * @param event     The event that triggers this transition
     * @param delay     The delay before triggering the transition
     * @param unit      The time unit of the delay
     * @return This state machine for method chaining
     */
    public Nexum<S, E> addScheduledTransition(S fromState, S toState, E event, long delay, TimeUnit unit) {
        return addScheduledTransition(fromState, toState, event, delay, unit, null, null);
    }

    /**
     * Add a scheduled transition with a guard condition
     *
     * @param fromState The source state
     * @param toState   The target state
     * @param event     The event that triggers this transition
     * @param delay     The delay before triggering the transition
     * @param unit      The time unit of the delay
     * @param guard     The guard condition
     * @return This state machine for method chaining
     */
    public Nexum<S, E> addScheduledTransition(S fromState, S toState, E event, long delay, TimeUnit unit,
            Transition.TransitionGuard<S, E> guard) {
        return addScheduledTransition(fromState, toState, event, delay, unit, guard, null);
    }

    /**
     * Add a scheduled transition with a guard and action
     *
     * @param fromState The source state
     * @param toState   The target state
     * @param event     The event that triggers this transition
     * @param delay     The delay before triggering the transition
     * @param unit      The time unit of the delay
     * @param guard     The guard condition
     * @param action    The action to execute
     * @return This state machine for method chaining
     */
    public Nexum<S, E> addScheduledTransition(S fromState, S toState, E event, long delay, TimeUnit unit,
            Transition.TransitionGuard<S, E> guard, Transition.TransitionAction<S, E> action) {
        ScheduledTransition<S, E> scheduledTransition = new ScheduledTransition<>(fromState, toState, event, delay, unit, timerService, this, guard, action);
        lock.lock();
        try {
            transitions.add(scheduledTransition);
            scheduledTransitions.add(scheduledTransition);
            return this;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Add multiple scheduled transitions from an array of source states to a single target state
     *
     * @param fromStates Array of source states
     * @param toState    The target state
     * @param event      The event that triggers the transitions
     * @param delay      The delay before triggering the transition
     * @param unit       The time unit of the delay
     * @return This state machine for method chaining
     */
    public Nexum<S, E> addScheduledTransition(S[] fromStates, S toState, E event, long delay, TimeUnit unit) {
        for (S fromState : fromStates) {
            addScheduledTransition(fromState, toState, event, delay, unit);
        }
        return this;
    }

    /**
     * Add multiple scheduled transitions from an array of source states to a single target state with a guard
     *
     * @param fromStates Array of source states
     * @param toState    The target state
     * @param event      The event that triggers the transitions
     * @param delay      The delay before triggering the transition
     * @param unit       The time unit of the delay
     * @param guard      The guard condition
     * @return This state machine for method chaining
     */
    public Nexum<S, E> addScheduledTransition(S[] fromStates, S toState, E event, long delay, TimeUnit unit,
            Transition.TransitionGuard<S, E> guard) {
        for (S fromState : fromStates) {
            addScheduledTransition(fromState, toState, event, delay, unit, guard);
        }
        return this;
    }

    /**
     * Add multiple scheduled transitions from an array of source states to a single target state with a guard and action
     *
     * @param fromStates Array of source states
     * @param toState    The target state
     * @param event      The event that triggers the transitions
     * @param delay      The delay before triggering the transition
     * @param unit       The time unit of the delay
     * @param guard      The guard condition
     * @param action     The action to execute
     * @return This state machine for method chaining
     */
    public Nexum<S, E> addScheduledTransition(S[] fromStates, S toState, E event, long delay, TimeUnit unit,
            Transition.TransitionGuard<S, E> guard, Transition.TransitionAction<S, E> action) {
        for (S fromState : fromStates) {
            addScheduledTransition(fromState, toState, event, delay, unit, guard, action);
        }
        return this;
    }

    /**
     * Add multiple scheduled transitions from a single source state to a single target state with multiple events
     *
     * @param fromState The source state
     * @param toState   The target state
     * @param events    Array of events that trigger the transitions
     * @param delay     The delay before triggering the transition
     * @param unit      The time unit of the delay
     * @return This state machine for method chaining
     */
    public Nexum<S, E> addScheduledTransition(S fromState, S toState, E[] events, long delay, TimeUnit unit) {
        for (E event : events) {
            addScheduledTransition(fromState, toState, event, delay, unit);
        }
        return this;
    }

    /**
     * Add multiple scheduled transitions from a single source state to a single target state with multiple events and a guard
     *
     * @param fromState The source state
     * @param toState   The target state
     * @param events    Array of events that trigger the transitions
     * @param delay     The delay before triggering the transition
     * @param unit      The time unit of the delay
     * @param guard     The guard condition
     * @return This state machine for method chaining
     */
    public Nexum<S, E> addScheduledTransition(S fromState, S toState, E[] events, long delay, TimeUnit unit,
            Transition.TransitionGuard<S, E> guard) {
        for (E event : events) {
            addScheduledTransition(fromState, toState, event, delay, unit, guard);
        }
        return this;
    }

    /**
     * Add multiple scheduled transitions from a single source state to a single target state with multiple events, guard and action
     *
     * @param fromState The source state
     * @param toState   The target state
     * @param events    Array of events that trigger the transitions
     * @param delay     The delay before triggering the transition
     * @param unit      The time unit of the delay
     * @param guard     The guard condition
     * @param action    The action to execute
     * @return This state machine for method chaining
     */
    public Nexum<S, E> addScheduledTransition(S fromState, S toState, E[] events, long delay, TimeUnit unit,
            Transition.TransitionGuard<S, E> guard, Transition.TransitionAction<S, E> action) {
        for (E event : events) {
            addScheduledTransition(fromState, toState, event, delay, unit, guard, action);
        }
        return this;
    }

    /**
     * Add multiple scheduled transitions from multiple source states to a single target state with multiple events
     *
     * @param fromStates Array of source states
     * @param toState    The target state
     * @param events     Array of events that trigger the transitions
     * @param delay      The delay before triggering the transition
     * @param unit       The time unit of the delay
     * @return This state machine for method chaining
     */
    public Nexum<S, E> addScheduledTransition(S[] fromStates, S toState, E[] events, long delay, TimeUnit unit) {
        for (S fromState : fromStates) {
            for (E event : events) {
                addScheduledTransition(fromState, toState, event, delay, unit);
            }
        }
        return this;
    }

    /**
     * Add multiple scheduled transitions from multiple source states to a single target state with multiple events and a guard
     *
     * @param fromStates Array of source states
     * @param toState    The target state
     * @param events     Array of events that trigger the transitions
     * @param delay      The delay before triggering the transition
     * @param unit       The time unit of the delay
     * @param guard      The guard condition
     * @return This state machine for method chaining
     */
    public Nexum<S, E> addScheduledTransition(S[] fromStates, S toState, E[] events, long delay, TimeUnit unit,
            Transition.TransitionGuard<S, E> guard) {
        for (S fromState : fromStates) {
            for (E event : events) {
                addScheduledTransition(fromState, toState, event, delay, unit, guard);
            }
        }
        return this;
    }

    /**
     * Add multiple scheduled transitions from multiple source states to a single target state with multiple events, guard and action
     *
     * @param fromStates Array of source states
     * @param toState    The target state
     * @param events     Array of events that trigger the transitions
     * @param delay      The delay before triggering the transition
     * @param unit       The time unit of the delay
     * @param guard      The guard condition
     * @param action     The action to execute
     * @return This state machine for method chaining
     */
    public Nexum<S, E> addScheduledTransition(S[] fromStates, S toState, E[] events, long delay, TimeUnit unit,
            Transition.TransitionGuard<S, E> guard, Transition.TransitionAction<S, E> action) {
        for (S fromState : fromStates) {
            for (E event : events) {
                addScheduledTransition(fromState, toState, event, delay, unit, guard, action);
            }
        }
        return this;
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

            // Schedule transitions for the initial state
            scheduleTransitionsForState(currentState);

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
     * Schedule all transitions for a given state
     */
    private void scheduleTransitionsForState(S state) {
        for (ScheduledTransition<S, E> scheduledTransition : scheduledTransitions) {
            if (scheduledTransition.getFromState().equals(state)) {
                scheduledTransition.schedule();
            }
        }
    }

    /**
     * Cancel all scheduled transitions for a given state
     */
    private void cancelTransitionsForState(S state) {
        for (ScheduledTransition<S, E> scheduledTransition : scheduledTransitions) {
            if (scheduledTransition.getFromState().equals(state)) {
                scheduledTransition.cancel();
            }
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

        // Cancel scheduled transitions for the current state
        cancelTransitionsForState(fromState);

        // Execute transition action
        transition.executeAction(context, eventData);

        // Update the state
        context.setCurrentState(toState);

        // Call onEnter for the new state
        StateHandler<S, E> toHandler = stateHandlers.get(toState);
        if (toHandler != null) {
            toHandler.onEnter(context, fromState, event);
        }

        // Schedule transitions for the new state
        scheduleTransitionsForState(toState);

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
     * Gets the state machine context
     *
     * @return The context
     */
    public NexumContext<S> getContext() {
        return context;
    }

    /**
     * Sets the timer service to use for scheduling events
     *
     * @param timerService The timer service
     */
    public void setTimerService(TimerService timerService) {
        this.timerService = timerService;
    }

    /**
     * Schedules an event to be fired after a delay
     *
     * @param event The event to fire
     * @param delay The delay before firing the event
     * @param unit  The time unit of the delay
     * @throws IllegalStateException If no timer service is set
     */
    public void scheduleEvent(E event, long delay, TimeUnit unit) {
        scheduleEvent(event, null, delay, unit);
    }

    /**
     * Schedules an event to be fired after a delay with associated data
     *
     * @param event The event to fire
     * @param eventData Optional data associated with the event
     * @param delay The delay before firing the event
     * @param unit  The time unit of the delay
     * @throws IllegalStateException If no timer service is set
     */
    public void scheduleEvent(E event, Object eventData, long delay, TimeUnit unit) {
        if (timerService == null) {
            throw new IllegalStateException("No timer service set");
        }
        timerService.scheduleOnce(() -> fireEvent(event, eventData), delay, unit);
    }

    /**
     * Schedules an event to be fired periodically
     *
     * @param event         The event to fire
     * @param initialDelay The initial delay before the first firing
     * @param period        The period between firings
     * @param unit          The time unit of the delay and period
     * @throws IllegalStateException If no timer service is set
     */
    public void schedulePeriodicEvent(E event, long initialDelay, long period, TimeUnit unit) {
        schedulePeriodicEvent(event, null, initialDelay, period, unit);
    }

    /**
     * Schedules an event to be fired periodically with associated data
     *
     * @param event The event to fire
     * @param eventData Optional data associated with the event
     * @param initialDelay The initial delay before the first firing
     * @param period The period between firings
     * @param unit   The time unit of the delay and period
     * @throws IllegalStateException If no timer service is set
     */
    public void schedulePeriodicEvent(E event, Object eventData, long initialDelay, long period, TimeUnit unit) {
        if (timerService == null) {
            throw new IllegalStateException("No timer service set");
        }
        timerService.schedulePeriodically(() -> fireEvent(event, eventData), initialDelay, period, unit);
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

            // Schedule transitions for the new state
            scheduleTransitionsForState(newState);

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

    public Integer getTransactionCount() {
        return transitions.size();
    }

    public Integer getScheduledTransactionCount() {
        return scheduledTransitions.size();
    }
}