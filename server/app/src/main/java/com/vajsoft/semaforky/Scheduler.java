package com.vajsoft.semaforky;

import android.os.Build;

import java.util.Comparator;
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
    }

    // TIMERS
    Timer setTimer = null;
    Timer roundTimer = null;

    Date setStart;
    Date roundStart;

    class Event implements Comparable {
        public long time;
        public int status;
        Event(long time, int status) {
            this.time = time;
            this.status = status;
        }

        @Override
        public int compareTo(Object o) {
            if (time < ((Event)o).time) return -1;
            if (time > ((Event)o).time) return 1;
            return 0;
        }
    };

    class SemaphoreEvent extends Event {
        SemaphoreEvent(long time, int status) {
            super(time, status);
        }
    };

    class ClockEvent extends Event {
        ClockEvent(long time, int status) {
            super(time, status);
        }
    };

    /// Planned events
    PriorityQueue<Event> events = new PriorityQueue<Event>();

    /// Timer event handler
    protected void UpdateControllers(long now) {
        if (events.isEmpty()) {
            return;
        }

        while (now > events.peek().time) {
            if (events.peek() instanceof SemaphoreEvent) {
                mainController.SetupSemaphores(events.peek().status);
                events.remove();
            } else if (events.peek() instanceof ClockEvent) {
                mainActivity.LogMessage("clock event!");
                mainController.SetupClocks(Math.max(settings.GetSetTime() - (int)(now / 1000), 0));
                events.remove();
                events.add(new ClockEvent((now / 1000 + 1) * 1000, 666));
            }
        }
    }

    private void StartSetTimer() {
        setStart = new Date();
        setTimer = new Timer();
        setTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                mainActivity.UpdateClocks(setStart);
                UpdateControllers(new Date().getTime() - setStart.getTime());
            }
        }, 0, 100);
    }

    private void StopSetTimer() {
        if (setTimer != null) {
            setTimer.cancel();
            setTimer.purge();
            setTimer = null;
        }
    }

    public void StartRound() {
        roundStart = new Date();
        roundTimer = new Timer();
        roundTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                mainActivity.UpdateRoundClocks(roundStart);
            }
        }, 0, 100);
    }

    public void EndRound() {
    }

    /// Plan events for set start
    public void StartSet() {
        StopSetTimer();

        events.clear();
        events.add(new SemaphoreEvent(settings.GetPreparationTimeTime() * 1000, 2));
        events.add(new SemaphoreEvent((settings.GetSetTime() - settings.GetPreparationTimeTime() - settings.GetWarningTimeTime()) * 1000, 1));
        events.add(new SemaphoreEvent(settings.GetWarningTimeTime() * 1000, 3));
        events.add(new ClockEvent(0, 0));

        StartSetTimer();
    }

    public void StopSet() {
        StopSetTimer();
    }

    public void CancelSet() {
        StopSetTimer();
    }
}
