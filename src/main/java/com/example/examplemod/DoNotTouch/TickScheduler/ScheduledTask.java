package com.example.examplemod.DoNotTouch.TickScheduler;

public class ScheduledTask {
    public int delay;
    public Runnable action;

    public ScheduledTask(int delay, Runnable action) {
        this.delay = delay;
        this.action = action;
    }
}