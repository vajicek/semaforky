package com.vajsoft.semaforky.controllers;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Logger;

public abstract class AbstractController implements Controller {
    private static final Logger LOGGER = Logger.getLogger(AbstractController.class.getName());
    private static final int SOCKET_TIMEOUT = 5000;
    private static final int PING_TIMEOUT = 3000;
    private static final int PING_MAGIC_VALUE = 69;
    private static final int CONTROL_MAGIC_VALUE = 123;
    private final Socket socket;

    public AbstractController(Socket socket) {
        this.socket = socket;
    }

    static private int little2big(int i) {
        return (i & 0xff) << 24 | (i & 0xff00) << 8 | (i & 0xff0000) >> 8 | (i >> 24) & 0xff;
    }

    static void logSevere(String msg) {
        if (msg != null) {
            LOGGER.severe(msg);
        }
    }

    public void run() throws IOException {
        socket.setSoTimeout(SOCKET_TIMEOUT);
        InputStream inputStream = socket.getInputStream();
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        while (!socket.isClosed()) {
            try {
                Thread.sleep(PING_TIMEOUT);

                // ping
                ping();

                // pong
                if (little2big(dataInputStream.readInt()) != PING_MAGIC_VALUE) {
                    LOGGER.severe("Invalid pong received");
                    break;
                }
            } catch (SocketTimeoutException e) {
                LOGGER.severe("Client timeout");
                logSevere(e.getMessage());
                break;
            } catch (IOException e) {
                // CLIENT DISCONNECTED - EXCEPTION SWALLOWED
                LOGGER.severe("Client disconnected");
                logSevere(e.getMessage());
                break;
            } catch (Exception e) {
                LOGGER.severe("Unknown exception");
                logSevere(e.getMessage());
                break;
            }
        }
    }

    public String getAddress() {
        return socket.getLocalSocketAddress().toString();
    }

    public void send(int value) {
        sendControlChunk(CONTROL_MAGIC_VALUE, value);
    }

    private void ping() {
        sendControlChunk(PING_MAGIC_VALUE, PING_MAGIC_VALUE);
    }

    private void sendControlChunk(int control, int value) {
        try {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            ByteBuffer buf = ByteBuffer.allocate(8);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.putInt(control);
            buf.putInt(value);
            out.write(buf.array());
        } catch (IOException e) {
            logSevere(e.getMessage());
            e.printStackTrace();
        }
    }
}
