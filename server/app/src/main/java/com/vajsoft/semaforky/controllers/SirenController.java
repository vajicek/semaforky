package com.vajsoft.semaforky.controllers;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import java.net.Socket;

/** Siren device controller. */
public class SirenController extends AbstractController {
    public SirenController(Socket socket) {
        super(socket);
    }
}
