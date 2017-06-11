package com.vajsoft.semaforky.controllers;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by vajicek on 10/21/2016.
 */

public class SemaphoreController implements Controller {
    private Socket socket;
    private boolean interrupted = false;

    public SemaphoreController(Socket socket) {
        this.socket = socket;
    }

    public void run() throws IOException {
        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            while (!interrupted) {
                byte[] packetData = new byte[in.readInt()];
                in.readFully(packetData);
            }
        } catch (java.io.EOFException e) {
            // CLIENT DISCONNECTED - EXCEPTION SWALLOWED
        }
    }

    public void Send(int status) {
        try {
            SendControlChunk(status);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void SendControlChunk(int status) throws IOException {
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(123);
        buf.putInt(status);
        out.write(buf.array());
    }
}
