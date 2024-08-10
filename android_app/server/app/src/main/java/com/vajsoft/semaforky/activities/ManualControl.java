package com.vajsoft.semaforky.activities;

/// Copyright (C) 2019, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.vajsoft.semaforky.R;
import com.vajsoft.semaforky.Semaforky;
import com.vajsoft.semaforky.controllers.SemaforkyEvents;
import com.vajsoft.semaforky.controllers.SemaphoreController;
import com.vajsoft.semaforky.scheduler.ClockCountdownEvent;
import com.vajsoft.semaforky.scheduler.Scheduler;
import com.vajsoft.semaforky.scheduler.SetClockEvent;

public class ManualControl extends AppCompatActivity {

    private SemaforkyEvents semaforkyEvents;
    private Scheduler scheduler;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_control);
        this.scheduler = ((Semaforky) getApplication()).getScheduler();
        this.semaforkyEvents = ((Semaforky) getApplication()).getSemaforkyEvents();
    }

    @Override
    protected void onPause() {
        super.onPause();
        resetScheduler();
    }

    public void onRedClicked(final View view) {
        semaforkyEvents.updateSemaphores(SemaphoreController.SemaphoreLight.RED);
    }

    public void onGreenClicked(final View view) {
        semaforkyEvents.updateSemaphores(SemaphoreController.SemaphoreLight.GREEN);
    }

    public void onYellowClicked(final View view) {
        semaforkyEvents.updateSemaphores(SemaphoreController.SemaphoreLight.YELLOW);
    }

    public void onNoneClicked(final View view) {
        semaforkyEvents.updateSemaphores(SemaphoreController.SemaphoreLight.NONE);
    }

    public void onThreeBeep(final View view) {
        semaforkyEvents.playSiren(3);
    }

    public void onTwoBeep(final View view) {
        semaforkyEvents.playSiren(2);
    }

    public void onOneBeep(final View view) {
        semaforkyEvents.playSiren(1);
    }

    public void onClockSet0Clicked(final View view) {
        setClock(0);
    }

    public void onClockSet5Clicked(final View view) {
        setClock(5);
    }

    public void onClockSet10Clicked(final View view) {
        setClock(10);
    }

    public void onClockSet20Clicked(final View view) {
        setClock(20);
    }

    public void onClockSet30Clicked(final View view) {
        setClock(30);
    }

    public void onClockSet120Clicked(final View view) {
        setClock(120);
    }

    public void onClockSet240Clicked(final View view) {
        setClock(240);
    }

    public void onCountdown30Clicked(final View view) {
        resetScheduler();
        scheduler.AddEvent(new ClockCountdownEvent(30, ((Semaforky) getApplication())));
    }

    private void setClock(final int value) {
        resetScheduler();
        semaforkyEvents.updateClocks(value);
    }

    private void resetScheduler() {
        scheduler.RemoveAllEventsByClass(ClockCountdownEvent.class);
        scheduler.RemoveAllEventsByClass(SetClockEvent.class);
    }
}
