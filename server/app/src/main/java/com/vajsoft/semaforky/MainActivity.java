package com.vajsoft.semaforky;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private MainController mainController;
    private Scheduler scheduler;
    private Settings settings;
    private int currentSet = 0;

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
                long milliseconds = diff % 1000;
                long seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60;
                long minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60;
                ((TextView) findViewById(R.id.tvRoundTime)).setText(
                        String.format("%1$02d:%2$02d:%3$03d", minutes, seconds, milliseconds)
                );
            }
        });
    }

    public void UpdateClocks(final Date setStart) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                long diff = (new Date()).getTime() - setStart.getTime();
                ((TextView) findViewById(R.id.tvSetTime)).setText(
                        String.format("%1$03d", TimeUnit.MILLISECONDS.toSeconds(diff))
                );
            }
        });
    }

    public void UpdateSet() {
        ((TextView) findViewById(R.id.tvSet)).setText(Integer.toString(currentSet));
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
        currentSet++;
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
        scheduler.EndRound();;
        mainController.EndRound();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == SettingsActivity.SETTINGS_UPDATED) {
            finish();
            startActivity(getIntent());
        }
    }
}
