package com.vajsoft.semaforky.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.vajsoft.semaforky.R;
import com.vajsoft.semaforky.data.Settings;
import com.vajsoft.semaforky.controllers.MainController;
import com.vajsoft.semaforky.scheduler.Scheduler;

import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private MainController mainController;
    private Scheduler scheduler;
    private Settings settings;
    private int currentSet = 0;
    private int currentLine = -1;
    private SemaphoreWidget semaphoreWidget;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settings = Settings.getInstance();
        mainController = new MainController(this);
        scheduler = new Scheduler(mainController, this, settings);

        UpdateLocale();

        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        semaphoreWidget = new SemaphoreWidget((SurfaceView) findViewById(R.id.svSemaphore));
    }

    public void SetSemaphore(final int status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                semaphoreWidget.UpdateStatus(status);
            }
        });
    }


    public void DumpLocalIpAddress() {
        /*
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        System.out.println(inetAddress.getHostAddress().toString());
                        EditText editText = (EditText) findViewById(R.id.editText);
                        editText.append(inetAddress.getHostAddress().toString() + "\n");
                    }
                }
            }
        } catch (SocketException ex) {
            System.out.println( ex.toString());
        }
        */
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

    public void UpdateClocks(final int remainingSeconds) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView) findViewById(R.id.tvSetTime)).setText(
                        String.format("%1$03d", remainingSeconds)
                );
            }
        });
    }

    public void UpdateSet() {
        ((TextView) findViewById(R.id.tvSet)).setText(Integer.toString(currentSet));

        if (settings.GetLines() == 1) {
            ((TextView) findViewById(R.id.tvLine)).setText("AB");
        } else  if (settings.GetLines() == 2) {
            ((TextView) findViewById(R.id.tvLine)).setText(currentLine == 0 ? "AB" : "CD");
        }
    }

    public void UpdateLocale() {
        Locale locale = new Locale(settings.GetLanguageCode());
        Locale.setDefault(locale);
        Configuration config = getBaseContext().getResources().getConfiguration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }

    public void onStartSetClick(View view) {
        scheduler.StartSet();
        if ((currentLine + 1) < settings.GetLines()) {
            currentLine++;
        } else {
            currentSet++;
            currentLine = 0;
        }
        UpdateSet();
    }

    public void onStopSetClick(View view) {
        scheduler.StopSet();
    }

    public void onCancelSetClick(View view) {
        scheduler.CancelSet();
        currentSet--;
        UpdateSet();
    }

    public void onSettingsClick(View view) {
        Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
        startActivityForResult(intent, 0);
    }

    public void onBeginRoundClicked(View view) {
        scheduler.StartRound();
        mainController.StartRound();
        currentSet = 1;
        UpdateSet();
    }

    public void onEndRoundClicked(View view) {
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == SettingsActivity.SETTINGS_UPDATED) {
            finish();
            startActivity(getIntent());
        }
    }
}
