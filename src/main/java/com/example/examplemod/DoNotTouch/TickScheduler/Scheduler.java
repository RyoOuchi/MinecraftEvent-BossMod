package com.example.examplemod.DoNotTouch.TickScheduler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Scheduler {
    private static final List<ScheduledTask> TASKS = new ArrayList<>();

    public static void schedule(int delayTicks, Runnable action) {
        TASKS.add(new ScheduledTask(delayTicks, action));
    }

    public static void tick() {
        Iterator<ScheduledTask> iterator = TASKS.iterator();

        while (iterator.hasNext()) {
            ScheduledTask task = iterator.next();
            task.delay--;

            if (task.delay <= 0) {
                task.action.run();
                iterator.remove();
            }
        }
    }
}