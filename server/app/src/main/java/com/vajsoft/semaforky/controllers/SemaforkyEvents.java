package com.vajsoft.semaforky.controllers;

public interface SemaforkyEvents {
    void updateClocks(final int remainingSeconds);
    void updateSemaphores(final SemaphoreController.SemaphoreLight state);
    void playSiren(final int count);
}
