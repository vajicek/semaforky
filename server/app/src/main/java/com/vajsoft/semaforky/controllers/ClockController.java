package com.vajsoft.semaforky.controllers;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Clock controller. Implements sending binary data to the device.
 * */
public class ClockController implements Controller {
    private Socket socket;
    private boolean interrupted = false;
    public ClockController(Socket socket){
        this.socket = socket;
    }

    public void run() {
        DataInputStream in = null;
        try {
            in = new DataInputStream(socket.getInputStream());
            while (!interrupted) {
                byte[] packetData = new byte[in.readInt()];
                in.readFully(packetData);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void Send(int seconds) {
        try {
            SendControlChunk(seconds);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void SendControlChunk(int seconds) throws IOException {
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(123);
        buf.putInt(seconds);
        out.write(buf.array());
    }
}
