package com.vajsoft.semaforky.scheduler;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import com.vajsoft.semaforky.Semaforky;

import java.util.Date;

/** Round clock event. Updates GUI clock control on main activity. */
public class RoundClockEvent extends Event {
    private Date roundStart;
    private Semaforky semaforky;

    RoundClockEvent(Date time, Date roundStart, Semaforky semaforky) {
        super(time);
        this.roundStart = roundStart;
        this.semaforky = semaforky;
    }

    public void run() {
        Date now = new Date();
        semaforky.getGuiEventReceiver().updateRoundClocks(roundStart);
        semaforky.getScheduler().AddEvent(new RoundClockEvent(new Date(now.getTime() + 200), roundStart, semaforky));
    }
}
