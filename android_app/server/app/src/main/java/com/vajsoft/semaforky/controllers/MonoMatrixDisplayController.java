package com.vajsoft.semaforky.controllers;

/// Copyright (C) 2023, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import java.net.Socket;

public class MonoMatrixDisplayController extends AbstractController {

    private int color = 0;
    private int value = 0;
    private int brightness = 10;

    public MonoMatrixDisplayController(final Socket socket) {
        super(socket);
    }

    public static int encodeLightColor(final int ordinal) {
        return (ordinal << 16) & 0x00ff0000;
    }

    public static int encodeLightBrightness(final int ordinal) {
        return (ordinal << 24) & 0xff000000;
    }

    private static int decodeLightColor(final int encodedOrdinal) {
        return (encodedOrdinal & 0x00ff0000) >> 16;
    }

    private static int decodeLightBrightness(final int encodedOrdinal) {
        return (encodedOrdinal & 0xff000000) >> 24;
    }

    public void send(final int v) {
        if (isLightBrightness(v)) {
            brightness = decodeLightBrightness(v);
        } else if (isLightColor(v)) {
            color = decodeLightColor(v);
        } else {
            value = v;
        }
        super.send(encodeValue());
    }

    private int encodeValue() {
        return value | (color << 16) | (brightness << 24);
    }

    private boolean isLightColor(final int value) {
        return (value & 0x00ff0000) != 0;
    }

    private boolean isLightBrightness(final int value) {
        return (value & 0xff000000) != 0;
    }
}
