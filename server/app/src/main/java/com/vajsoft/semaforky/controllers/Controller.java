package com.vajsoft.semaforky.controllers;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import java.io.IOException;

/** Controller base. */
public interface Controller {
    void run() throws IOException;

    String getAddress();

    void send(int value);
}
