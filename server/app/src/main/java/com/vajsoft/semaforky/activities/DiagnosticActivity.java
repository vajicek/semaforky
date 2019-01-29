package com.vajsoft.semaforky.activities;

/// Copyright (C) 2019, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.vajsoft.semaforky.R;
import com.vajsoft.semaforky.Semaforky;
import com.vajsoft.semaforky.controllers.ClockController;
import com.vajsoft.semaforky.controllers.Controller;
import com.vajsoft.semaforky.controllers.MainController;
import com.vajsoft.semaforky.controllers.SemaphoreController;
import com.vajsoft.semaforky.controllers.SirenController;

/** Diagnostic activity to show list of connected components and their reported state. */
public class DiagnosticActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diagnostic);
        report();
    }

    private void report() {
        MainController mainController = ((Semaforky) getApplication()).getMainController();
        ((TextView) findViewById(R.id.textDevicesReport)).append("-----------------------\n");
        for(Controller controller : mainController.getControllers()) {
            if(controller instanceof ClockController) {
                ((TextView) findViewById(R.id.textDevicesReport)).append("ClockController");
            } else if(controller instanceof SemaphoreController) {
                ((TextView) findViewById(R.id.textDevicesReport)).append("SemaphoreController");
            } else if(controller instanceof SirenController) {
                ((TextView) findViewById(R.id.textDevicesReport)).append("SirenController");
            }
            ((TextView) findViewById(R.id.textDevicesReport)).append(" - " + controller.getAddress() + "\n");
        }
    }
}
