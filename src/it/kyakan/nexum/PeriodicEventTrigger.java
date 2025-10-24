package it.kyakan.nexum;

import java.util.concurrent.TimeUnit;

/**
 * Represents a periodic event trigger that fires events at regular intervals
 * while the state machine is in a specific state, without causing state transitions.
 * This is useful for periodic monitoring, polling, or other recurring actions
 * that should happen while in a particular state.
 *
 * @param <S> The type of the state
 * @param <E> The type of the event
 */
public class PeriodicEventTrigger<S, E> {
    private final S state;
    private final E event;
    private final long initialDelay;
    private final long period;
    private final TimeUnit unit;
    private final TimerService timerService;
    private final Nexum<S, E> nexum;
    private final Transition.TransitionGuard<S, E> guard;
    private final Integer maxOccurrences;
    private Runnable scheduledTask;
    private volatile Boolean isScheduled = null;
    private volatile int occurrenceCount = 0;

    /**
     * Create a new periodic event trigger
     *
     * @param state The state in which this trigger is active
     * @param event The event to fire periodically
     * @param initialDelay The initial delay before the first event
     * @param period The period between events
     * @param unit The time unit of the delays
     * @param timerService The timer service to use for scheduling
     * @param nexum The state machine instance
     * @param guard Optional guard condition to control when events are fired
     * @param maxOccurrences Maximum number of times to fire the event (null or 0 for infinite)
     */
    public PeriodicEventTrigger(S state, E event, long initialDelay, long period, TimeUnit unit, 
                                TimerService timerService, Nexum<S, E> nexum, 
                                Transition.TransitionGuard<S, E> guard, Integer maxOccurrences) {
        this.state = state;
        this.event = event;
        this.initialDelay = initialDelay;
        this.period = period;
        this.unit = unit;
        this.timerService = timerService;
        this.nexum = nexum;
        this.guard = guard;
        this.maxOccurrences = maxOccurrences;
    }

    /**
     * Get the state for which this trigger is active
     *
     * @return The state
     */
    public S getState() {
        return state;
    }

    /**
     * Get the event that is fired periodically
     *
     * @return The event
     */
    public E getEvent() {
        return event;
    }

    /**
     * Get the initial delay before the first event
     *
     * @return The initial delay
     */
    public long getInitialDelay() {
        return initialDelay;
    }

    /**
     * Get the period between events
     *
     * @return The period
     */
    public long getPeriod() {
        return period;
    }

    /**
     * Get the time unit of the delays
     *
     * @return The time unit
     */
    public TimeUnit getUnit() {
        return unit;
    }

    /**
     * Get the maximum number of occurrences
     *
     * @return The maximum occurrences (null or 0 means infinite)
     */
    public Integer getMaxOccurrences() {
        return maxOccurrences;
    }

    /**
     * Get the current occurrence count
     *
     * @return The number of times the event has been fired
     */
    public int getOccurrenceCount() {
        return occurrenceCount;
    }

    /**
     * Check if the trigger has reached its maximum occurrences
     *
     * @return true if max occurrences reached, false otherwise
     */
    public boolean hasReachedMaxOccurrences() {
        return maxOccurrences != null && maxOccurrences > 0 && occurrenceCount >= maxOccurrences;
    }

    /**
     * Reset the occurrence counter
     */
    public void resetOccurrenceCount() {
        occurrenceCount = 0;
    }

    private void internallySchedule() {
        if (isScheduled == null) {
            if (this.timerService == null) {
                throw new IllegalStateException("No timer service available");
            }
            scheduledTask = () -> {
                if (isScheduled && nexum.getCurrentState().equals(state)) {
                    // Check if we've reached max occurrences
                    if (hasReachedMaxOccurrences()) {
                        cancel();
                        return;
                    }

                    // Check guard condition if present
                    if (guard != null && !guard.evaluate(nexum.getContext(), event, null)) {
                        return;
                    }

                    try {
                        occurrenceCount++;
                        this.nexum.fireEvent(event);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to trigger periodic event", e);
                    }
                }
            };
            this.timerService.schedulePeriodically(scheduledTask, initialDelay, period, unit);
        }
    }

    /**
     * Schedule this periodic event trigger to start firing events
     */
    public void schedule() {
        internallySchedule();
        isScheduled = true;
    }

    /**
     * Cancel the periodic event trigger
     */
    public void cancel() {
        internallySchedule();
        isScheduled = false;
    }
}