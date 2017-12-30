package com.vajsoft.semaforky;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import com.vajsoft.semaforky.activities.MainActivity;
import com.vajsoft.semaforky.controllers.MainController;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Logger;

public class ServerStabilityTest {
    private static final Logger LOGGER = Logger.getLogger(ServerStabilityTest.class.getName());

    @Test
    public void serverWorkingTest() throws Exception {
        MainController mainController = new MainController(Mockito.mock(MainActivity.class));
        sync();
        runClient(1);
        sync();
        Assert.assertEquals(1, mainController.getControllers().size());
    }

    @Test
    public void serverStabilityTest() throws Exception {
        MainController mainController = new MainController(Mockito.mock(MainActivity.class));
        sync();

        // connect first client
        ClientThread client1Thread = runClient(1);
        sync();
        Assert.assertEquals(1, mainController.getControllers().size());

        // connect second client
        ClientThread client2Thread = runClient(1);
        sync();
        Assert.assertEquals(2, mainController.getControllers().size());

        // disconnect second client (by force)
        client2Thread.close();
        sync();
        Assert.assertEquals(1, mainController.getControllers().size());
    }

    private void sync() throws InterruptedException {
        Thread.sleep(1000);
    }

    private ClientThread runClient(final int type) {
        ClientThread thread = new ClientThread(type);
        thread.start();
        return thread;
    }

    static class ClientThread extends Thread {
        private static final Logger LOGGER = Logger.getLogger(ServerStabilityTest.class.getName());
        private int type;
        private Socket socket;

        public ClientThread(int type) {
            this.type = type;
        }

        public void close() throws IOException {
            LOGGER.entering(this.getClass().getName(), "close");
            socket.close();
            LOGGER.exiting(this.getClass().getName(), "close");
        }

        public void run() {
            LOGGER.entering(this.getClass().getName(), "run");
            runClientThread(type);
            LOGGER.exiting(this.getClass().getName(), "run");
        }

        private void runClientThread(int type) {
            try {
                socket = new Socket("localhost", 8888);
                socket.setKeepAlive(true);
                sendRegisterChunk(socket.getOutputStream(), type);
                while (socket.isConnected()) {
                    readControlChunk(socket.getInputStream());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void sendRegisterChunk(OutputStream outputStream, int type) throws IOException {
            DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
            ByteBuffer byteBuffer = ByteBuffer.allocate(4);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            byteBuffer.putInt(type);
            dataOutputStream.write(byteBuffer.array());
        }

        private void readControlChunk(InputStream inputStream) throws IOException {
            DataInputStream dataInputStream = new DataInputStream(inputStream);
            ByteBuffer byteBuffer = ByteBuffer.allocate(4 + 4);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            dataInputStream.read(byteBuffer.array());
            byteBuffer.getInt();
            byteBuffer.getInt();
        }
    }
}
