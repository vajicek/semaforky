package com.vajsoft.semaforky.controllers;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import java.net.Socket;

/** Controls 3 color semaphores. */
public class SemaphoreController extends AbstractController {
    final public static int SEMAPHORE_NONE = 0;
    final public static int SEMAPHORE_RED = 1;
    final public static int SEMAPHORE_GREEN = 2;
    final public static int SEMAPHORE_YELLOW = 3;

    public SemaphoreController(Socket socket) {
        super(socket);
    }
}
