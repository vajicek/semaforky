package com.vajsoft.semaforky.activities;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import com.vajsoft.semaforky.R;
import com.vajsoft.semaforky.Semaforky;
import com.vajsoft.semaforky.data.Settings;
import com.vajsoft.semaforky.utils.HotspotManager;

import java.util.logging.Logger;

/** Setting activity/dialog. */
public class SettingsActivity extends AppCompatActivity {

    public static final int SETTINGS_UPDATED = 1;
    public static final int SETTINGS_UPDATE_CANCELED = 1;
    private static final Logger LOGGER = Logger.getLogger(SettingsActivity.class.getName());
    private Settings settings;

    public void onCancelClicked(View view) {
        setResult(SETTINGS_UPDATE_CANCELED, null);
        finish();
    }

    public void onOkClicked(View view) {
        if (validateTime()) {
            updateDataFromGui();
            setResult(SETTINGS_UPDATED, null);
            settings.saveSetting(getApplicationContext());
            finish();
        }
    }

    public void showMessageBox(String message) {
        LOGGER.info("showMessageBox: " + message);
        getDialogBuilder().setTitle(getResources().getString(R.string.warningTitle))
                .setMessage(message)
                .show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.settings = ((Semaforky) getApplication()).getSettings();
        setContentView(R.layout.activity_settings);
        updateGuiFromData();
    }

    private void updateGuiFromData() {
        ((Spinner) findViewById(R.id.spinnerLanguage)).setSelection(settings.getLanguage());
        ((Spinner) findViewById(R.id.spinnerLineRotation)).setSelection(settings.getLinesRotation().ordinal());
        ((TextView) findViewById(R.id.editLines)).setText(Integer.toString(settings.getLines()));
        ((TextView) findViewById(R.id.editRounds)).setText(Integer.toString(settings.getRoundSets()));
        ((TextView) findViewById(R.id.editSetTime)).setText(Integer.toString(settings.getSetTime()));
        ((TextView) findViewById(R.id.editPreparationTime)).setText(Integer.toString(settings.getPreparationTimeTime()));
        ((TextView) findViewById(R.id.editWarningTime)).setText(Integer.toString(settings.getWarningTimeTime()));
        ((CheckBox) findViewById(R.id.cbContinuous)).setChecked(settings.getContinuous());
        ((TextView) findViewById(R.id.editNumberOfSets)).setText(Integer.toString(settings.getNumberOfSets()));
    }

    private boolean validateTime() {
        if (getWarningTimeFromGui() > getSetTimeFromGui()) {
            showMessageBox(getResources().getString(R.string.warningTimeInvalid));
            return false;
        }
        if (getLinesFromGui() > 2) {
            showMessageBox(getResources().getString(R.string.lineCountInvalid));
            return false;
        }
        return true;
    }

    private void updateDataFromGui() {
        settings.setLanguage(((Spinner) findViewById(R.id.spinnerLanguage)).getSelectedItemPosition());
        settings.setLinesRotation(Settings.LinesRotation.values()[((Spinner) findViewById(R.id.spinnerLineRotation)).getSelectedItemPosition()]);
        settings.setLines(getLinesFromGui());
        settings.setRoundSets(Integer.parseInt(((TextView) findViewById(R.id.editRounds)).getText().toString()));
        settings.setSetTime(getSetTimeFromGui());
        settings.setPreparationTimeTime(getPreparationTimeFromGui());
        settings.setWarningTimeTime(getWarningTimeFromGui());
        settings.setContinuous(getContinuousFromGui());
        settings.setNumberOfSets(getNumberOfSetsGui());
    }

    private int getWarningTimeFromGui() {
        return Integer.parseInt(((TextView) findViewById(R.id.editWarningTime)).getText().toString());
    }

    private int getSetTimeFromGui() {
        return Integer.parseInt(((TextView) findViewById(R.id.editSetTime)).getText().toString());
    }

    private int getNumberOfSetsGui() {
        return Integer.parseInt(((TextView) findViewById(R.id.editNumberOfSets)).getText().toString());
    }

    private boolean getContinuousFromGui() {
        return ((CheckBox) findViewById(R.id.cbContinuous)).isChecked();
    }

    private int getLinesFromGui() {
        return Integer.parseInt(((TextView) findViewById(R.id.editLines)).getText().toString());
    }

    private int getPreparationTimeFromGui() {
        return Integer.parseInt(((TextView) findViewById(R.id.editPreparationTime)).getText().toString());
    }

    private AlertDialog.Builder getDialogBuilder() {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        return builder;
    }
}
