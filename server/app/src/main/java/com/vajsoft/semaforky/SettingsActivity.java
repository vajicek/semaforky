package com.vajsoft.semaforky;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Locale;

/**
 * */
public class SettingsActivity extends AppCompatActivity {

    private Settings settings;

    private final String SETTINGS_EXTRA_ID = "settings";

    static final int SETTINGS_UPDATED = 1;

    public SettingsActivity() {
        settings = Settings.getInstance();
    }

    private void UpdateDataFromGui() {
        settings.SetLanguage(((Spinner)findViewById(R.id.spinnerLanguage)).getSelectedItemPosition());
        settings.SetLines(Integer.parseInt(((TextView)findViewById(R.id.editLines)).getText().toString()));
        settings.SetRoundSets(Integer.parseInt(((TextView)findViewById(R.id.editRounds)).getText().toString()));
        settings.SetSetTime(Integer.parseInt(((TextView)findViewById(R.id.editSetTime)).getText().toString()));
        settings.SetPreparationTimeTime(Integer.parseInt(((TextView)findViewById(R.id.editPreparationTime)).getText().toString()));
        settings.SetWarningTimeTime(Integer.parseInt(((TextView)findViewById(R.id.editWarningTime)).getText().toString()));
    }

    private void UpdateGuiFromData() {
        ((Spinner)findViewById(R.id.spinnerLanguage)).setSelection(settings.GetLanguage());
        ((TextView)findViewById(R.id.editLines)).setText(Integer.toString(settings.GetLines()));
        ((TextView)findViewById(R.id.editRounds)).setText(Integer.toString(settings.GetRoundSets()));
        ((TextView)findViewById(R.id.editSetTime)).setText(Integer.toString(settings.GetSetTime()));
        ((TextView)findViewById(R.id.editPreparationTime)).setText(Integer.toString(settings.GetPreparationTimeTime()));
        ((TextView)findViewById(R.id.editWarningTime)).setText(Integer.toString(settings.GetWarningTimeTime()));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        UpdateGuiFromData();
    }

    boolean ValidateTime() {
        return true;
    }

    public void onOkClicked(View view) {
        UpdateDataFromGui();
        setResult(SETTINGS_UPDATED, null);
        finish();
    }

    public void onCancelClicked(View view) {
        finish();
    }
}
