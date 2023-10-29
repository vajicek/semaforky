package com.vajsoft.semaforky.controllers;

import java.util.ArrayList;

public interface ControllerRegistry {
    ArrayList<Controller> getControllers();
    void unregisterControllerAddedListener(final SocketServerController.ControllerAddedListener controllerAddedListener);
    void registerControllerAddedListener(final SocketServerController.ControllerAddedListener controllerAddedListener);
}
