package com.vajsoft.semaforky.activities;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import com.vajsoft.semaforky.R;
import com.vajsoft.semaforky.Semaforky;
import com.vajsoft.semaforky.data.Settings;
import com.vajsoft.semaforky.utils.PopupWindow;

import java.sql.Time;
import java.util.Locale;

/**
 * Setting activity/dialog.
 */
public class SettingsActivity extends AppCompatActivity {

    public static final int SETTINGS_UPDATED = 1;
    public static final int SETTINGS_UPDATE_CANCELED = 1;
    private Settings settings;

    public void onCancelClicked(final View view) {
        setResult(SETTINGS_UPDATE_CANCELED, null);
        finish();
    }

    public void onOkClicked(final View view) {
        if (validateTime()) {
            updateDataFromGui();
            setResult(SETTINGS_UPDATED, null);
            settings.saveSetting(getApplicationContext());
            finish();
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.settings = ((Semaforky) getApplication()).getSettings();
        setContentView(R.layout.activity_settings);
        updateGuiFromData();
    }

    private void updateGuiFromData() {
        ((Spinner) findViewById(R.id.spinnerLanguage)).setSelection(settings.getLanguage());
        ((Spinner) findViewById(R.id.spinnerLineRotation)).setSelection(settings.getLinesRotation().ordinal());
        ((TextView) findViewById(R.id.editLines)).setText(String.format(Locale.ROOT, "%d", settings.getLines()));
        ((TextView) findViewById(R.id.editRounds)).setText(String.format(Locale.ROOT, "%d", settings.getRoundSets()));
        ((TextView) findViewById(R.id.editSetTime)).setText(String.format(Locale.ROOT, "%d", settings.getSetTime()));
        ((TextView) findViewById(R.id.editPreparationTime)).setText(String.format(Locale.ROOT, "%d", settings.getPreparationTimeTime()));
        ((TextView) findViewById(R.id.editWarningTime)).setText(String.format(Locale.ROOT, "%d", settings.getWarningTimeTime()));
        ((CheckBox) findViewById(R.id.cbContinuous)).setChecked(settings.getContinuous());
        ((TextView) findViewById(R.id.editNumberOfSets)).setText(String.format(Locale.ROOT, "%d", settings.getNumberOfSets()));
        ((CheckBox) findViewById(R.id.cbDelayStart)).setChecked(settings.isDelayedStartEnabled());
        ((TextView) findViewById(R.id.ebRoundStart)).setText(settings.getDelayedStartTime().toString());
    }

    private boolean validateTime() {
        if (getWarningTimeFromGui() > getSetTimeFromGui()) {
            PopupWindow.showMessageBox(this, getResources().getString(R.string.warningTimeInvalid));
            return false;
        }
        if (getLinesFromGui() > 2) {
            PopupWindow.showMessageBox(this, getResources().getString(R.string.lineCountInvalid));
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
        settings.setDelayedStartEnabled(getDelayedStartEnabledFromGui());
        settings.setDelayedStartTime(getDelayedStartTimeFromGui());
    }

    private boolean getDelayedStartEnabledFromGui() {
        return ((CheckBox) findViewById(R.id.cbDelayStart)).isChecked();
    }

    private Time getDelayedStartTimeFromGui() {
        return Time.valueOf(((TextView) findViewById(R.id.ebRoundStart)).getText().toString());
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
}
