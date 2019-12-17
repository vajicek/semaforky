package com.vajsoft.semaforky.scheduler;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import com.vajsoft.semaforky.Semaforky;
import com.vajsoft.semaforky.controllers.SemaforkyMachine;
import com.vajsoft.semaforky.controllers.SemaforkyState;
import com.vajsoft.semaforky.data.Settings;

import java.util.Date;
import java.util.PriorityQueue;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Plans events in time and control main controller and main activity using settings.
 */
public class Scheduler {
    private Timer setTimer = null;
    private PriorityQueue<Event> events = new PriorityQueue<>();
    private Semaforky semaforky;

    public Scheduler(Semaforky semaforky) {
        this.semaforky = semaforky;
        this.setTimer = new Timer();
        this.setTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                UpdateControllers();
            }
        }, 0, 50);
    }

    public void StartRound() {
        AddEvent(new RoundClockEvent(new Date(), new Date(), semaforky));
    }

    public void AddEvent(Event event) {
        events.add(event);
    }

    public void EndRound() {
        CancelSetEvents();
        RemoveAllEventsByClass(RoundClockEvent.class);
    }

    public void RemoveAllEventsByClass(Class eventType) {
        Object[] eventsArray = events.toArray();
        for (int i = 0; i < eventsArray.length; i++) {
            if (eventsArray[i].getClass().equals(eventType)) {
                events.remove(eventsArray[i]);
            }
        }
    }

    /// Plan events for set start
    public void StartSet() {
        CancelSetEvents();
        Date now = new Date();

        Settings settings = semaforky.getSettings();
        SemaforkyMachine machine = semaforky.getMachine();

        AddEvent(new SemaphoreEvent(now,
                SemaforkyState.READY,
                machine));
        AddEvent(new SemaphoreEvent(new Date(now.getTime() + settings.getPreparationTimeTime() * 1000),
                SemaforkyState.FIRE,
                machine));
        AddEvent(new SemaphoreEvent(new Date(now.getTime() + (settings.getPreparationTimeTime() + settings.getSetTime() - settings.getWarningTimeTime()) * 1000),
                SemaforkyState.WARNING,
                machine));
        AddEvent(new SemaphoreEvent(new Date(now.getTime() + (settings.getPreparationTimeTime() + settings.getSetTime()) * 1000 + 500),
                SemaforkyState.SET_STOPPED,
                machine));
        AddEvent(new SetClockEvent(new Date(), new Date(), semaforky));
    }

    public void StopSet() {
        CancelSetEvents();
    }

    public void CancelSet() {
        CancelSetEvents();
    }

    /// Timer event handler
    protected void UpdateControllers() {
        Date now = new Date();

        while (!events.isEmpty() && now.getTime() > events.peek().time.getTime()) {
            Event event = events.peek();
            event.run();
            events.remove(event);
        }
    }

    private void CancelSetEvents() {
        RemoveAllEventsByClass(SemaphoreEvent.class);
        RemoveAllEventsByClass(SetClockEvent.class);
    }
}
