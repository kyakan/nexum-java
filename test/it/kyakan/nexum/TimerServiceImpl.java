package it.kyakan.nexum;

import java.util.concurrent.TimeUnit;

public class TimerServiceImpl implements TimerService {
    private Runnable task;
    public int SCHEDULED_COUNT = 0;
    public int EXECUTED_COUNT = 0;

    @Override
    public void scheduleOnce(Runnable task, long delay, TimeUnit unit) {
        SCHEDULED_COUNT++;
        this.task = task;
    }

    @Override
    public void schedulePeriodically(Runnable task, long initialDelay, long period, TimeUnit unit) {
        SCHEDULED_COUNT++;
        this.task = task;
    }

    @Override
    public void cancel() {
        throw new UnsupportedOperationException("Cancel not implemented");
    }

    public void trigger() {
        EXECUTED_COUNT++;
        this.task.run();
    }
}
