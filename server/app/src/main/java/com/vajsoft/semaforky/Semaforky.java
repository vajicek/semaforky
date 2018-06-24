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

import java.io.InputStream;
import java.util.logging.LogManager;

/** Application object. Allocates and holds all infrastructure objects. */
public class Semaforky extends Application {
    private MainController mainController;
    private Scheduler scheduler;
    private Settings settings;
    private SemaforkyMachine machine;
    private SoundManager soundManager;
    private MainActivity mainActivity;

    public Semaforky() {
        settings = new Settings();
        mainController = new MainController(this);
        scheduler = new Scheduler(this);
        machine = new SemaforkyMachine(this, settings);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initLogging();
        soundManager = new SoundManager(getApplicationContext());
        settings.loadSetting(getApplicationContext());
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

    private void initLogging() {
        try {
            final LogManager logManager = LogManager.getLogManager();
            InputStream is = getResources().getAssets().open("logging.properties");
            logManager.readConfiguration(is);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
