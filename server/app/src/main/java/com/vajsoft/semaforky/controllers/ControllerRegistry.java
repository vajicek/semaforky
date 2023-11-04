package com.vajsoft.semaforky.controllers;

/// Copyright (C) 2023, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import java.util.ArrayList;

public interface ControllerRegistry {
    ArrayList<Controller> getControllers();
    void unregisterControllerAddedListener(final SocketServerController.ControllerAddedListener controllerAddedListener);
    void registerControllerAddedListener(final SocketServerController.ControllerAddedListener controllerAddedListener);
}
