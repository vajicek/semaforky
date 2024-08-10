package com.vajsoft.semaforky.controllers;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import com.vajsoft.semaforky.Semaforky;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main controller which implements server, creates client listeners and macro operation for sending
 * messages to clients.
 */
public class SocketServerController implements ControllerRegistry, SemaforkyEvents {
    private static final int SEMAPHORE_CLIENT = 1;
    private static final int CLOCK_CLIENT = 2;
    private static final int SIREN_CLIENT = 3;
    private static final int RGB_MATRIX_DISPLAY_CLIENT = 4;
    private static final int MONO_MATRIX_DISPLAY_CLIENT = 5;
    private static final int SERVER_PORT = 8888;
    private static final Logger LOGGER = Logger.getLogger(SocketServerController.class.getName());

    private SemaphoreController.SemaphoreLight lastState = SemaphoreController.SemaphoreLight.RED;
    private int lastRemainingSeconds = 0;

    private final Semaforky semaforky;
    private final ArrayList<Controller> controllers = new ArrayList<Controller>();
    private final ArrayList<ControllerAddedListener> controllerAddedListenersList = new ArrayList<ControllerAddedListener>();

    public interface ControllerAddedListener {
        void onControllerAdded();
    }

    public SocketServerController(final Semaforky semaforky) {
        this.semaforky = semaforky;
        startServer();
    }

    @Override
    public ArrayList<Controller> getControllers() {
        return controllers;
    }

    @Override
    public void unregisterControllerAddedListener(final ControllerAddedListener controllerAddedListener) {
        controllerAddedListenersList.remove(controllerAddedListener);
    }

    @Override
    public void registerControllerAddedListener(final ControllerAddedListener controllerAddedListener) {
        controllerAddedListenersList.add(controllerAddedListener);
    }

    @Override
    public void updateClocks(final int remainingSeconds) {
        lastRemainingSeconds = remainingSeconds;

        for (int i = 0; i < controllers.size(); ++i) {
            Controller controller = controllers.get(i);
            if (controller instanceof ClockController ||
                    controller instanceof RgbMatrixDisplayController ||
                    controller instanceof MonoMatrixDisplayController) {
                LOGGER.fine("Setting clock!");
                controller.send(remainingSeconds);
            }
        }
    }

    @Override
    public void updateSemaphores(final SemaphoreController.SemaphoreLight state) {
        lastState = state;

        for (int i = 0; i < controllers.size(); ++i) {
            Controller controller = controllers.get(i);
            if (controller instanceof SemaphoreController) {
                LOGGER.fine("Setting semaphore state!");
                controller.send(state.ordinal());
            }
            if (controller instanceof RgbMatrixDisplayController) {
                LOGGER.fine("Setting semaphore state of rgb matrix display!");
                controller.send(RgbMatrixDisplayController.encodeLightColor(state.ordinal()));
            }
            if (controller instanceof MonoMatrixDisplayController) {
                LOGGER.fine("Setting semaphore state of mono matrix display!");
                controller.send(MonoMatrixDisplayController.encodeLightColor(state.ordinal()));
            }
        }
    }

    @Override
    public void playSiren(final int count) {
        LOGGER.info("play siren! count: " + count);
        semaforky.getSoundManager().play("buzzer", count);

        for (int i = 0; i < controllers.size(); ++i) {
            Controller controller = controllers.get(i);
            if (controller instanceof SirenController) {
                LOGGER.info("play siren controller!");
                controller.send(count);
            }
        }
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
            final ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            LOGGER.log(Level.CONFIG, "IP: {0}", serverSocket.getLocalSocketAddress().toString());
            int clientsConnected = 0;
            while (true) {
                try {
                    final Socket clientSocket = serverSocket.accept();
                    clientsConnected++;
                    LOGGER.log(Level.INFO, "Client no. {0} connected!", clientsConnected);
                    new Thread(new Runnable() {
                        public void run() {
                            startupController(clientSocket);
                        }
                    }).start();
                } catch (SocketTimeoutException e) {
                    //SWALLOW EXCEPTION
                    LOGGER.severe("SocketTimeoutException");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private RegisterChunk readRegisterChunk(final InputStream inputStream) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        dataInputStream.read(byteBuffer.array());
        int clientType = byteBuffer.getInt();
        return new RegisterChunk(clientType);
    }

    private void startupController(final Socket serverSocket) {
        LOGGER.entering(this.getClass().getName(), "startupController");
        try {
            RegisterChunk registerChunk = readRegisterChunk(serverSocket.getInputStream());
            Controller controller = null;
            switch (registerChunk.getType()) {
                case SEMAPHORE_CLIENT:
                    LOGGER.info("Semaphore connected!");
                    controller = new SemaphoreController(serverSocket);
                    break;
                case CLOCK_CLIENT:
                    LOGGER.info("Clock connected!");
                    controller = new ClockController(serverSocket);
                    break;
                case SIREN_CLIENT:
                    LOGGER.info("Siren connected!");
                    controller = new SirenController(serverSocket);
                    break;
                case RGB_MATRIX_DISPLAY_CLIENT:
                    LOGGER.info("RGB matrix display connected!");
                    controller = new RgbMatrixDisplayController(serverSocket);
                    break;
                case MONO_MATRIX_DISPLAY_CLIENT:
                    LOGGER.info("Mono matrix display connected!");
                    controller = new MonoMatrixDisplayController(serverSocket);
                    break;
                default:
                    LOGGER.info(String.format(Locale.ROOT, "Unknown client connected! (clientType=%1$d)", registerChunk.getType()));
            }
            if (controller != null) {
                LOGGER.info("run()");
                addController(controller);

                // Initialize state of newly connected controller
                updateSemaphores(lastState);
                updateClocks(lastRemainingSeconds);

                controller.run();
                controllers.remove(controller);
                LOGGER.log(Level.INFO, "Client {0} disconnected!", controller.getClass().getSimpleName());
            } else {
                LOGGER.severe("Unknown client!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addController(final Controller controller) {
        controllers.add(controller);
        for (ControllerAddedListener listener : controllerAddedListenersList) {
            listener.onControllerAdded();
        }
    }

    private static class RegisterChunk {
        int type;

        RegisterChunk(final int type) {
            this.type = type;
        }

        public int getType() {
            return type;
        }
    }
}
