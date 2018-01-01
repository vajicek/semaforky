package com.vajsoft.semaforky.controllers;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import com.vajsoft.semaforky.Semaforky;
import com.vajsoft.semaforky.activities.MainActivity;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main controller which implements server, creates client listeners and macro operation for sending
 * messages to clients.
 */
public class MainController {
    public static final int SEMAPHORE_CLIENT = 1;
    public static final int CLOCK_CLIENT = 2;
    public static final int SIREN_CLIENT = 3;
    public static final int SERVER_PORT = 8888;
    private static final Logger LOGGER = Logger.getLogger(MainController.class.getName());
    private Semaforky semaforky;
    private ServerSocket serverSocket;
    private ArrayList<Controller> controllers = new ArrayList<Controller>();

    public MainController(Semaforky semaforky) {
        this.semaforky = semaforky;
        startServer();
    }

    public ArrayList<Controller> getControllers() {
        return controllers;
    }

    public void updateClocks(int remainingSeconds) {
        for (int i = 0; i < controllers.size(); ++i) {
            Controller controller = controllers.get(i);
            if (controller instanceof ClockController) {
                semaforky.logMessage("Setting clock!");
                controller.send(remainingSeconds);
            }
        }
    }

    public void updateSemaphores(int state) {
        for (int i = 0; i < controllers.size(); ++i) {
            Controller controller = controllers.get(i);
            if (controller instanceof SemaphoreController) {
                semaforky.logMessage("Setting semaphore state!");
                controller.send(state);
            }
        }
    }

    public void playSiren(int count) {
        semaforky.getSoundManager().Play("buzzer", count);

        for (int i = 0; i < controllers.size(); ++i) {
            Controller controller = controllers.get(i);
            if (controller instanceof SirenController) {
                semaforky.logMessage("Play siren!");
                controller.send(count);
            }
        }
    }

    public Semaforky getSemaforky() {
        return semaforky;
    }

    private void startServer() {
        LOGGER.entering(this.getClass().getName(), "startServer");
        new Thread(new Runnable() {
            public void run() {
                serverLoop();
            }
        }).start();
    }

    private void serverLoop() {
        LOGGER.entering(this.getClass().getName(), "serverLoop");
        try {
            serverSocket = new ServerSocket(SERVER_PORT);
            semaforky.logMessage("IP: " + serverSocket.getLocalSocketAddress().toString());
            int clientsConnected = 0;
            while (true) {
                try {
                    final Socket server = serverSocket.accept();
                    clientsConnected++;
                    LOGGER.log(Level.INFO, "Client no. {0} connected!", clientsConnected);
                    new Thread(new Runnable() {
                        public void run() {
                            startupController(server);
                        }
                    }).start();
                } catch (SocketTimeoutException e) {
                    //SWALLOW EXCEPTION
                    semaforky.logMessage("SocketTimeoutException");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private RegisterChunk readRegisterChunk(InputStream inputStream) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        dataInputStream.read(byteBuffer.array());
        int clientType = byteBuffer.getInt();
        return new RegisterChunk(clientType);
    }

    private void startupController(Socket server) {
        LOGGER.entering(this.getClass().getName(), "startupController");
        try {
            RegisterChunk registerChunk = readRegisterChunk(server.getInputStream());
            Controller controller = null;
            switch (registerChunk.getType()) {
                case SEMAPHORE_CLIENT:
                    semaforky.logMessage("Semaphore connected!");
                    controller = new SemaphoreController(server);
                    break;
                case CLOCK_CLIENT:
                    semaforky.logMessage("Clock connected!");
                    controller = new ClockController(server);
                    break;
                case SIREN_CLIENT:
                    semaforky.logMessage("Siren connected!");
                    controller = new SirenController(server);
                    break;
                default:
                    semaforky.logMessage(String.format("Unknown client connected! (clientType=%1$d)", registerChunk.getType()));
            }
            if (controller != null) {
                LOGGER.info("run()");
                controllers.add(controller);
                controller.run();
                controllers.remove(controller);
                semaforky.logMessage("Client disconnected!");
                LOGGER.log(Level.INFO, "Client disconnected!");
            } else {
                LOGGER.log(Level.SEVERE, "Unknown client!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class RegisterChunk {
        int type;

        RegisterChunk(int type) {
            this.type = type;
        }

        public int getType() {
            return type;
        }
    }
}
