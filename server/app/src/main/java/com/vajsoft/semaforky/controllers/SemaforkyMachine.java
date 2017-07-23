package com.vajsoft.semaforky.controllers;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import com.vajsoft.semaforky.data.Settings;
import com.vajsoft.semaforky.scheduler.Scheduler;
import com.vajsoft.semaforky.utils.State;
import com.vajsoft.semaforky.utils.StateMachine;

/** Semaforky state machine. Defines sttate names and state change implementation.
 * */
public class SemaforkyMachine extends StateMachine {
    public static String STARTED = "started";
    public static String ROUND_STARTED = "round started";
    public static String SET_STARTED = "set started";
    public static String READY = "ready";
    public static String FIRE= "fire";
    public static String WARNING = "warning";
    public static String SET_STOPPED = "set stopped";
    public static String SET_CANCELED = "set canceled";
    public static String ROUND_STOPPED = "round stopped";
    public static String SETTINGS = "settings";

    private int currentSet = 1;
    private int currentLine = 0;

    private MainController mainController;
    private Scheduler scheduler;

    public int GetCurrentLine() {
        return currentLine;
    }

    public int GetCurrentSet() {
        return currentSet;
    }

    public SemaforkyMachine(MainController m, Scheduler s) {
        this.mainController = m;
        this.scheduler = s;

        SetCurrent(AddState(new State(STARTED, new String[] {ROUND_STARTED, SETTINGS}){
            @Override
            public void run(State previous) {
                //Nothing to do
            }
        }));
        AddState(new State(ROUND_STARTED, new String[] {SET_STARTED, ROUND_STOPPED}){
            @Override public void run(State previous) {
                scheduler.StartRound();
                mainController.GetMainActivity().UpdateGui();
                mainController.GetMainActivity().GetSoundManager().Play("buzzer", 1);
                currentSet = 1;
                currentLine = 0;
            }
        });
        AddState(new State(SET_STARTED, new String[] {READY}){
            @Override public void run(State previous) {
                mainController.GetMainActivity().UpdateGui();
                mainController.GetMainActivity().GetSoundManager().Play("buzzer", 1);
                scheduler.StartSet();
            }
        });
        AddState(new State(READY, new String[] {FIRE, SET_CANCELED}){
            @Override public void run(State previous) {
                mainController.GetMainActivity().UpdateGui();
            }
        });
        AddState(new State(FIRE, new String[] {SET_STOPPED, SET_CANCELED, WARNING}){
            @Override public void run(State previous) {
                mainController.GetMainActivity().UpdateGui();
                mainController.GetMainActivity().GetSoundManager().Play("buzzer", 1);
            }
        });
        AddState(new State(WARNING, new String[] {SET_CANCELED, SET_STOPPED}){
            @Override public void run(State previous) {
                mainController.GetMainActivity().UpdateGui();
            }
        });
        AddState(new State(SET_STOPPED, new String[] {SET_STARTED, ROUND_STOPPED}){
            @Override public void run(State previous) {
                scheduler.StopSet();
                if ((currentLine + 1) < Settings.getInstance().GetLines()) {
                    currentLine++;
                } else {
                    currentSet++;
                    currentLine = 0;
                }
                mainController.GetMainActivity().UpdateGui();
                mainController.GetMainActivity().GetSoundManager().Play("buzzer", 2);
            }
        });
        AddState(new State(SET_CANCELED, new String[] {ROUND_STOPPED, SET_STARTED}) {
            @Override public void run(State previous) {
                scheduler.CancelSet();
                mainController.GetMainActivity().UpdateGui();
                mainController.GetMainActivity().GetSoundManager().Play("buzzer", 2);
            }
        });
        AddState(new State(ROUND_STOPPED, new String[] {SETTINGS, ROUND_STARTED}){
            @Override public void run(State previous) {
                mainController.GetMainActivity().UpdateGui();
                mainController.GetMainActivity().GetSoundManager().Play("buzzer", 3);
                scheduler.EndRound();
            }
        });
        AddState(new State(SETTINGS, new String[] {STARTED}){
            @Override public void run(State previous) {
                mainController.GetMainActivity().UpdateGui();
            }
        });
    }
};
