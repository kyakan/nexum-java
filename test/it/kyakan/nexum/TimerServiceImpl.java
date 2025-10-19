package it.kyakan.nexum;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class TimerServiceImpl implements TimerService {
    private ArrayList<Runnable> task = new ArrayList<>();
    private ArrayList<Runnable> task1 = new ArrayList<>();
    public int SCHEDULED_COUNT = 0;
    public int EXECUTED_COUNT = 0;

    @Override
    public void scheduleOnce(Runnable task, long delay, TimeUnit unit) {
        SCHEDULED_COUNT++;
        this.task1.add(task);
    }

    @Override
    public void schedulePeriodically(Runnable task, long initialDelay, long period, TimeUnit unit) {
        SCHEDULED_COUNT++;
        this.task.add(task);
    }

    @Override
    public void cancel() {
        throw new UnsupportedOperationException("Cancel not implemented");
    } 

    public void trigger() {
        var task1 = (ArrayList<Runnable>)this.task1.clone();
        task1.forEach(t->t.run());
        EXECUTED_COUNT++;
    }
    
    public void trigger(int index) {
        this.task1.get(index).run();
        EXECUTED_COUNT++;
    }

    public void triggerPeriod() {
        var task = (ArrayList<Runnable>)this.task.clone();
        task.forEach(t -> t.run());
        EXECUTED_COUNT++;
    }

    public void triggerPeriod(int index) {
        this.task.get(index).run();
        EXECUTED_COUNT++;
    }
}
