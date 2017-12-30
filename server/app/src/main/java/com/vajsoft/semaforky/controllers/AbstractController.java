package com.vajsoft.semaforky.controllers;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Logger;

public abstract class AbstractController implements Controller {
    private static final Logger LOGGER = Logger.getLogger(AbstractController.class.getName());
    private Socket socket;
    private boolean interrupted = false;

    public AbstractController(Socket socket) {
        this.socket = socket;
    }

    public void run() throws IOException {
        InputStream inputStream = socket.getInputStream();
        try {
            DataInputStream dataInputStream = new DataInputStream(inputStream);
            while (!interrupted) {
                byte[] packetData = new byte[dataInputStream.readInt()];
                dataInputStream.readFully(packetData);
            }
        } catch (IOException e) {
            LOGGER.severe(e.getMessage());
            // CLIENT DISCONNECTED - EXCEPTION SWALLOWED
        }
    }

    public void send(int value) {
        try {
            sendControlChunk(value);
        } catch (IOException e) {
            LOGGER.severe(e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendControlChunk(int value) throws IOException {
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(123);
        buf.putInt(value);
        out.write(buf.array());
    }
}
