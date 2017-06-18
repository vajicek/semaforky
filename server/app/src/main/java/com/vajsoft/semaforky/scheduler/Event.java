package com.vajsoft.semaforky.scheduler;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import java.util.Date;

/** Abstract base class for events which can be added to prioritized (by time) event queue.
 * Run method is executed when event is fired.
 * */
abstract class Event implements Comparable {
    public Date time;
    Event(Date time) {
        this.time = time;
    }

    public abstract void run();

    @Override
    public int compareTo(Object o) {
        if (time.getTime() < ((Event)o).time.getTime()) return -1;
        if (time.getTime() > ((Event)o).time.getTime()) return 1;
        return 0;
    }
};

