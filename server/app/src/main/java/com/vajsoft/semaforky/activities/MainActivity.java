package com.vajsoft.semaforky.activities;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.vajsoft.semaforky.R;
import com.vajsoft.semaforky.Semaforky;
import com.vajsoft.semaforky.controllers.MainController;
import com.vajsoft.semaforky.controllers.SemaforkyMachine;
import com.vajsoft.semaforky.controllers.SemaforkyState;
import com.vajsoft.semaforky.data.Settings;
import com.vajsoft.semaforky.utils.HotspotManager;

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
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API. See
     * https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

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
        Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
        startActivityForResult(intent, 0);
    }

    public void onWifiApSwitchClick(View view) throws HotspotManager.HotspotManagerException {
        HotspotManager hotspotManager = new HotspotManager(getApplicationContext());
        if (((Switch) findViewById(R.id.switchWifiAp)).isChecked()) {
            hotspotManager.configApState(true, Settings.SEMAFORKY_ESSID, Settings.SEMAFORKY_PASSWORD);
        } else {
            hotspotManager.disableApState();
        }
    }

    public void updateGui() {
        runOnUiThread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            @Override
            public void run() {
                updateSet();

                ((Button) findViewById(R.id.btnBeginRound)).setEnabled(
                        machine.getCurrenState().name.equals(SemaforkyState.STARTED) ||
                                machine.getCurrenState().name.equals(SemaforkyState.ROUND_STOPPED));
                ((Button) findViewById(R.id.btnEndRound)).setEnabled(
                        machine.getCurrenState().name.equals(SemaforkyState.ROUND_STARTED) ||
                                machine.getCurrenState().name.equals(SemaforkyState.SET_STOPPED) ||
                                machine.getCurrenState().name.equals(SemaforkyState.SET_CANCELED));
                ((Button) findViewById(R.id.btnStartSet)).setEnabled(
                        machine.getCurrenState().name.equals(SemaforkyState.ROUND_STARTED) ||
                                machine.getCurrenState().name.equals(SemaforkyState.SET_CANCELED) ||
                                machine.getCurrenState().name.equals(SemaforkyState.SET_STOPPED));
                ((Button) findViewById(R.id.btnStopSet)).setEnabled(
                        machine.getCurrenState().name.equals(SemaforkyState.FIRE) ||
                                machine.getCurrenState().name.equals(SemaforkyState.WARNING));
                ((Button) findViewById(R.id.btnCancelSet)).setEnabled(
                        machine.getCurrenState().name.equals(SemaforkyState.READY) ||
                                machine.getCurrenState().name.equals(SemaforkyState.FIRE) ||
                                machine.getCurrenState().name.equals(SemaforkyState.WARNING));
                ((Switch) findViewById(R.id.switchWifiAp)).
                        setChecked(new HotspotManager(getApplicationContext()).isApOn(Settings.SEMAFORKY_ESSID, Settings.SEMAFORKY_PASSWORD));

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

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API. See
     * https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == SettingsActivity.SETTINGS_UPDATED) {
            finish();
            startActivity(getIntent());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOGGER.info("MainActivity.onCreate() called");

        Semaforky semaforky = (Semaforky) getApplication();
        semaforky.updateMainActivity(this);
        settings = semaforky.getSettings();
        machine = semaforky.getMachine();
        mainController = semaforky.getMainController();

        updateLocale();
        setContentView(R.layout.activity_main);
        semaphoreWidget = new SemaphoreWidget((SurfaceView) findViewById(R.id.svSemaphore));
        updateGui();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
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
