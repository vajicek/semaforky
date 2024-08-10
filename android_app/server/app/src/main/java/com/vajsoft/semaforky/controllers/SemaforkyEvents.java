package com.vajsoft.semaforky.controllers;

/// Copyright (C) 2023, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

public interface SemaforkyEvents {
    void updateClocks(final int remainingSeconds);
    void updateSemaphores(final SemaphoreController.SemaphoreLight state);
    void playSiren(final int count);
}
