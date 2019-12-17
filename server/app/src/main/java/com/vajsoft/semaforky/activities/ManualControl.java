package com.vajsoft.semaforky.activities;

/// Copyright (C) 2019, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.vajsoft.semaforky.R;
import com.vajsoft.semaforky.Semaforky;
import com.vajsoft.semaforky.controllers.MainController;
import com.vajsoft.semaforky.controllers.SemaphoreController;
import com.vajsoft.semaforky.scheduler.ClockCountdownEvent;
import com.vajsoft.semaforky.scheduler.Scheduler;
import com.vajsoft.semaforky.scheduler.SetClockEvent;

public class ManualControl extends AppCompatActivity {

    private MainController mainController;
    private Scheduler scheduler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_control);
        this.scheduler = ((Semaforky) getApplication()).getScheduler();
        this.mainController = ((Semaforky) getApplication()).getMainController();
    }

    @Override
    protected void onPause() {
        super.onPause();
        resetScheduler();
    }

    public void onRedClicked(View view) {
        mainController.updateSemaphores(SemaphoreController.SemaphoreLight.RED);
    }

    public void onGreenClicked(View view) {
        mainController.updateSemaphores(SemaphoreController.SemaphoreLight.GREEN);
    }

    public void onYellowClicked(View view) {
        mainController.updateSemaphores(SemaphoreController.SemaphoreLight.YELLOW);
    }

    public void onNoneClicked(View view) {
        mainController.updateSemaphores(SemaphoreController.SemaphoreLight.NONE);
    }

    public void onThreeBeep(View view) {
        mainController.playSiren(3);
    }

    public void onTwoBeep(View view) {
        mainController.playSiren(2);
    }

    public void onOneBeep(View view) {
        mainController.playSiren(1);
    }

    public void onClockSet0Clicked(View view) {
        setClock(0);
    }

    public void onClockSet5Clicked(View view) {
        setClock(5);
    }

    public void onClockSet10Clicked(View view) {
        setClock(10);
    }

    public void onClockSet20Clicked(View view) {
        setClock(20);
    }

    public void onClockSet30Clicked(View view) {
        setClock(30);
    }

    public void onClockSet120Clicked(View view) {
        setClock(120);
    }

    public void onClockSet240Clicked(View view) {
        setClock(240);
    }

    public void onCountdown30Clicked(View view) {
        resetScheduler();
        scheduler.AddEvent(new ClockCountdownEvent(30, ((Semaforky) getApplication())));
    }

    private void setClock(int value) {
        resetScheduler();
        mainController.updateClocks(value);
    }

    private void resetScheduler() {
        scheduler.RemoveAllEventsByClass(ClockCountdownEvent.class);
        scheduler.RemoveAllEventsByClass(SetClockEvent.class);
    }
}
