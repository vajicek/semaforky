package com.vajsoft.semaforky;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import android.app.Application;
import android.os.StrictMode;

import com.vajsoft.semaforky.activities.GuiEventReceiver;
import com.vajsoft.semaforky.controllers.ControllerRegistry;
import com.vajsoft.semaforky.controllers.SemaforkyEvents;
import com.vajsoft.semaforky.controllers.SemaforkyMachine;
import com.vajsoft.semaforky.controllers.SocketServerController;
import com.vajsoft.semaforky.data.Settings;
import com.vajsoft.semaforky.scheduler.Scheduler;
import com.vajsoft.semaforky.utils.HotspotManager;
import com.vajsoft.semaforky.utils.SoundManager;

import java.io.InputStream;
import java.util.logging.LogManager;

/**
 * Application object. Allocates and holds all infrastructure objects.
 */
public class Semaforky extends Application {
    private SocketServerController socketServerController;
    private Scheduler scheduler;
    private Settings settings;
    private SemaforkyMachine machine;
    private SoundManager soundManager;
    private GuiEventReceiver guiEventReceiver;
    private HotspotManager hotspotManager;

    @Override
    public void onCreate() {
        super.onCreate();
        initLogging();
        initThreading();

        guiEventReceiver = new GuiEventReceiver();
        soundManager = new SoundManager(getApplicationContext());
        settings = new Settings(getApplicationContext());
        socketServerController = new SocketServerController(this);
        scheduler = new Scheduler(this);
        machine = new SemaforkyMachine(this);
        hotspotManager = new HotspotManager(getApplicationContext(), settings);
    }

    public HotspotManager getHotspotManager() {
        return hotspotManager;
    }

    public Settings getSettings() {
        return settings;
    }

    public SemaforkyMachine getMachine() {
        return machine;
    }

    public SemaforkyEvents getSemaforkyEvents() {
        return socketServerController;
    }

    public ControllerRegistry getControllerRegistry() {
        return socketServerController;
    }

    public GuiEventReceiver getGuiEventReceiver() {
        return guiEventReceiver;
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

    private void initThreading() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }
}
