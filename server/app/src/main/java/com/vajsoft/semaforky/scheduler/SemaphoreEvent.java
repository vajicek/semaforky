package com.vajsoft.semaforky.scheduler;

import com.vajsoft.semaforky.controllers.SemaforkyMachine;

import java.util.Date;


/** Semaphore event, move state machine to the next state.
 * */
class SemaphoreEvent extends Event {
    private SemaforkyMachine machine;
    private String nextStateName;
    SemaphoreEvent(Date time, String nextStateName, SemaforkyMachine machine) {
        super(time);
        this.machine = machine;
        this.nextStateName = nextStateName;
    }
    public void run() {
        machine.MoveTo(nextStateName);
    }
};
