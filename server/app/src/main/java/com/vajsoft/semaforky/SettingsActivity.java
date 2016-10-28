package com.vajsoft.semaforky;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

/**
 * Set time 120
 * Double set time 240
 * Preparation time 10
 * Warning time 30
 *
 * */
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
    }

    boolean ValidateTime() {
        return true;
    }


}
