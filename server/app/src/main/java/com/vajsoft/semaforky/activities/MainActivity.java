package com.vajsoft.semaforky.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.vajsoft.semaforky.R;
import com.vajsoft.semaforky.controllers.SemaforkyMachine;
import com.vajsoft.semaforky.data.Settings;
import com.vajsoft.semaforky.controllers.MainController;
import com.vajsoft.semaforky.scheduler.Scheduler;

import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** Main activity wrapper. Allocate and hold all infrastructure objects. Handle GUI interactions.
 * */
public class MainActivity extends AppCompatActivity {

    private MainController mainController;
    private Scheduler scheduler;
    private Settings settings;
    private SemaphoreWidget semaphoreWidget;
    private SemaforkyMachine machine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settings = Settings.getInstance();
        mainController = new MainController(this);
        scheduler = new Scheduler(mainController);
        machine = new SemaforkyMachine(mainController, scheduler);

        settings.LoadSetting(getApplicationContext());

        UpdateLocale();

        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        semaphoreWidget = new SemaphoreWidget((SurfaceView) findViewById(R.id.svSemaphore));

        UpdateGui();
    }

    public void SetSemaphore(final SemaphoreWidget.SemaphoreLight light) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                semaphoreWidget.UpdateStatus(light);
            }
        });
    }

    public void LogMessage(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                EditText editText = (EditText) findViewById(R.id.etLog);
                editText.append(message);
                editText.append("\n");
                System.out.println(message);
            }
        });
    }

    public void UpdateRoundClocks(final Date roundStart) {
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

    public void UpdateSetClocks(final int remainingSeconds) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView) findViewById(R.id.tvSetTime)).setText(
                        String.format("%1$03d", remainingSeconds)
                );
            }
        });
    }

    private void UpdateSet() {
        ((TextView) findViewById(R.id.tvSet)).setText(Integer.toString(machine.GetCurrentSet()));
        if (machine.GetCurrenState().name.equals(SemaforkyMachine.STARTED) ||
                machine.GetCurrenState().name.equals(SemaforkyMachine.ROUND_STOPPED)) {
            ((TextView) findViewById(R.id.tvLine)).setText("--");
        } else if (settings.GetLines() == 1) {
            ((TextView) findViewById(R.id.tvLine)).setText("AB");
        } else  if (settings.GetLines() == 2) {
            if (settings.GetLinesRotation() == Settings.LinesRotation.SIMPLE) {
                ((TextView) findViewById(R.id.tvLine)).setText(machine.GetCurrentLine() == 0 ? "AB" : "CD");
            } else {
                ((TextView) findViewById(R.id.tvLine)).setText(machine.GetCurrentLine() != machine.GetCurrentSet() % 2 ? "AB" : "CD");
            }
        }
    }

    public void UpdateLocale() {
        Locale locale = new Locale(settings.GetLanguageCode());
        Locale.setDefault(locale);
        Configuration config = getBaseContext().getResources().getConfiguration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }

    public void UpdateGui() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                UpdateSet();

                ((Button) findViewById(R.id.btnBeginRound)).setEnabled(
                        machine.GetCurrenState().name.equals(SemaforkyMachine.STARTED) ||
                                machine.GetCurrenState().name.equals(SemaforkyMachine.ROUND_STOPPED));
                ((Button) findViewById(R.id.btnEndRound)).setEnabled(
                        machine.GetCurrenState().name.equals(SemaforkyMachine.ROUND_STARTED) ||
                                machine.GetCurrenState().name.equals(SemaforkyMachine.SET_STOPPED) ||
                                machine.GetCurrenState().name.equals(SemaforkyMachine.SET_CANCELED));
                ((Button) findViewById(R.id.btnStartSet)).setEnabled(
                        machine.GetCurrenState().name.equals(SemaforkyMachine.ROUND_STARTED) ||
                                machine.GetCurrenState().name.equals(SemaforkyMachine.SET_CANCELED) ||
                                machine.GetCurrenState().name.equals(SemaforkyMachine.SET_STOPPED));
                ((Button) findViewById(R.id.btnStopSet)).setEnabled(
                        machine.GetCurrenState().name.equals(SemaforkyMachine.FIRE) ||
                                machine.GetCurrenState().name.equals(SemaforkyMachine.WARNING));
                ((Button) findViewById(R.id.btnCancelSet)).setEnabled(
                        machine.GetCurrenState().name.equals(SemaforkyMachine.READY) ||
                                machine.GetCurrenState().name.equals(SemaforkyMachine.FIRE) ||
                                machine.GetCurrenState().name.equals(SemaforkyMachine.WARNING));

                if (machine.GetCurrenState().name.equals(SemaforkyMachine.READY)) {
                    SetSemaphore(SemaphoreWidget.SemaphoreLight.RED);
                } else if (machine.GetCurrenState().name.equals(SemaforkyMachine.FIRE)) {
                    SetSemaphore(SemaphoreWidget.SemaphoreLight.GREEN);
                } else if (machine.GetCurrenState().name.equals(SemaforkyMachine.WARNING)) {
                    SetSemaphore(SemaphoreWidget.SemaphoreLight.YELLOW);
                } else {
                    SetSemaphore(SemaphoreWidget.SemaphoreLight.NONE);
                }
            }
        });
    }

    public void onStartSetClick(View view) {
        machine.MoveTo(SemaforkyMachine.SET_STARTED);
    }

    public void onStopSetClick(View view) {
        machine.MoveTo(SemaforkyMachine.SET_STOPPED);
    }

    public void onCancelSetClick(View view) {
        machine.MoveTo(SemaforkyMachine.SET_CANCELED);
    }

    public void onSettingsClick(View view) {
        machine.MoveTo(SemaforkyMachine.SETTINGS);
        Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
        startActivityForResult(intent, 0);
    }

    public void onBeginRoundClicked(View view) {
        machine.MoveTo(SemaforkyMachine.ROUND_STARTED);
    }

    public void onEndRoundClicked(View view) {
        machine.MoveTo(SemaforkyMachine.ROUND_STOPPED);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == SettingsActivity.SETTINGS_UPDATED) {
            finish();
            startActivity(getIntent());
        }
    }

    public SemaforkyMachine GetMachine() {
        return machine;
    }
}
