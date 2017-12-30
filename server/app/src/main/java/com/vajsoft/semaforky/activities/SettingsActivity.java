package com.vajsoft.semaforky.activities;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;

import com.vajsoft.semaforky.R;
import com.vajsoft.semaforky.data.Settings;

/**
 * Setting activity/dialog.
 */
public class SettingsActivity extends AppCompatActivity {

    static final int SETTINGS_UPDATED = 1;
    private Settings settings;

    public SettingsActivity() {
        settings = Settings.getInstance();
    }

    public void onCancelClicked(View view) {
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        updateGuiFromData();
    }

    private void updateGuiFromData() {
        ((Spinner) findViewById(R.id.spinnerLanguage)).setSelection(settings.GetLanguage());
        ((Spinner) findViewById(R.id.spinnerLineRotation)).setSelection(settings.GetLinesRotation().ordinal());
        ((TextView) findViewById(R.id.editLines)).setText(Integer.toString(settings.GetLines()));
        ((TextView) findViewById(R.id.editRounds)).setText(Integer.toString(settings.GetRoundSets()));
        ((TextView) findViewById(R.id.editSetTime)).setText(Integer.toString(settings.GetSetTime()));
        ((TextView) findViewById(R.id.editPreparationTime)).setText(Integer.toString(settings.GetPreparationTimeTime()));
        ((TextView) findViewById(R.id.editWarningTime)).setText(Integer.toString(settings.GetWarningTimeTime()));
    }

    public void onOkClicked(View view) {
        if (validateTime()) {
            updateDataFromGui();
            setResult(SETTINGS_UPDATED, null);
            settings.SaveSetting(getApplicationContext());
            finish();
        }
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
        settings.SetLanguage(((Spinner) findViewById(R.id.spinnerLanguage)).getSelectedItemPosition());
        settings.SetLinesRotation(Settings.LinesRotation.values()[((Spinner) findViewById(R.id.spinnerLineRotation)).getSelectedItemPosition()]);
        settings.SetLines(getLinesFromGui());
        settings.SetRoundSets(Integer.parseInt(((TextView) findViewById(R.id.editRounds)).getText().toString()));
        settings.SetSetTime(getSetTimeFromGui());
        settings.SetPreparationTimeTime(getPreparationTimeFromGui());
        settings.SetWarningTimeTime(getWarningTimeFromGui());
    }

    private int getWarningTimeFromGui() {
        return Integer.parseInt(((TextView) findViewById(R.id.editWarningTime)).getText().toString());
    }

    private int getSetTimeFromGui() {
        return Integer.parseInt(((TextView) findViewById(R.id.editSetTime)).getText().toString());
    }

    public void showMessageBox(String message) {
        getDialogBuilder().setTitle(getResources().getString(R.string.warningTitle))
                .setMessage(message)
                .show();
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
