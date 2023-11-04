package com.vajsoft.semaforky.data;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.Serializable;
import java.sql.Time;
import java.util.Date;

/**
 * Singleton class. Holds, store, load setting of the application.
 */
public class Settings implements Serializable {

    public enum LinesRotation {
        SIMPLE,
        ALTERNATING
    }

    private static final String SEMAFORKY_ESSID = "semaforky";
    private static final String SEMAFORKY_PASSWORD = "semaforky";

    public static String PREFS_NAME = "semaforkySettings";
    private int language = 0;
    private int roundSets = 10;
    private int setTime = 120;
    private int customSetTime = 120;
    private int preparationTime = 10;
    private int warningTime = 30;
    private int lines = 1;
    private boolean continuous = false;
    private int numberOfSets = 10;
    private LinesRotation linesRotation = LinesRotation.SIMPLE;
    private boolean delayedStartEnabled = false;
    private Time delayedStartTime = new Time(12, 0, 0);

    public Settings(final Context applicationContext) {
        loadSetting(applicationContext);
    }

    public boolean isDelayedStartEnabled() {
        return delayedStartEnabled;
    }

    public void setDelayedStartEnabled(boolean delayedStartEnabled) {
        this.delayedStartEnabled = delayedStartEnabled;
    }

    public Date getDelayedStartDate() {
        final Date now = new Date();
        return new Date(now.getYear(), now.getMonth(), now.getDate(),
                delayedStartTime.getHours(), delayedStartTime.getMinutes(), delayedStartTime.getSeconds());
    }

    public Time getDelayedStartTime() {
        return delayedStartTime;
    }

    public void setDelayedStartTime(Time delayedStartTime) {
        this.delayedStartTime = delayedStartTime;
    }

    public int getCustomSetTime() {
        return customSetTime;
    }

    public void setCustomSetTime(int customSetTime) {
        this.customSetTime = customSetTime;
    }

    public int getLines() {
        return lines;
    }

    public void setLines(int lines) {
        this.lines = lines;
    }

    public int getLanguage() {
        return language;
    }

    public void setLanguage(final int selectedLanguageNo) {
        language = selectedLanguageNo;
    }

    public String getLanguageCode() {
        switch (language) {
            case 1:
                return "cs";
            case 0:
            default:
                return "";
        }
    }

    public LinesRotation getLinesRotation() {
        return linesRotation;
    }

    public void setLinesRotation(final LinesRotation linesRotation) {
        this.linesRotation = linesRotation;
    }

    public int getRoundSets() {
        return roundSets;
    }

    public void setRoundSets(int roundSets) {
        this.roundSets = roundSets;
    }

    public int getSetTime() {
        return setTime;
    }

    public void setSetTime(final int setTime) {
        this.setTime = setTime;
    }

    public int getPreparationTimeTime() {
        return preparationTime;
    }

    public void setPreparationTimeTime(final int preparationTime) {
        this.preparationTime = preparationTime;
    }

    public int getWarningTimeTime() {
        return warningTime;
    }

    public void setWarningTimeTime(final int warningTime) {
        this.warningTime = warningTime;
    }

    public void loadSetting(final Context applicationContext) {
        SharedPreferences settings = applicationContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        language = settings.getInt("language", language);
        roundSets = settings.getInt("roundSets", roundSets);
        setTime = settings.getInt("setTime", setTime);
        preparationTime = settings.getInt("preparationTime", preparationTime);
        warningTime = settings.getInt("warningTime", warningTime);
        lines = settings.getInt("lines", lines);
        linesRotation = LinesRotation.values()[settings.getInt("linesRotation", linesRotation.ordinal())];
        numberOfSets = settings.getInt("numberOfSets", numberOfSets);
        continuous = settings.getBoolean("continuous", continuous);
        delayedStartEnabled = settings.getBoolean("delayedStartEnabled", delayedStartEnabled);
        delayedStartTime = Time.valueOf(settings.getString("delayedStartTime", delayedStartTime.toString()));
    }

    public void saveSetting(final Context applicationContext) {
        SharedPreferences settings = applicationContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit().clear();
        editor.putInt("language", language);
        editor.putInt("roundSets", roundSets);
        editor.putInt("setTime", setTime);
        editor.putInt("preparationTime", preparationTime);
        editor.putInt("warningTime", warningTime);
        editor.putInt("lines", lines);
        editor.putInt("linesRotation", linesRotation.ordinal());
        editor.putBoolean("continuous", continuous);
        editor.putInt("numberOfSets", numberOfSets);
        editor.putBoolean("delayedStartEnabled", delayedStartEnabled);
        editor.putString("delayedStartTime", delayedStartTime.toString());
        editor.apply();
    }

    public boolean getContinuous() {
        return continuous;
    }

    public void setContinuous(final boolean continuous) {
        this.continuous = continuous;
    }

    public int getNumberOfSets() {
        return numberOfSets;
    }

    public void setNumberOfSets(final int numberOfSets) {
        this.numberOfSets = numberOfSets;
    }

    public String getEssid() {
        return Settings.SEMAFORKY_ESSID;
    }

    public String getPassword() {
        return Settings.SEMAFORKY_PASSWORD;
    }
}
