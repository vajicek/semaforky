package com.vajsoft.semaforky.activities;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.vajsoft.semaforky.R;

/**
 * Created by vajicek on 11.6.17.
 */

public class SemaphoreWidget implements  SurfaceHolder.Callback {

    private SurfaceView target;
    private int status = 0;

    public SemaphoreWidget(SurfaceView t) {
        target = t;
        t.getHolder().addCallback(this);
    }

    private void RedrawSemaphore() {
        SurfaceHolder holder = target.getHolder();
        Canvas c = holder.lockCanvas(null);
        c.drawColor(Color.WHITE);
        int h = c.getHeight();
        int w = c.getWidth();
        int[] colors = new int[]{Color.RED, Color.GREEN, Color.YELLOW};
        float circle_diameter = w / colors.length;
        float circle_radius = circle_diameter / 2.0f;
        for(int i = 0; i < colors.length; i++) {
            if ((i + 1) != status) {
                continue;
            }
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(colors[i]);
            c.drawCircle(circle_radius + i * circle_diameter, h / 2.0f, circle_radius, paint);
        }
        holder.unlockCanvasAndPost(c);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        RedrawSemaphore();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        RedrawSemaphore();
    }

    public void UpdateStatus(int newStatus){
        status = newStatus;
        RedrawSemaphore();
    }
}
