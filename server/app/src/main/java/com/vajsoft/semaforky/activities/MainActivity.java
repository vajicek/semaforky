package com.vajsoft.semaforky.activities;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import com.vajsoft.semaforky.BuildConfig;
import com.vajsoft.semaforky.R;
import com.vajsoft.semaforky.Semaforky;
import com.vajsoft.semaforky.controllers.MainController;
import com.vajsoft.semaforky.controllers.SemaforkyMachine;
import com.vajsoft.semaforky.controllers.SemaforkyState;
import com.vajsoft.semaforky.data.Settings;
import com.vajsoft.semaforky.utils.HotspotManager;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/** Main activity wrapper. Handle GUI interactions. */
public class MainActivity extends AppCompatActivity {
    private static final Logger LOGGER = Logger.getLogger(MainActivity.class.getName());
    private SemaphoreWidget semaphoreWidget;
    private Settings settings;
    private SemaforkyMachine machine;
    private MainController mainController;

    public void updateRoundClocks(final Date roundStart) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                long diff = (new Date()).getTime() - roundStart.getTime();
                long seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60;
                long minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60;
                ((TextView) findViewById(R.id.tvRoundTime)).setText(
                        String.format("%1$02d:%2$02d", minutes, seconds)
                );
            }
        });
    }

    public void updateSetClocks(final int remainingSeconds) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView) findViewById(R.id.tvSetTime)).setText(
                        String.format("%1$03d", remainingSeconds)
                );
            }
        });
    }

    public void onStartSetClick(View view) {
        machine.moveTo(SemaforkyState.SET_STARTED);
    }

    public void onStopSetClick(View view) {
        machine.moveTo(SemaforkyState.SET_STOPPED);
    }

    public void onCancelSetClick(View view) {
        machine.moveTo(SemaforkyState.SET_CANCELED);
    }

    public void onBeginRoundClicked(View view) {
        machine.moveTo(SemaforkyState.ROUND_STARTED);
    }

    public void onEndRoundClicked(View view) {
        machine.moveTo(SemaforkyState.ROUND_STOPPED);
    }

    public void onSettingsClick(View view) {
        machine.moveTo(SemaforkyState.SETTINGS);
        startActivityForResult(new Intent(getApplicationContext(), SettingsActivity.class), 0);
    }

    public void onWifiApSwitchClick(View view) throws HotspotManager.HotspotManagerException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!android.provider.Settings.System.canWrite(getApplicationContext())) {
                // if settings write is not enabled, it must be enabled manually by the user in a different activity
                // NOTE(vajicek): this appeared to be a problem since Android 6.0
                startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS));
                ((Switch) findViewById(R.id.switchWifiAp)).setChecked(false);
                return;
            }
        }
        // else wifi can be controlled directly
        HotspotManager hotspotManager = new HotspotManager(getApplicationContext());
        if (((Switch) findViewById(R.id.switchWifiAp)).isChecked()) {
            hotspotManager.configApState(true, Settings.SEMAFORKY_ESSID, Settings.SEMAFORKY_PASSWORD);
        } else {
            hotspotManager.disableApState();
        }
    }

    public void onDiagnosticClick(View view) {
        machine.moveTo(SemaforkyState.SETTINGS);
        startActivityForResult(new Intent(getApplicationContext(), DiagnosticActivity.class), 0);
    }

    public void updateGui() {
        LOGGER.entering(this.getClass().getName(), "updateGui() called");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LOGGER.entering(this.getClass().getName(), "updateGui().run() called");
                updateSet();

                Object stateName = machine.getCurrenState().name;
                ((Button) findViewById(R.id.btnBeginRound)).setEnabled(
                        Arrays.asList(SemaforkyState.STARTED, SemaforkyState.ROUND_STOPPED).contains(stateName));
                ((Button) findViewById(R.id.btnEndRound)).setEnabled(
                        Arrays.asList(SemaforkyState.ROUND_STARTED, SemaforkyState.SET_STOPPED, SemaforkyState.SET_CANCELED).contains(stateName));
                ((Button) findViewById(R.id.btnStartSet)).setEnabled(
                        Arrays.asList(SemaforkyState.ROUND_STARTED, SemaforkyState.SET_CANCELED, SemaforkyState.SET_STOPPED).contains(stateName));
                ((Button) findViewById(R.id.btnStopSet)).setEnabled(
                        Arrays.asList(SemaforkyState.FIRE, SemaforkyState.WARNING).contains(stateName));
                ((Button) findViewById(R.id.btnCancelSet)).setEnabled(
                        Arrays.asList(SemaforkyState.READY, SemaforkyState.FIRE, SemaforkyState.WARNING).contains(stateName));
                ((Button) findViewById(R.id.btnSettings)).setEnabled(
                        Arrays.asList(SemaforkyState.ROUND_STOPPED, SemaforkyState.STARTED).contains(stateName));
                ((Switch) findViewById(R.id.switchWifiAp)).setChecked(
                        new HotspotManager(getApplicationContext()).isApOn(Settings.SEMAFORKY_ESSID, Settings.SEMAFORKY_PASSWORD));

                if (machine.getCurrenState().name.equals(SemaforkyState.READY)) {
                    setSemaphore(SemaphoreWidget.SemaphoreLight.RED);
                } else if (machine.getCurrenState().name.equals(SemaforkyState.FIRE)) {
                    setSemaphore(SemaphoreWidget.SemaphoreLight.GREEN);
                } else if (machine.getCurrenState().name.equals(SemaforkyState.WARNING)) {
                    setSemaphore(SemaphoreWidget.SemaphoreLight.YELLOW);
                } else {
                    setSemaphore(SemaphoreWidget.SemaphoreLight.NONE);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        machine.moveTo(SemaforkyState.STARTED);
        if (resultCode == SettingsActivity.SETTINGS_UPDATED) {
            // reset is required because of changed language
            finish();
            startActivity(getIntent());
        } else {
            // just gui state update is required due to changed state (to started)
            updateGui();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LOGGER.info("MainActivity.onCreate() called");

        Semaforky semaforky = (Semaforky) getApplication();
        semaforky.updateMainActivity(this);
        settings = semaforky.getSettings();
        settings.loadSetting(getApplicationContext());
        machine = semaforky.getMachine();
        mainController = semaforky.getMainController();

        updateLocale();
        setContentView(R.layout.activity_main);
        semaphoreWidget = new SemaphoreWidget((SurfaceView) findViewById(R.id.svSemaphore));
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setTitle(getString(R.string.app_name_build, BuildConfig.GitHash));

        updateGui();
    }

    private void updateLocale() {
        Locale locale = new Locale(settings.getLanguageCode());
        Locale.setDefault(locale);
        Configuration config = getBaseContext().getResources().getConfiguration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }

    private void updateSet() {
        ((TextView) findViewById(R.id.tvSet)).setText(Integer.toString(machine.getCurrentSet()));
        if (machine.getCurrenState().name.equals(SemaforkyState.STARTED) ||
                machine.getCurrenState().name.equals(SemaforkyState.ROUND_STOPPED)) {
            ((TextView) findViewById(R.id.tvLine)).setText("--");
        } else if (settings.getLines() == 1) {
            ((TextView) findViewById(R.id.tvLine)).setText("AB");
        } else if (settings.getLines() == 2) {
            if (settings.getLinesRotation() == Settings.LinesRotation.SIMPLE) {
                ((TextView) findViewById(R.id.tvLine)).setText(machine.getCurrentLine() == 0 ? "AB" : "CD");
            } else {
                ((TextView) findViewById(R.id.tvLine)).setText(machine.getCurrentLine() != machine.getCurrentSet() % 2 ? "AB" : "CD");
            }
        }
    }

    private void setSemaphore(final SemaphoreWidget.SemaphoreLight light) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                semaphoreWidget.updateStatus(light);
            }
        });

        Map<SemaphoreWidget.SemaphoreLight, Integer> lightToInt = new HashMap<>();
        lightToInt.put(SemaphoreWidget.SemaphoreLight.RED, 1);
        lightToInt.put(SemaphoreWidget.SemaphoreLight.GREEN, 2);
        lightToInt.put(SemaphoreWidget.SemaphoreLight.YELLOW, 3);
        lightToInt.put(SemaphoreWidget.SemaphoreLight.NONE, 0);

        mainController.updateSemaphores(lightToInt.get(light));
    }
}
