package com.vajsoft.semaforky.scheduler;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import com.vajsoft.semaforky.Semaforky;
import com.vajsoft.semaforky.activities.MainActivity;
import com.vajsoft.semaforky.controllers.MainController;
import com.vajsoft.semaforky.controllers.SemaforkyState;
import com.vajsoft.semaforky.data.Settings;

import java.util.Date;

/** Set Clock event. Sets GUI and device controllers values. */
class SetClockEvent extends Event {
    private Settings settings;
    private Semaforky semaforky;
    private Date setStart;

    SetClockEvent(Date time, Date start, Semaforky semaforky) {
        super(time);
        this.settings = Settings.getInstance();
        this.setStart = start;
        this.semaforky = semaforky;
    }

    public void run() {
        Date now = new Date();
        long seconds = (now.getTime() - setStart.getTime()) / 1000;
        int remaining_seconds = getRemainingSeconds(seconds);

        semaforky.getMainController().updateClocks(remaining_seconds);
        semaforky.getMainActivity().updateSetClocks(remaining_seconds);
        semaforky.getScheduler().AddEvent(new SetClockEvent(new Date(now.getTime() + 100), setStart, semaforky));
    }

    /** Compute remaining seconds based on total seconds from start. */
    private int getRemainingSeconds(long seconds) {
        int remaining_seconds = 0;
        if (semaforky.getMachine().getCurrenState().name.equals(SemaforkyState.READY)) {
            remaining_seconds = (int) Math.max(settings.getPreparationTimeTime() - seconds, 0);
        } else if (semaforky.getMachine().getCurrenState().name.equals(SemaforkyState.FIRE) ||
                semaforky.getMachine().getCurrenState().name.equals(SemaforkyState.WARNING)) {
            remaining_seconds = (int) Math.max(settings.getPreparationTimeTime() + settings.getSetTime() - seconds, 0);
        }
        return remaining_seconds;
    }
};
