package com.vajsoft.semaforky.data;

/**
 * Created by vajicek on 10/21/2016.
 */

import java.io.Serializable;

/**
 * Holds. store, load setting of the application.
 */
public class Settings implements Serializable {

    private static Settings instance = null;

    protected Settings() {
    }

    public static Settings getInstance() {
        if(instance == null) {
            instance = new Settings();
        }
        return instance;
    }

    private int language = 0;
    private int roundSets = 10;
    private int setTime = 120;
    private int preparationTime = 10;
    private int warningTime = 30;
    private int lines = 1;

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

    public void LoadSetting() {
        //TODO: common vay to store app configuration
    }

    public void SaveSetting() {
        //TODO:
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
}
