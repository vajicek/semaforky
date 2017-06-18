package com.vajsoft.semaforky.controllers;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import com.vajsoft.semaforky.activities.MainActivity;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

/** Main controller which implements server, creates client listeners and macro operation for
 *  sending messages to clients.
 * */
public class MainController {

    private ServerSocket serverSocket;

    public static final int SEMAPHORE_CLIENT = 1;
    public static final int CLOCK_CLIENT = 2;

    private MainActivity mainActivity;

    public MainController(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        StartServer();
    }

    private ArrayList<Controller> controllers = new ArrayList<Controller>();

    private void StartupController(Socket server) {
        try {
            DataInputStream in = new DataInputStream(server.getInputStream());
            ByteBuffer buf = ByteBuffer.allocate(4);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            in.read(buf.array());
            int clientType = buf.getInt();
            Controller controller = null;
            switch (clientType) {
                case SEMAPHORE_CLIENT:
                    mainActivity.LogMessage("Semaphore connected!");
                    controller = new SemaphoreController(server);
                    break;
                case CLOCK_CLIENT:
                    mainActivity.LogMessage("Clock connected!");
                    controller = new ClockController(server);
                    break;
                default:
                    mainActivity.LogMessage(String.format("Unknown client connected! (clientType=%1$d)", clientType));
            }
            if (controller != null) {
                controllers.add(controller);
                controller.run();
                controllers.remove(controller);
                mainActivity.LogMessage("Client disconnected!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void ServerLoop() {
        try {
            serverSocket = new ServerSocket(8888);
            serverSocket.setSoTimeout(10000);
            mainActivity.LogMessage("IP: " + serverSocket.getLocalSocketAddress().toString());
            while (true) {
                try {
                    final Socket server = serverSocket.accept();
                    mainActivity.LogMessage("Client connected!");
                    new Thread(new Runnable() {
                        public void run() {
                            StartupController(server);
                        }
                    }).start();
                } catch (SocketTimeoutException e) {
                    //SWALLOW EXCEPTION
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void StartServer() {
        new Thread(new Runnable() {
            public void run() {
                ServerLoop();
            }
        }).start();
    }

    public void UpdateClocks(int remainingSeconds) {
        for (int i = 0; i < controllers.size(); ++i) {
            Controller controller = controllers.get(i);
            if (controller instanceof ClockController) {
                mainActivity.LogMessage("Setting clock!");
                ((ClockController)controller).Send(remainingSeconds);
            }
        }
    }

    public MainActivity GetMainActivity() {
        return mainActivity;
    }

    /*
if(wifiManager.isWifiEnabled())
{
    wifiManager.setWifiEnabled(false);
}

WifiConfiguration netConfig = new WifiConfiguration();

netConfig.SSID = "MyAP";
netConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
netConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
netConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
netConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

try{
    Method setWifiApMethod = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
    boolean apstatus=(Boolean) setWifiApMethod.invoke(wifiManager, netConfig,true);

    Method isWifiApEnabledmethod = wifiManager.getClass().getMethod("isWifiApEnabled");
    while(!(Boolean)isWifiApEnabledmethod.invoke(wifiManager)){};
    Method getWifiApStateMethod = wifiManager.getClass().getMethod("getWifiApState");
    int apstate=(Integer)getWifiApStateMethod.invoke(wifiManager);
    Method getWifiApConfigurationMethod = wifiManager.getClass().getMethod("getWifiApConfiguration");
    netConfig=(WifiConfiguration)getWifiApConfigurationMethod.invoke(wifiManager);
    Log.e("CLIENT", "\nSSID:"+netConfig.SSID+"\nPassword:"+netConfig.preSharedKey+"\n");

} catch (Exception e) {
    Log.e(this.getClass().toString(), "", e);
}
     */

}
