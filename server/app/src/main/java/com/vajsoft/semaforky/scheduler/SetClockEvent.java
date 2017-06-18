package com.vajsoft.semaforky.scheduler;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import com.vajsoft.semaforky.activities.MainActivity;
import com.vajsoft.semaforky.controllers.MainController;
import com.vajsoft.semaforky.controllers.SemaforkyMachine;
import com.vajsoft.semaforky.data.Settings;

import java.util.Date;

/** Set Clock event. Sets GUI and device controllers values.
 * */
class SetClockEvent extends Event {
    private Settings settings;
    private MainActivity mainActivity;
    private MainController mainController;
    private Date setStart;
    private Scheduler scheduler;
    SetClockEvent(Date time, MainController mco, Date start, Scheduler sch) {
        super(time);
        settings = Settings.getInstance();
        mainController = mco;
        mainActivity = mainController.GetMainActivity();
        setStart = start;
        scheduler = sch;
    }
    public void run() {
        Date now = new Date();
        long seconds = (now.getTime() - setStart.getTime()) / 1000;

        int remaining_seconds = 0;
        if (mainActivity.GetMachine().GetCurrenState().name.equals(SemaforkyMachine.READY)) {
            remaining_seconds = (int)Math.max(settings.GetPreparationTimeTime() - seconds, 0);
        } else if (mainActivity.GetMachine().GetCurrenState().name.equals(SemaforkyMachine.FIRE) ||
                mainActivity.GetMachine().GetCurrenState().name.equals(SemaforkyMachine.WARNING)) {
            remaining_seconds = (int)Math.max(settings.GetPreparationTimeTime() + settings.GetSetTime() - seconds, 0);
        }

        mainActivity.LogMessage("clock event!");
        mainController.UpdateClocks(remaining_seconds);
        mainActivity.UpdateSetClocks(remaining_seconds);
        scheduler.AddEvent(new SetClockEvent(new Date(now.getTime() + 100), mainController, setStart, scheduler));
    }
};
