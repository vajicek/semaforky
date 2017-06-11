package com.vajsoft.semaforky.scheduler;

import java.util.Date;

/**
 * Created by vajicek on 11.6.17.
 */

abstract class Event implements Comparable {
    public Date time;
    public int status;
    Event(Date time, int status) {
        this.time = time;
        this.status = status;
    }

    public abstract void run();

    @Override
    public int compareTo(Object o) {
        if (time.getTime() < ((Event)o).time.getTime()) return -1;
        if (time.getTime() > ((Event)o).time.getTime()) return 1;
        return 0;
    }
};

