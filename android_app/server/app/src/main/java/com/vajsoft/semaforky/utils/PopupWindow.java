package com.vajsoft.semaforky.utils;

/// Copyright (C) 2019, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Build;

import com.vajsoft.semaforky.R;
import com.vajsoft.semaforky.activities.SettingsActivity;

import java.util.logging.Logger;

public class PopupWindow {
    private static final Logger LOGGER = Logger.getLogger(SettingsActivity.class.getName());

    static public void showMessageBox(final Activity activity, final String message) {
        LOGGER.info("showMessageBox: " + message);
        getDialogBuilder(activity).setTitle(activity.getResources().getString(R.string.warningTitle))
                .setMessage(message)
                .show();
    }

    static private AlertDialog.Builder getDialogBuilder(final Activity activity) {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(activity, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(activity);
        }
        return builder;
    }
}
