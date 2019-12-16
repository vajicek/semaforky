package com.vajsoft.semaforky.controllers;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import java.net.Socket;

/**
 * Clock controller. Implements sending binary data to the device.
 */
public class ClockController extends AbstractController {
    public ClockController(Socket socket) {
        super(socket);
    }
}
