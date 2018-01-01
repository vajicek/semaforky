package com.vajsoft.semaforky.data;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import android.content.Context;
import android.content.SharedPreferences;

import java.io.Serializable;

/**
 * Singleton class. Holds, store, load setting of the application.
 */
public class Settings implements Serializable {

    public static String PREFS_NAME = "semaforkySettings";
    public static final String SEMAFORKY_ESSID = "semaforky";
    public static final String SEMAFORKY_PASSWORD = "semaforky";
    private static Settings instance = null;

    private int language = 0;
    private int roundSets = 10;
    private int setTime = 120;
    private int preparationTime = 10;
    private int warningTime = 30;
    private int lines = 1;
    private LinesRotation linesRotation = LinesRotation.SIMPLE;
    protected Settings() {
    }

    public static Settings getInstance() {
        if (instance == null) {
            instance = new Settings();
        }
        return instance;
    }

    public int getLines() {
        return lines;
    }

    public int getLanguage() {
        return language;
    }

    public String getLanguageCode() {
        switch (language) {
            case 1:
                return "cs";
            default:
                return "";
        }
    }

    public LinesRotation getLinesRotation() {
        return linesRotation;
    }

    public int getRoundSets() {
        return roundSets;
    }

    public int getSetTime() {
        return setTime;
    }

    public int getPreparationTimeTime() {
        return preparationTime;
    }

    public int getWarningTimeTime() {
        return warningTime;
    }

    public void loadSetting(Context applicationContext) {
        SharedPreferences settings = applicationContext.getSharedPreferences(PREFS_NAME, 0);
        language = settings.getInt("homeScore", language);
        ;
        roundSets = settings.getInt("roundSets", roundSets);
        ;
        setTime = settings.getInt("setTime", setTime);
        ;
        preparationTime = settings.getInt("preparationTime", preparationTime);
        ;
        warningTime = settings.getInt("warningTime", warningTime);
        ;
        lines = settings.getInt("lines", lines);
        ;
        linesRotation = LinesRotation.values()[settings.getInt("linesRotation", linesRotation.ordinal())];
    }

    public void saveSetting(Context applicationContext) {
        SharedPreferences settings = applicationContext.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("language", language);
        editor.putInt("roundSets", roundSets);
        editor.putInt("setTime", setTime);
        editor.putInt("preparationTime", preparationTime);
        editor.putInt("warningTime", warningTime);
        editor.putInt("lines", lines);
        editor.putInt("linesRotation", linesRotation.ordinal());
        editor.apply();
    }

    public void setLinesRotation(LinesRotation linesRotation) {
        this.linesRotation = linesRotation;
    }

    public void setLanguage(int selectedLanguageNo) {
        language = selectedLanguageNo;
    }

    public void setLines(int lines) {
        this.lines = lines;
    }

    public void setRoundSets(int roundSets) {
        this.roundSets = roundSets;
    }

    public void setSetTime(int setTime) {
        this.setTime = setTime;
    }

    public void setPreparationTimeTime(int preparationTime) {
        this.preparationTime = preparationTime;
    }

    public void setWarningTimeTime(int warningTime) {
        this.warningTime = warningTime;
    }

    public enum LinesRotation {
        SIMPLE,
        ALTERNATING
    }
}
