package com.vajsoft.semaforky.activities;

/// Copyright (C) 2019, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.vajsoft.semaforky.R;
import com.vajsoft.semaforky.Semaforky;
import com.vajsoft.semaforky.scheduler.ClockCountdownEvent;
import com.vajsoft.semaforky.scheduler.SetClockEvent;

import java.util.Date;

public class ManualControl extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_control);
    }

    public void onRedClicked(View view) {
        ((Semaforky) getApplication()).getMainController().updateSemaphores(1);
    }

    public void onGreenClicked(View view) {
        ((Semaforky) getApplication()).getMainController().updateSemaphores(2);
    }

    public void onYellowClicked(View view) {
        ((Semaforky) getApplication()).getMainController().updateSemaphores(3);
    }

    public void onNoneClicked(View view) {
        ((Semaforky) getApplication()).getMainController().updateSemaphores(0);
    }

    public void onThreeBeep(View view) {
        ((Semaforky) getApplication()).getMainController().playSiren(3);
    }

    public void onTwoBeep(View view) {
        ((Semaforky) getApplication()).getMainController().playSiren(2);
    }

    public void onOneBeep(View view) {
        ((Semaforky) getApplication()).getMainController().playSiren(1);
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
        ((Semaforky) getApplication()).getScheduler().RemoveAllEventsByClass(ClockCountdownEvent.class);
        ((Semaforky) getApplication()).getScheduler().RemoveAllEventsByClass(SetClockEvent.class);
        ((Semaforky) getApplication()).getScheduler().AddEvent(new ClockCountdownEvent(30, ((Semaforky) getApplication())));
    }

    private void setClock(int value) {
        ((Semaforky) getApplication()).getScheduler().RemoveAllEventsByClass(ClockCountdownEvent.class);
        ((Semaforky) getApplication()).getScheduler().RemoveAllEventsByClass(SetClockEvent.class);
        ((Semaforky) getApplication()).getMainController().updateClocks(value);
    }
}
