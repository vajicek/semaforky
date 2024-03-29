package com.vajsoft.semaforky.activities;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.vajsoft.semaforky.BuildConfig;
import com.vajsoft.semaforky.R;
import com.vajsoft.semaforky.Semaforky;
import com.vajsoft.semaforky.controllers.SemaforkyEvents;
import com.vajsoft.semaforky.controllers.SemaforkyMachine;
import com.vajsoft.semaforky.controllers.SemaforkyState;
import com.vajsoft.semaforky.controllers.SemaphoreController;
import com.vajsoft.semaforky.data.Settings;
import com.vajsoft.semaforky.utils.HotspotManager;
import com.vajsoft.semaforky.utils.PopupWindow;

import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Main activity wrapper. Handle GUI interactions.
 */
public class MainActivity extends AppCompatActivity implements GuiEventReceiver.GuiEventSubscriber {
    private static final Logger LOGGER = Logger.getLogger(MainActivity.class.getName());
    private SemaphoreWidget semaphoreWidget;
    private Settings settings;
    private SemaforkyMachine machine;
    private SemaforkyEvents semaforkyEvents;
    private Menu optionsMenu;
    private HotspotManager hotspotManager;

    public void updateRoundClocks(final Date roundStart) {
        runOnUiThread(() -> {
            long diff = (new Date()).getTime() - roundStart.getTime();
            long seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60;
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60;
            ((TextView) findViewById(R.id.tvRoundTime)).setText(
                    String.format(Locale.ROOT, "%1$02d:%2$02d", minutes, seconds)
            );
        });
    }

    public void updateSetClocks(final int remainingSeconds) {
        runOnUiThread(() -> ((TextView) findViewById(R.id.tvSetTime)).setText(
                String.format(Locale.ROOT, "%1$03d", remainingSeconds)
        ));
    }

    public void updateGui() {
        LOGGER.entering(this.getClass().getName(), "updateGui() called");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LOGGER.entering(this.getClass().getName(), "updateGui().run() called");
                updateSet();

                final SemaforkyState stateName = machine.getCurrentState().name;
                findViewById(R.id.btnBeginRound).setEnabled(
                        Arrays.asList(SemaforkyState.STARTED, SemaforkyState.ROUND_STOPPED).contains(stateName));
                findViewById(R.id.btnEndRound).setEnabled(
                        Arrays.asList(SemaforkyState.START_WAITING, SemaforkyState.ROUND_STARTED, SemaforkyState.SET_STOPPED, SemaforkyState.SET_CANCELED, SemaforkyState.FIRE, SemaforkyState.WARNING).contains(stateName));
                findViewById(R.id.btnStartSet).setEnabled(
                        Arrays.asList(SemaforkyState.ROUND_STARTED, SemaforkyState.SET_CANCELED, SemaforkyState.SET_STOPPED).contains(stateName));
                findViewById(R.id.btnStopSet).setEnabled(
                        Arrays.asList(SemaforkyState.FIRE, SemaforkyState.WARNING).contains(stateName));
                findViewById(R.id.btnCancelSet).setEnabled(
                        Arrays.asList(SemaforkyState.READY, SemaforkyState.FIRE, SemaforkyState.WARNING).contains(stateName));
                findViewById(R.id.btnCustomSet).setEnabled(
                        Arrays.asList(SemaforkyState.SET_STOPPED, SemaforkyState.SET_CANCELED).contains(stateName));

                if (optionsMenu != null) {
                    optionsMenu.findItem(R.id.menuItemSettings).setEnabled(
                            Arrays.asList(SemaforkyState.ROUND_STOPPED, SemaforkyState.STARTED).contains(stateName));
                    optionsMenu.findItem(R.id.menuItemSwitchWifiAp).setChecked(hotspotManager.isApOn());
                    optionsMenu.findItem(R.id.menuItemManualControl).setEnabled(
                            Arrays.asList(SemaforkyState.ROUND_STOPPED, SemaforkyState.STARTED).contains(stateName));
                }

                if (machine.getCurrentState().name.equals(SemaforkyState.READY)) {
                    setSemaphore(SemaphoreController.SemaphoreLight.RED);
                } else if (machine.getCurrentState().name.equals(SemaforkyState.FIRE)) {
                    setSemaphore(SemaphoreController.SemaphoreLight.GREEN);
                } else if (machine.getCurrentState().name.equals(SemaforkyState.WARNING)) {
                    setSemaphore(SemaphoreController.SemaphoreLight.YELLOW);
                } else {
                    setSemaphore(SemaphoreController.SemaphoreLight.NONE);
                }
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

    public void onCustomSetClick(View view) {
        final LayoutInflater li = LayoutInflater.from(this);
        final View customSetDialog = li.inflate(R.layout.activity_custom_set, null);
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(customSetDialog);
        final EditText customSetLength = customSetDialog.findViewById(R.id.editCustomSetLength);

        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        (dialog, id) -> {
                            settings.setCustomSetTime(Integer.parseInt((customSetLength.getText().toString())));
                            machine.moveTo(SemaforkyState.CUSTOM_SET_STARTED);
                        })
                .setNegativeButton("Cancel",
                        (dialog, id) -> dialog.cancel());
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public void onBeginRoundClicked(View view) {
        if (settings.isDelayedStartEnabled()) {
            machine.moveTo(SemaforkyState.START_WAITING);
        } else {
            machine.moveTo(SemaforkyState.ROUND_STARTED);
        }
    }

    public void onEndRoundClicked(View view) {
        machine.moveTo(SemaforkyState.ROUND_STOPPED);
    }

    public void onSettingsClick(View view) {
        machine.moveTo(SemaforkyState.SETTINGS);
        startActivityForResult(new Intent(getApplicationContext(), SettingsActivity.class), 0);
    }

    public void onManualControlClick(View view) {
        machine.moveTo(SemaforkyState.MANUAL_CONTROL);
        startActivityForResult(new Intent(getApplicationContext(), ManualControl.class), 0);
    }

    public void onWifiApSwitchClick(View view) {
        updateWifiApState();
    }

    public void onDiagnosticClick(View view) {
        machine.moveTo(SemaforkyState.SETTINGS);
        startActivityForResult(new Intent(getApplicationContext(), DiagnosticActivity.class), 0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuItemSettings) {
            onSettingsClick(null);
        } else if (item.getItemId() == R.id.menuItemSwitchWifiAp) {
            item.setChecked(!item.isChecked());
            onWifiApSwitchClick(null);
        } else if (item.getItemId() == R.id.menuItemDiagnostic) {
            onDiagnosticClick(null);
        } else if (item.getItemId() == R.id.menuItemManualControl) {
            onManualControlClick(null);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        optionsMenu = menu;
        updateGui();
        return true;
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
        settings = semaforky.getSettings();
        machine = semaforky.getMachine();
        semaforkyEvents = semaforky.getSemaforkyEvents();
        hotspotManager = semaforky.getHotspotManager();
        semaforky.getGuiEventReceiver().subscribe(this);

        initGui();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ((Semaforky) getApplication()).getGuiEventReceiver().unsubscribe(this);
    }

    private void initGui() {
        updateLocale();
        setContentView(R.layout.activity_main);
        semaphoreWidget = new SemaphoreWidget((SurfaceView) findViewById(R.id.svSemaphore));
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setTitle(getString(R.string.app_name_build, BuildConfig.GitHash));
        updateGui();
    }

    private void updateContextLocale(final Context context, final Locale locale) {
        Configuration config = context.getResources().getConfiguration();
        config.locale = locale;
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
    }

    private void updateLocale() {
        Locale locale = new Locale(settings.getLanguageCode());
        Locale.setDefault(locale);
        updateContextLocale(getBaseContext(), locale);
        updateContextLocale(getApplication(), locale);
    }

    private void updateSet() {
        ((TextView) findViewById(R.id.tvSet)).setText(String.format(Locale.ROOT, "%d", machine.getCurrentSet()));
        if (machine.getCurrentState().name.equals(SemaforkyState.STARTED) ||
                machine.getCurrentState().name.equals(SemaforkyState.ROUND_STOPPED)) {
            ((TextView) findViewById(R.id.tvLine)).setText("--");
        } else if (settings.getLines() == 1) {
            ((TextView) findViewById(R.id.tvLine)).setText(getResources().getText(R.string.linesAB));
        } else if (settings.getLines() == 2) {
            if (settings.getLinesRotation() == Settings.LinesRotation.SIMPLE) {
                ((TextView) findViewById(R.id.tvLine)).setText(machine.getCurrentLine() == 0 ?
                        getResources().getText(R.string.linesAB) :
                        getResources().getText(R.string.linesCD));
            } else {
                ((TextView) findViewById(R.id.tvLine)).setText(machine.getCurrentLine() != machine.getCurrentSet() % 2 ?
                        getResources().getText(R.string.linesAB) :
                        getResources().getText(R.string.linesCD));
            }
        }
    }

    private void wifiApStateSetFailed(final String detailMessage) {
        LOGGER.severe("Failed to switch WifiAp");
        final MainActivity self = this;
        runOnUiThread(() -> {
            PopupWindow.showMessageBox(self, String.format(self.getResources().getString(R.string.wifiHotspotControlFailed), settings.getEssid(), settings.getPassword(), detailMessage));
            MenuItem switchWifiAp = optionsMenu.findItem(R.id.menuItemSwitchWifiAp);
            switchWifiAp.setChecked(!switchWifiAp.isChecked());
        });
    }

    private void updateWifiApState() {
        MenuItem switchWifiAp = optionsMenu.findItem(R.id.menuItemSwitchWifiAp);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!android.provider.Settings.System.canWrite(getApplicationContext())) {
                // if settings write is not enabled, it must be enabled manually by the user in a different activity
                // NOTE(vajicek): this appeared to be a problem since Android 6.0
                startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS));
                switchWifiAp.setChecked(false);
                return;
            }
        }
        try {
            hotspotManager.setWifiState(switchWifiAp.isChecked(), new HotspotManager.OnHotspotControlCallbacks() {
                public void failed(String message) {
                    wifiApStateSetFailed(message);
                }

                public void started() {
                }
            });
        } catch (HotspotManager.HotspotManagerException e) {
            wifiApStateSetFailed(Log.getStackTraceString(e));
        }
    }

    private void setSemaphore(final SemaphoreController.SemaphoreLight light) {
        runOnUiThread(() -> semaphoreWidget.updateStatus(light));
        semaforkyEvents.updateSemaphores(light);
    }
}
