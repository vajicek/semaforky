package com.vajsoft.semaforky.scheduler;

import android.util.Log;

import com.vajsoft.semaforky.activities.MainActivity;
import com.vajsoft.semaforky.controllers.MainController;
import com.vajsoft.semaforky.data.Settings;

import java.util.Date;
import java.util.PriorityQueue;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by vajicek on 10/21/2016.
 */

/**
 * Plans events in time and control main controller and main activity using settings.
 */
public class Scheduler {
    private MainController mainController;
    private MainActivity mainActivity;
    private Settings settings;

    public Scheduler(MainController mainController, MainActivity mainActivity, Settings settings) {
        this.mainController = mainController;
        this.mainActivity = mainActivity;
        this.settings = settings;

        this.setTimer = new Timer();
        this.setTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                UpdateControllers();
            }
        }, 0, 100);
    }

    // TIMERS
    Timer setTimer = null;
    Date start;

    /// Planned events
    PriorityQueue<Event> events = new PriorityQueue<Event>();

    /// Timer event handler
    protected void UpdateControllers() {
        if (events.isEmpty()) {
            return;
        }

        Date now = new Date();

        while (now.getTime() > events.peek().time.getTime()) {
            events.peek().run();
            events.remove();
        }
        Log.d("Update ", "");
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
        AddEvent(new RoundClockEvent(new Date(), 0, mainActivity,  new Date(), this));
    }

    public void EndRound() {
    }

    private void CancelSetEvents() {
        RemoveAllEventsByClass(SemaphoreEvent.class);
        RemoveAllEventsByClass(SetClockEvent.class);
    }

    /// Plan events for set start
    public void StartSet() {
        CancelSetEvents();
        Date now = new Date();

        AddEvent(new SemaphoreEvent(now, SemaphoreEvent.RED, mainController));
        AddEvent(new SemaphoreEvent(new Date(now.getTime() + settings.GetPreparationTimeTime() * 1000), SemaphoreEvent.GREEN, mainController));
        AddEvent(new SemaphoreEvent(new Date(now.getTime() + (settings.GetSetTime() - settings.GetWarningTimeTime()) * 1000), SemaphoreEvent.YELLOW, mainController));
        AddEvent(new SetClockEvent(new Date(), 0, settings, mainActivity, mainController, new Date(), this));
    }

    public void StopSet() {
        CancelSetEvents();
    }

    public void CancelSet() {
        CancelSetEvents();
    }
}
