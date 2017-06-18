package com.vajsoft.semaforky.scheduler;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import com.vajsoft.semaforky.activities.MainActivity;
import com.vajsoft.semaforky.controllers.MainController;
import com.vajsoft.semaforky.controllers.SemaforkyMachine;
import com.vajsoft.semaforky.data.Settings;

import java.util.Date;
import java.util.PriorityQueue;
import java.util.Timer;
import java.util.TimerTask;

/** Plans events in time and control main controller and main activity using settings.
 * */
public class Scheduler {
    private MainController mainController;

    public Scheduler(MainController mainController) {
        this.mainController = mainController;

        this.setTimer = new Timer();
        this.setTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                UpdateControllers();
            }
        }, 0, 50);
    }

    Timer setTimer = null;
    PriorityQueue<Event> events = new PriorityQueue<Event>();

    /// Timer event handler
    protected void UpdateControllers() {
        Date now = new Date();

        while (!events.isEmpty() && now.getTime() > events.peek().time.getTime()) {
            Event event = events.peek();
            event.run();
            events.remove(event);
        }
    }

    public void AddEvent(Event event) {
        events.add(event);
    }

    public void RemoveAllEventsByClass(Class eventType) {
        Object[] eventsArray = events.toArray();
        for(int i = 0; i < eventsArray.length; i++) {
            if(eventsArray[i].getClass().equals(eventType)) {
                events.remove(eventsArray[i]);
            }
        }
    }

    public void StartRound() {
        AddEvent(new RoundClockEvent(new Date(), mainController.GetMainActivity(),  new Date(), this));
    }

    public void EndRound() {
        CancelSetEvents();
        RemoveAllEventsByClass(RoundClockEvent.class);
    }

    private void CancelSetEvents() {
        RemoveAllEventsByClass(SemaphoreEvent.class);
        RemoveAllEventsByClass(SetClockEvent.class);
    }

    /// Plan events for set start
    public void StartSet() {
        CancelSetEvents();
        Date now = new Date();

        Settings settings = Settings.getInstance();
        MainActivity mainActivity = mainController.GetMainActivity();
        SemaforkyMachine machine = mainActivity.GetMachine();

        AddEvent(new SemaphoreEvent(now, SemaforkyMachine.READY, machine));
        AddEvent(new SemaphoreEvent(new Date(now.getTime() + settings.GetPreparationTimeTime() * 1000), SemaforkyMachine.FIRE, machine));
        AddEvent(new SemaphoreEvent(new Date(now.getTime() + (settings.GetPreparationTimeTime() + settings.GetSetTime() - settings.GetWarningTimeTime()) * 1000), SemaforkyMachine.WARNING, machine));
        AddEvent(new SemaphoreEvent(new Date(now.getTime() + (settings.GetPreparationTimeTime() + settings.GetSetTime()) * 1000 + 500), SemaforkyMachine.SET_STOPPED, machine));
        AddEvent(new SetClockEvent(new Date(), mainController, new Date(), this));

    }

    public void StopSet() {
        CancelSetEvents();
    }

    public void CancelSet() {
        CancelSetEvents();
    }
}
