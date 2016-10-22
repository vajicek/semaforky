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

public class Scheduler {
    private MainController mainController;
    private MainActivity mainActivity;
    private Settings settings;

    public Scheduler(MainController mainController, MainActivity mainActivity, Settings settings) {
        this.mainController = mainController;
        this.mainActivity = mainActivity;
        this.settings = settings;
    }

    //TIMER
    Timer setTimer = null;
    Timer roundTimer = null;

    Date setStart;

    public void StartRound() {
        //timer;
    }

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

    PriorityQueue<Event> events = new PriorityQueue<Event>();

    public void UpdateControllers(long now) {
        if (events.isEmpty()) {
            return;
        }

        while (now > events.peek().time) {
            if (events.peek() instanceof SemaphoreEvent) {
                mainController.SetupSemaphores(events.peek().status);
                events.remove();
            } else if (events.peek() instanceof ClockEvent) {
                mainActivity.LogMessage("clock event!");
                mainController.SetupClocks((int)(now / 1000));
                events.remove();
                events.add(new ClockEvent((now / 1000 + 1) * 1000, 666));
            }
        }
    }

    private void StopSetTimer() {
        if (setTimer!=null) {
            setTimer.cancel();
            setTimer.purge();
            setTimer = null;
        }
    }

    public void StartSet() {
        StopSetTimer();

        events.clear();
        events.add(new SemaphoreEvent(settings.yellowTime * 1000, 2));
        events.add(new SemaphoreEvent(settings.greenTime * 1000, 1));
        events.add(new SemaphoreEvent(settings.redTime * 1000, 3));
        events.add(new ClockEvent(0, 0));

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

    public void StopSet() {
        StopSetTimer();
    }

    public void CancelSet() {
        StopSetTimer();
    }
}
