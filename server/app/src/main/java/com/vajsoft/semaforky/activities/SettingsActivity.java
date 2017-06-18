package com.vajsoft.semaforky.activities;

import android.app.AlertDialog;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;

import com.vajsoft.semaforky.R;
import com.vajsoft.semaforky.data.Settings;

/** Setting activity/dialog.
 * */
public class SettingsActivity extends AppCompatActivity {

    private Settings settings;

    static final int SETTINGS_UPDATED = 1;

    public SettingsActivity() {
        settings = Settings.getInstance();
    }

    private int GetSetTimeFromGui() {
        return Integer.parseInt(((TextView)findViewById(R.id.editSetTime)).getText().toString());
    }

    private int GetPreparationTimeFromGui() {
        return Integer.parseInt(((TextView)findViewById(R.id.editPreparationTime)).getText().toString());
    }

    private int GetWarningTimeFromGui() {
        return Integer.parseInt(((TextView)findViewById(R.id.editWarningTime)).getText().toString());
    }

    private int GetLinesFromGui() {
        return Integer.parseInt(((TextView) findViewById(R.id.editLines)).getText().toString());
    }

    private void UpdateDataFromGui() {
        settings.SetLanguage(((Spinner)findViewById(R.id.spinnerLanguage)).getSelectedItemPosition());
        settings.SetLinesRotation(Settings.LinesRotation.values()[((Spinner)findViewById(R.id.spinnerLineRotation)).getSelectedItemPosition()]);
        settings.SetLines(GetLinesFromGui());
        settings.SetRoundSets(Integer.parseInt(((TextView)findViewById(R.id.editRounds)).getText().toString()));
        settings.SetSetTime(GetSetTimeFromGui());
        settings.SetPreparationTimeTime(GetPreparationTimeFromGui());
        settings.SetWarningTimeTime(GetWarningTimeFromGui());
    }

    private void UpdateGuiFromData() {
        ((Spinner)findViewById(R.id.spinnerLanguage)).setSelection(settings.GetLanguage());
        ((Spinner)findViewById(R.id.spinnerLineRotation)).setSelection(settings.GetLinesRotation().ordinal());
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

    private AlertDialog.Builder GetDialogBuilder() {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        return builder;
    }

    public void MessageBox(String message) {
        GetDialogBuilder().setTitle(getResources().getString(R.string.warningTitle))
            .setMessage(message)
            .show();
    }

    boolean ValidateTime() {
        if (GetWarningTimeFromGui() > GetSetTimeFromGui()) {
            MessageBox(getResources().getString(R.string.warningTimeInvalid));
            return false;
        }
        if (GetLinesFromGui() > 2) {
            MessageBox(getResources().getString(R.string.lineCountInvalid));
            return false;
        }
        return true;
    }

    public void onOkClicked(View view) {
        if (ValidateTime()) {
            UpdateDataFromGui();
            setResult(SETTINGS_UPDATED, null);
            settings.SaveSetting(getApplicationContext());
            finish();
        }
    }

    public void onCancelClicked(View view) {
        finish();
    }
}
