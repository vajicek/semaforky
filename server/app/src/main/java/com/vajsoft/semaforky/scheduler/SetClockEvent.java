package com.vajsoft.semaforky.scheduler;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import com.vajsoft.semaforky.Semaforky;
import com.vajsoft.semaforky.controllers.SemaforkyState;
import com.vajsoft.semaforky.data.Settings;

import java.util.Date;
import java.util.logging.Logger;

/**
 * Set Clock event. Sets GUI and device controllers values.
 */
public class SetClockEvent extends Event {
    private final Semaforky semaforky;
    private final Settings settings;
    private final Date setStart;
    private static final Logger LOGGER = Logger.getLogger(SetClockEvent.class.getName());

    public SetClockEvent(final Date time, final Date start, final Semaforky semaforky) {
        super(time);
        this.settings = semaforky.getSettings();
        this.setStart = start;
        this.semaforky = semaforky;
    }

    public void run() {
        Date now = new Date();
        long seconds = (now.getTime() - setStart.getTime()) / 1000;
        int remaining_seconds = getRemainingSeconds(seconds);

        semaforky.getSemaforkyEvents().updateClocks(remaining_seconds);
        semaforky.getGuiEventReceiver().updateSetClocks(remaining_seconds);
        semaforky.getScheduler().AddEvent(new SetClockEvent(new Date(now.getTime() + 100), setStart, semaforky));
    }

    /**
     * Compute remaining seconds based on total seconds from start.
     */
    private int getRemainingSeconds(final long seconds) {
        int remaining_seconds = 0;
        if (semaforky.getMachine().getCurrenState().name.equals(SemaforkyState.READY)) {
            remaining_seconds = (int) Math.max(settings.getPreparationTimeTime() - seconds, 0);
        } else if (semaforky.getMachine().getCurrenState().name.equals(SemaforkyState.FIRE) ||
                semaforky.getMachine().getCurrenState().name.equals(SemaforkyState.WARNING)) {
            remaining_seconds = (int) Math.max(settings.getPreparationTimeTime() + settings.getSetTime() - seconds, 0);
        } else if (semaforky.getMachine().getCurrenState().name.equals(SemaforkyState.MANUAL_CONTROL)) {
            remaining_seconds = (int) seconds;
            LOGGER.info("seconds = " + remaining_seconds);
        }
        return remaining_seconds;
    }
}
