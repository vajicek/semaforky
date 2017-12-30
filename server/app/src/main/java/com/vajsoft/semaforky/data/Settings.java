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
    private static Settings instance = null;

    ;
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

    public int GetLines() {
        return lines;
    }

    public int GetLanguage() {
        return language;
    }

    public String GetLanguageCode() {
        switch (language) {
            case 1:
                return "cs";
            default:
                return "";
        }
    }

    public LinesRotation GetLinesRotation() {
        return linesRotation;
    }

    public int GetRoundSets() {
        return roundSets;
    }

    public int GetSetTime() {
        return setTime;
    }

    public int GetPreparationTimeTime() {
        return preparationTime;
    }

    public int GetWarningTimeTime() {
        return warningTime;
    }

    public void LoadSetting(Context applicationContext) {
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

    public void SaveSetting(Context applicationContext) {
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

    public void SetLinesRotation(LinesRotation linesRotation) {
        this.linesRotation = linesRotation;
    }

    public void SetLanguage(int selectedLanguageNo) {
        language = selectedLanguageNo;
    }

    public void SetLines(int lines) {
        this.lines = lines;
    }

    public void SetRoundSets(int roundSets) {
        this.roundSets = roundSets;
    }

    public void SetSetTime(int setTime) {
        this.setTime = setTime;
    }

    public void SetPreparationTimeTime(int preparationTime) {
        this.preparationTime = preparationTime;
    }

    public void SetWarningTimeTime(int warningTime) {
        this.warningTime = warningTime;
    }

    public enum LinesRotation {
        SIMPLE,
        ALTERNATING
    }
}
