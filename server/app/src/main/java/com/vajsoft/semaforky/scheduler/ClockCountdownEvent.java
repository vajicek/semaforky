package com.vajsoft.semaforky.scheduler;

/// Copyright (C) 2019, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import com.vajsoft.semaforky.Semaforky;

import java.util.Date;

public class ClockCountdownEvent extends Event {
    private int countdown;
    private Semaforky semaforky;
    private Date setStart;

    public ClockCountdownEvent(int countdown, Semaforky semaforky) {
        this(new Date(), countdown, semaforky);
    }

    private ClockCountdownEvent(Date setStart, int countdown, Semaforky semaforky) {
        super(new Date(new Date().getTime() + 100));
        this.countdown = countdown;
        this.semaforky = semaforky;
        this.setStart = setStart;
    }

    public void run() {
        Date now = new Date();
        long seconds = (now.getTime() - setStart.getTime()) / 1000;
        int remaining_seconds = Math.max(this.countdown - (int) seconds, 0);
        semaforky.getMainController().updateClocks(remaining_seconds);
        semaforky.getGuiEventReceiver().updateSetClocks(remaining_seconds);
        semaforky.getScheduler().AddEvent(new ClockCountdownEvent(this.setStart, countdown, semaforky));
    }
}
