package com.vajsoft.semaforky.scheduler;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import java.util.Date;

/**
 * Abstract base class for events which can be added to prioritized (by time) event queue. Run
 * method is executed when event is fired.
 */
abstract class Event implements Comparable<Event> {
    public Date time;

    Event(final Date time) {
        this.time = time;
    }

    public abstract void run();

    @Override
    public int compareTo(final Event o) {
        return Long.compare(time.getTime(), o.time.getTime());
    }
};

