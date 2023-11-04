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
    private final PriorityQueue<Event> events = new PriorityQueue<>();
    private final Semaforky semaforky;

    public Scheduler(final Semaforky semaforky) {
        this.semaforky = semaforky;
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                UpdateControllers();
            }
        }, 0, 50);
    }

    public void StartRound() {
        AddEvent(new RoundClockEvent(new Date(), new Date(), semaforky));
    }

    public void AddEvent(final Event event) {
        events.add(event);
    }

    public void EndRound() {
        CancelSetEvents();
        RemoveAllEventsByClass(RoundClockEvent.class);
    }

    public void RemoveAllEventsByClass(final Class<? extends Event> eventType) {
        final Event[] eventsArray = events.toArray(new Event[0]);
        for (Event o : eventsArray) {
            if (o.getClass().equals(eventType)) {
                events.remove(o);
            }
        }
    }

    /// Plan events for round start
    public void WaitForRoundStart() {
        final Settings settings = semaforky.getSettings();
        final SemaforkyMachine machine = semaforky.getMachine();

        // start round even
        AddEvent(new SemaphoreEvent(settings.getDelayedStartDate(),
                SemaforkyState.ROUND_STARTED,
                machine));

        // high frequency clock event
        AddEvent(new SetClockEvent(new Date(), new Date(), semaforky));
    }

    /// Plan events for set start
    public void StartSet() {
        Settings settings = semaforky.getSettings();
        StartSet(settings.getSetTime());
    }

    public void StartSet(int setTime) {
        CancelSetEvents();
        Date now = new Date();

        Settings settings = semaforky.getSettings();
        SemaforkyMachine machine = semaforky.getMachine();

        // plan top-level events
        AddEvent(new SemaphoreEvent(now,
                SemaforkyState.READY,
                machine));
        AddEvent(new SemaphoreEvent(new Date(now.getTime() + settings.getPreparationTimeTime() * 1000L),
                SemaforkyState.FIRE,
                machine));
        AddEvent(new SemaphoreEvent(new Date(now.getTime() + (settings.getPreparationTimeTime() + setTime - settings.getWarningTimeTime()) * 1000L),
                SemaforkyState.WARNING,
                machine));
        AddEvent(new SemaphoreEvent(new Date(now.getTime() + (settings.getPreparationTimeTime() + setTime) * 1000L + 500),
                SemaforkyState.SET_STOPPED,
                machine));

        // plan high frequency event for clock display
        AddEvent(new SetClockEvent(new Date(), new Date(), semaforky, new SetClockEvent.SetTiming(settings.getPreparationTimeTime(), setTime)));
    }

    public void StopSet() {
        CancelSetEvents();
    }

    public void CancelSet() {
        CancelSetEvents();
    }

    /// Timer event handler
    protected void UpdateControllers() {
        final Date now = new Date();

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
