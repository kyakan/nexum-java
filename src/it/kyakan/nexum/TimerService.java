package it.kyakan.nexum;

import java.util.concurrent.TimeUnit;

/**
 * Interface for managing timers.
 * Allows scheduling tasks to be executed once or periodically.
 */
public interface TimerService {

    /**
     * Schedules a task to be executed once after a delay.
     *
     * @param task The task to execute.
     * @param delay The delay before execution.
     * @param unit The time unit of the delay.
     */
    void scheduleOnce(Runnable task, long delay, TimeUnit unit);

    /**
     * Schedules a task to be executed periodically.
     *
     * @param task The task to execute.
     * @param initialDelay The initial delay before the first execution.
     * @param period The interval between successive executions.
     * @param unit The time unit of the delay and interval.
     */
    void schedulePeriodically(Runnable task, long initialDelay, long period, TimeUnit unit);

    /**
     * Cancels all scheduled tasks.
     */
    void cancel();
}