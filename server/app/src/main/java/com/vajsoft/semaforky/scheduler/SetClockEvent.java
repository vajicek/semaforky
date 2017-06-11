package com.vajsoft.semaforky.scheduler;

import com.vajsoft.semaforky.activities.MainActivity;
import com.vajsoft.semaforky.controllers.MainController;
import com.vajsoft.semaforky.data.Settings;

import java.util.Date;

/**
 * Created by vajicek on 11.6.17.
 */

class SetClockEvent extends Event {
    private Settings settings;
    private MainActivity mainActivity;
    private MainController mainController;
    private Date setStart;
    private Scheduler scheduler;
    SetClockEvent(Date time, int status, Settings s, MainActivity mac, MainController mco, Date start, Scheduler sch) {
        super(time, status);
        settings = s;
        mainActivity = mac;
        mainController = mco;
        setStart = start;
        scheduler = sch;
    }
    public void run() {
        Date now = new Date();
        long seconds = (now.getTime() - setStart.getTime()) / 1000;
        int remaining_seconds = (int)Math.max(settings.GetSetTime() - seconds, 0);

        mainActivity.LogMessage("clock event!");
        mainController.SetupClocks(remaining_seconds);
        mainActivity.UpdateClocks(remaining_seconds);
        scheduler.AddEvent(new SetClockEvent(new Date(now.getTime() + 200), 0, settings, mainActivity, mainController, setStart, scheduler));
    }
};
