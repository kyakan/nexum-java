package it.kyakan.nexum;

import java.util.concurrent.TimeUnit;

public class TimerServiceImpl implements TimerService {
    private Runnable task;
    private Runnable taskP;
    private Runnable task1;
    private Runnable taskP1;
    public int SCHEDULED_COUNT = 0;
    public int EXECUTED_COUNT = 0;

    @Override
    public void scheduleOnce(Runnable task, long delay, TimeUnit unit) {
        SCHEDULED_COUNT++;
        this.taskP1 = this.task;
        this.task1 = task;
    }

    @Override
    public void schedulePeriodically(Runnable task, long initialDelay, long period, TimeUnit unit) {
        SCHEDULED_COUNT++;
        this.taskP = this.task;
        this.task = task;
    }

    @Override
    public void cancel() {
        throw new UnsupportedOperationException("Cancel not implemented");
    }

    public void trigger() {
        EXECUTED_COUNT++;
        this.task1.run();
    }

    public void triggerPrevious() {
        EXECUTED_COUNT++;
        this.taskP1.run();
    }

    public void triggerPeriod() {
        EXECUTED_COUNT++;
        this.task.run();
    }

    public void triggerPreviousPeriod() {
        EXECUTED_COUNT++;
        this.taskP.run();
    }
}
