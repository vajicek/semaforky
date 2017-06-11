package com.vajsoft.semaforky.scheduler;

import com.vajsoft.semaforky.controllers.MainController;

import java.util.Date;

/**
 * Created by vajicek on 11.6.17.
 */

class SemaphoreEvent extends Event {
    public static final int YELLOW = 3;
    public static final int GREEN = 2;
    public static final int RED = 1;
    private MainController mainController;
    SemaphoreEvent(Date time, int status, MainController mco) {
        super(time, status);
        mainController = mco;
    }
    public void run() {
        mainController.SetupSemaphores(status);
    }
};
