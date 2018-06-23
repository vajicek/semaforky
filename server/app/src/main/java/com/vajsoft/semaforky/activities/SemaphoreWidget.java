package com.vajsoft.semaforky.activities;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/** Wrapper for SurfaceView which is responsible for plotting semaphore lights. */
public class SemaphoreWidget implements SurfaceHolder.Callback {

    public enum SemaphoreLight {
        NONE,
        RED,
        GREEN,
        YELLOW
    }
    private SurfaceView target;
    private SemaphoreLight light = SemaphoreLight.NONE;

    public SemaphoreWidget(SurfaceView target) {
        this.target = target;
        this.target.getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        redrawSemaphore();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        redrawSemaphore();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    public void updateStatus(SemaphoreLight newStatus) {
        light = newStatus;
        redrawSemaphore();
    }

    private void redrawSemaphore() {
        SurfaceHolder holder = target.getHolder();
        Canvas c = holder.lockCanvas(null);
        if (c == null) {
            return;
        }
        c.drawColor(Color.WHITE);
        int h = c.getHeight();
        int w = c.getWidth();
        int[] colors = new int[]{Color.RED, Color.GREEN, Color.YELLOW};
        float circle_diameter = w / colors.length;
        float circle_radius = circle_diameter / 2.0f;
        for (int i = 0; i < colors.length; i++) {
            if ((i + 1) != light.ordinal()) {
                continue;
            }
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(colors[i]);
            c.drawCircle(circle_radius + i * circle_diameter, h / 2.0f, circle_radius, paint);
        }
        holder.unlockCanvasAndPost(c);
    }
}
