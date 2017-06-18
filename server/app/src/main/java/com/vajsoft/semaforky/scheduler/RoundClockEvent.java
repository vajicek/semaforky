package com.vajsoft.semaforky.scheduler;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import com.vajsoft.semaforky.activities.MainActivity;

import java.util.Date;

/** Round clock event. Updates GUI clock control on main activity.
 * */
public class RoundClockEvent extends Event {
    private MainActivity mainActivity;
    private Date roundStart;
    private Scheduler scheduler;

    RoundClockEvent(Date time, MainActivity mac, Date rs, Scheduler sche) {
        super(time);
        mainActivity = mac;
        roundStart = rs;
        scheduler = sche;
    }
    public void run() {
        Date now = new Date();
        mainActivity.UpdateRoundClocks(roundStart);
        scheduler.AddEvent(new RoundClockEvent(new Date(now.getTime() + 200), mainActivity,  roundStart, scheduler));
    }
}
