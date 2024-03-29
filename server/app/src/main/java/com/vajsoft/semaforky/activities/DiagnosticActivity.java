package com.vajsoft.semaforky.activities;

/// Copyright (C) 2019, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.vajsoft.semaforky.R;
import com.vajsoft.semaforky.Semaforky;
import com.vajsoft.semaforky.controllers.ClockController;
import com.vajsoft.semaforky.controllers.Controller;
import com.vajsoft.semaforky.controllers.ControllerRegistry;
import com.vajsoft.semaforky.controllers.MonoMatrixDisplayController;
import com.vajsoft.semaforky.controllers.RgbMatrixDisplayController;
import com.vajsoft.semaforky.controllers.SemaphoreController;
import com.vajsoft.semaforky.controllers.SirenController;
import com.vajsoft.semaforky.controllers.SocketServerController;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Diagnostic activity to show list of connected components and their reported state.
 */
public class DiagnosticActivity extends AppCompatActivity implements SocketServerController.ControllerAddedListener {

    SimpleDateFormat formatter;

    public DiagnosticActivity() {
        this.formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ROOT);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diagnostic);
        report();
        ((Semaforky) getApplication()).getControllerRegistry().registerControllerAddedListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ((Semaforky) getApplication()).getControllerRegistry().unregisterControllerAddedListener(this);
    }

    public void onControllerAdded() {
        this.runOnUiThread(new Runnable() {
            public void run() {
                report();
            }
        });
    }

    private void appendLine(final String line) {
        ((TextView) findViewById(R.id.textDevicesReport)).append(formatter.format(new Date()));
        ((TextView) findViewById(R.id.textDevicesReport)).append(": " + line + "\n");
    }

    private void report() {
        ControllerRegistry controllerRegistry = ((Semaforky) getApplication()).getControllerRegistry();
        appendLine("-----------------------");
        for (Controller controller : controllerRegistry.getControllers()) {
            String line = "";
            if (controller instanceof ClockController) {
                line = "ClockController";
            } else if (controller instanceof RgbMatrixDisplayController) {
                line = "RgbMatrixDisplayController";
            } else if (controller instanceof MonoMatrixDisplayController) {
                line = "MonoMatrixDisplayController";
            } else if (controller instanceof SemaphoreController) {
                line = "SemaphoreController";
            } else if (controller instanceof SirenController) {
                line = "SirenController";
            }
            appendLine(line + " - " + controller.getAddress());
        }
    }
}
