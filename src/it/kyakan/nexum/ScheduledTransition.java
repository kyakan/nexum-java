package it.kyakan.nexum;

import java.util.concurrent.TimeUnit;

/**
 * Represents a scheduled transition in the state machine.
 * A scheduled transition is triggered automatically after a delay when the machine is in a specific state.
 *
 * @param <S> The type of the state
 * @param <E> The type of the event
 */
public class ScheduledTransition<S, E> extends Transition<S, E> {
    private final long delay;
    private final TimeUnit unit;
    private final TimerService timerService;
    private final Nexum<S, E> nexum;
    private Runnable scheduledTask;
    private volatile boolean isCancelled = false;

    /**
     * Create a new scheduled transition
     *
     * @param fromState The source state
     * @param toState The target state
     * @param event The event that triggers this transition
     * @param delay The delay before triggering the transition
     * @param unit The time unit of the delay
     * @param timerService The timer service to use for scheduling
     * @param nexum The state machine instance
     */
    public ScheduledTransition(S fromState, S toState, E event, long delay, TimeUnit unit, TimerService timerService, Nexum<S, E> nexum) {
        this(fromState, toState, event, delay, unit, timerService, nexum, null, null);
    }

    /**
     * Create a new scheduled transition with a guard condition
     *
     * @param fromState The source state
     * @param toState The target state
     * @param event The event that triggers this transition
     * @param delay The delay before triggering the transition
     * @param unit The time unit of the delay
     * @param timerService The timer service to use for scheduling
     * @param nexum The state machine instance
     * @param guard The guard condition (can be null)
     */
    public ScheduledTransition(S fromState, S toState, E event, long delay, TimeUnit unit, TimerService timerService, Nexum<S, E> nexum, TransitionGuard<S, E> guard) {
        this(fromState, toState, event, delay, unit, timerService, nexum, guard, null);
    }

    /**
     * Create a new scheduled transition with a guard and action
     *
     * @param fromState The source state
     * @param toState The target state
     * @param event The event that triggers this transition
     * @param delay The delay before triggering the transition
     * @param unit The time unit of the delay
     * @param timerService The timer service to use for scheduling
     * @param nexum The state machine instance
     * @param guard The guard condition (can be null)
     * @param action The action to execute during transition (can be null)
     */
    public ScheduledTransition(S fromState, S toState, E event, long delay, TimeUnit unit, TimerService timerService, Nexum<S, E> nexum, TransitionGuard<S, E> guard, TransitionAction<S, E> action) {
        super(fromState, toState, event, guard, action);
        this.delay = delay;
        this.unit = unit;
        this.timerService = timerService;
        this.nexum = nexum;
    }

    /**
     * Get the delay before triggering the transition
     *
     * @return The delay
     */
    public long getDelay() {
        return delay;
    }

    /**
     * Get the time unit of the delay
     *
     * @return The time unit
     */
    public TimeUnit getUnit() {
        return unit;
    }

    /**
     * Schedule this transition to be triggered after the delay
     */
    public void schedule() {
        if (timerService == null) {
            throw new IllegalStateException("No timer service available");
        }
        scheduledTask = () -> {
            if (!isCancelled) {
                try {
                    // Trigger the transition by firing the event
                    nexum.fireEvent(getEvent());
                } catch (Exception e) {
                    throw new RuntimeException("Failed to trigger scheduled transition", e);
                }
            }
        };
        timerService.scheduleOnce(scheduledTask, delay, unit);
    }

    /**
     * Cancel the scheduled transition
     */
    public void cancel() {
        isCancelled = true;
    }
}