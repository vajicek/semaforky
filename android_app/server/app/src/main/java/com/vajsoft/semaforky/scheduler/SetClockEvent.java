package com.vajsoft.semaforky.scheduler;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import com.vajsoft.semaforky.Semaforky;
import com.vajsoft.semaforky.controllers.SemaforkyState;

import java.util.Date;
import java.util.logging.Logger;

/**
 * Set Clock event. Sets GUI and device controllers values.
 */
public class SetClockEvent extends Event {
    private final Semaforky semaforky;
    private final Date setStart;
    private final SetTiming setTiming;
    private static final Logger LOGGER = Logger.getLogger(SetClockEvent.class.getName());

    public static class SetTiming {
        private final int preparationTimeTime;
        private final int setTime;

        public SetTiming(int preparationTimeTime, int setTime) {
            this.preparationTimeTime = preparationTimeTime;
            this.setTime = setTime;
        }

        public int getPreparationTimeTime() {
            return preparationTimeTime;
        }

        public int getSetTime() {
            return setTime;
        }
    }

    public SetClockEvent(final Date time, final Date start, final Semaforky semaforky, final SetTiming setTiming) {
        super(time);
        this.setTiming = setTiming;
        this.setStart = start;
        this.semaforky = semaforky;
    }

    public SetClockEvent(final Date time, final Date start, final Semaforky semaforky) {
        this(time, start, semaforky, new SetTiming(semaforky.getSettings().getPreparationTimeTime(),
                semaforky.getSettings().getSetTime()));
    }

    public void run() {
        Date now = new Date();
        long seconds = (now.getTime() - setStart.getTime()) / 1000;
        int remaining_seconds = getRemainingSeconds(seconds);

        this.semaforky.getSemaforkyEvents().updateClocks(remaining_seconds);
        this.semaforky.getGuiEventReceiver().updateSetClocks(remaining_seconds);

        // plan the event again
        this.semaforky.getScheduler().AddEvent(new SetClockEvent(new Date(now.getTime() + 100), setStart, this.semaforky, setTiming));
    }

    /**
     * Compute remaining seconds based on total seconds from start.
     */
    private int getRemainingSeconds(final long seconds) {
        // time to show based on top-level machine state
        int remaining_seconds = 0;
        final SemaforkyState currentStateName = this.semaforky.getMachine().getCurrentState().name;
        if (currentStateName.equals(SemaforkyState.START_WAITING)) {
            long sec = (this.semaforky.getSettings().getDelayedStartDate().getTime() - new Date().getTime()) / 1000;
            remaining_seconds = (int) Math.max(Math.min(sec, 999), 0);
        } else if (currentStateName.equals(SemaforkyState.READY)) {
            remaining_seconds = (int) Math.max(this.setTiming.getPreparationTimeTime() - seconds, 0);
        } else if (currentStateName.equals(SemaforkyState.FIRE) ||
                currentStateName.equals(SemaforkyState.WARNING)) {
            remaining_seconds = (int) Math.max(this.setTiming.getPreparationTimeTime() + this.setTiming.getSetTime() - seconds, 0);
        } else if (currentStateName.equals(SemaforkyState.MANUAL_CONTROL)) {
            remaining_seconds = (int) seconds;
            LOGGER.info("seconds = " + remaining_seconds);
        }
        return remaining_seconds;
    }
}
