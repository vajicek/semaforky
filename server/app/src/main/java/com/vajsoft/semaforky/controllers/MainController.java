package com.vajsoft.semaforky.controllers;

import com.vajsoft.semaforky.activities.MainActivity;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

/**
 * Created by vajicek on 10/21/2016.
 */

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

    public void StartClocks() {

    }

    public void SetupSemaphores(int status) {
        mainActivity.SetSemaphore(status);
    }

    public void SetupClocks(int remainingSeconds) {
        for (int i = 0; i < controllers.size(); ++i) {
            Controller controller = controllers.get(i);
            if (controller instanceof ClockController) {
                mainActivity.LogMessage("Setting clock!");
                ((ClockController)controller).Send(remainingSeconds);
            }
        }
    }

    public void StartRound() {
    }

    public void EndRound() {
    }
}
