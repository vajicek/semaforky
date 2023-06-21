package com.vajsoft.semaforky.controllers;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import java.net.Socket;

/**
 * Controls 3 color semaphores.
 */
public class SemaphoreController extends AbstractController {

    public enum SemaphoreLight {
        NONE(0),
        RED(1),
        GREEN(2),
        YELLOW(3);
        private final int value;

        SemaphoreLight(final int value) {
            this.value = value;
        }

        public int getInt() {
            return value;
        }
    }

    public SemaphoreController(final Socket socket) {
        super(socket);
    }
}
