package com.vajsoft.semaforky.scheduler;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import com.vajsoft.semaforky.activities.MainActivity;
import com.vajsoft.semaforky.controllers.MainController;
import com.vajsoft.semaforky.controllers.SemaforkyMachine;
import com.vajsoft.semaforky.controllers.SemaforkyState;
import com.vajsoft.semaforky.data.Settings;

import java.util.Date;

/** Set Clock event. Sets GUI and device controllers values. */
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
        mainActivity = mainController.getMainActivity();
        setStart = start;
        scheduler = sch;
    }

    public void run() {
        Date now = new Date();
        long seconds = (now.getTime() - setStart.getTime()) / 1000;
        int remaining_seconds = getRemainingSeconds(seconds);

        mainActivity.logMessage("clock event!");
        mainController.updateClocks(remaining_seconds);
        mainActivity.updateSetClocks(remaining_seconds);
        scheduler.AddEvent(new SetClockEvent(new Date(now.getTime() + 100), mainController, setStart, scheduler));
    }

    /** Compute remaining seconds based on total seconds from start. */
    private int getRemainingSeconds(long seconds) {
        int remaining_seconds = 0;
        if (mainActivity.getMachine().getCurrenState().name.equals(SemaforkyState.READY)) {
            remaining_seconds = (int) Math.max(settings.GetPreparationTimeTime() - seconds, 0);
        } else if (mainActivity.getMachine().getCurrenState().name.equals(SemaforkyState.FIRE) ||
                mainActivity.getMachine().getCurrenState().name.equals(SemaforkyState.WARNING)) {
            remaining_seconds = (int) Math.max(settings.GetPreparationTimeTime() + settings.GetSetTime() - seconds, 0);
        }
        return remaining_seconds;
    }
};
