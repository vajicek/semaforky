package com.vajsoft.semaforky.scheduler;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import com.vajsoft.semaforky.controllers.SemaforkyMachine;
import com.vajsoft.semaforky.controllers.SemaforkyState;

import java.util.Date;

/**
 * Semaphore event, move state machine to the next state.
 */
class SemaphoreEvent extends Event {
    private final SemaforkyMachine machine;
    private final SemaforkyState nextStateName;

    SemaphoreEvent(final Date time, final SemaforkyState nextStateName, final SemaforkyMachine machine) {
        super(time);
        this.machine = machine;
        this.nextStateName = nextStateName;
    }

    public void run() {
        machine.moveTo(nextStateName);
    }
};
