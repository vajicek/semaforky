package com.vajsoft.semaforky;

import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

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
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private MainController mainController;
    private Scheduler scheduler;
    private Settings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        settings = new Settings();
        mainController = new MainController(this);
        scheduler = new Scheduler(mainController, this, settings);
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

    public void UpdateClocks(final Date setStart) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Date now = new Date();
                EditText editText = (EditText) findViewById(R.id.tvSetTime);
                long diff = now.getTime() - setStart.getTime();//as given
                long milliseconds = diff % 1000;
                long seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60;
                long minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60;
                editText.setText(String.format("%1$02d:%2$02d:%3$03d", minutes, seconds, milliseconds));
            }
        });
    }

    public void onStartSetClick(View view) {
        scheduler.StartSet();
    }

    public void onStopSetClick(View view) {
        scheduler.StopSet();
    }

    public void onCancelSetClick(View view) {
        scheduler.CancelSet();
    }
}
