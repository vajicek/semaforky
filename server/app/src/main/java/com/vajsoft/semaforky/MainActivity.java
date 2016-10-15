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
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }


    private ServerSocket serverSocket;
    private Socket server;

    public void StartServer() throws IOException {
        serverSocket = new ServerSocket(8888);
        serverSocket.setSoTimeout(10000);
        System.out.println("IP: " + serverSocket.getLocalSocketAddress().toString());
        server = serverSocket.accept();
    }

    private void SendControlChunk() throws IOException {
        DataOutputStream out = new DataOutputStream(server.getOutputStream());
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putInt(123);
        buf.putInt(321);
        out.write(buf.array());
    }

    public void DumpLocalIpAddress() {
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
    }

    public void onButton1Click(View view) {
        System.out.println("onButton1Click");
        try {
            StartServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onButton2Click(View view) {
        System.out.println("onButton2Click");
        try {
            SendControlChunk();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onButton3Click(View view) {
        DumpLocalIpAddress();
    }
}
