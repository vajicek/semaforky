package com.vajsoft.semaforky.scheduler;

import com.vajsoft.semaforky.activities.MainActivity;

import java.util.Date;

/**
 * Created by vajicek on 11.6.17.
 */

public class RoundClockEvent extends Event {
    private MainActivity mainActivity;
    private Date roundStart;
    private Scheduler scheduler;

    RoundClockEvent(Date time, int status, MainActivity mac, Date rs, Scheduler sche) {
        super(time, status);
        mainActivity = mac;
        roundStart = rs;
        scheduler = sche;
    }
    public void run() {
        Date now = new Date();
        mainActivity.UpdateRoundClocks(roundStart);
        scheduler.AddEvent(new RoundClockEvent(new Date(now.getTime() + 200), 0, mainActivity,  roundStart, scheduler));
    }
}
