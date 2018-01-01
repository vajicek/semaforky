package com.vajsoft.semaforky;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import android.app.Application;
import android.os.StrictMode;

import com.vajsoft.semaforky.activities.MainActivity;
import com.vajsoft.semaforky.controllers.MainController;
import com.vajsoft.semaforky.controllers.SemaforkyMachine;
import com.vajsoft.semaforky.data.Settings;
import com.vajsoft.semaforky.scheduler.Scheduler;
import com.vajsoft.semaforky.utils.SoundManager;

import java.util.logging.Logger;

/** Application object. Allocates and holds all infrastructure objects. */
public class Semaforky extends Application {
    private static final Logger LOGGER = Logger.getLogger(MainActivity.class.getName());
    private MainController mainController;
    private Scheduler scheduler;
    private Settings settings;
    private SemaforkyMachine machine;
    private SoundManager soundManager;
    private MainActivity mainActivity;

    public void logMessage(final String message) {
        LOGGER.info(message);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        settings = Settings.getInstance();
        settings.loadSetting(getApplicationContext());

        mainController = new MainController(this);
        scheduler = new Scheduler(this);
        machine = new SemaforkyMachine(this);
        soundManager = new SoundManager(getApplicationContext());

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

    }

    public void updateMainActivity(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public Settings getSettings() {
        return settings;
    }

    public SemaforkyMachine getMachine() {
        return machine;
    }

    public MainController getMainController() {
        return mainController;
    }

    public MainActivity getMainActivity() {
        return mainActivity;
    }

    public SoundManager getSoundManager() {
        return soundManager;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }
}
