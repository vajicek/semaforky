package com.vajsoft.semaforky.controllers;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import com.vajsoft.semaforky.activities.SemaphoreWidget;
import com.vajsoft.semaforky.data.Settings;
import com.vajsoft.semaforky.scheduler.Scheduler;
import com.vajsoft.semaforky.utils.State;
import com.vajsoft.semaforky.utils.StateMachine;

import static com.vajsoft.semaforky.controllers.SemaforkyState.*;

/** Semaforky state machine. Defines sttate names and state change implementation. */
public class SemaforkyMachine extends StateMachine {

    private int currentSet = 1;
    private int currentLine = 0;

    private MainController mainController;
    private Scheduler scheduler;

    public SemaforkyMachine(MainController m, Scheduler s) {
        this.mainController = m;
        this.scheduler = s;

        setCurrent(addState(new State<SemaforkyState>(STARTED, new SemaforkyState[]{ROUND_STARTED, SETTINGS}) {
            @Override
            public void run(State previous) {
                //Nothing to do
            }
        }));
        addState(new State<SemaforkyState>(ROUND_STARTED, new SemaforkyState[]{SET_STARTED, ROUND_STOPPED}) {
            @Override
            public void run(State previous) {
                scheduler.StartRound();
                mainController.getMainActivity().updateGui();
                currentSet = 1;
                currentLine = 0;
                moveTo(SET_STARTED);
            }
        });
        addState(new State<SemaforkyState>(SET_STARTED, new SemaforkyState[]{READY}) {
            @Override
            public void run(State previous) {
                mainController.getMainActivity().updateGui();
                mainController.playSiren(2);
                scheduler.StartSet();
            }
        });
        addState(new State<SemaforkyState>(READY, new SemaforkyState[]{FIRE, SET_CANCELED}) {
            @Override
            public void run(State previous) {
                mainController.getMainActivity().updateGui();
            }
        });
        addState(new State<SemaforkyState>(FIRE, new SemaforkyState[]{SET_STOPPED, SET_CANCELED, WARNING}) {
            @Override
            public void run(State previous) {
                mainController.getMainActivity().updateGui();
                mainController.playSiren(1);
            }
        });
        addState(new State<SemaforkyState>(WARNING, new SemaforkyState[]{SET_CANCELED, SET_STOPPED}) {
            @Override
            public void run(State previous) {
                mainController.getMainActivity().updateGui();
            }
        });
        addState(new State<SemaforkyState>(SET_STOPPED, new SemaforkyState[]{SET_STARTED, ROUND_STOPPED}) {
            @Override
            public void run(State previous) {
                scheduler.StopSet();
                if ((currentLine + 1) < Settings.getInstance().GetLines()) {
                    currentLine++;
                } else {
                    currentSet++;
                    currentLine = 0;
                }
                mainController.getMainActivity().updateGui();

                if (currentLine > 0) {
                    moveTo(SET_STARTED);
                } else {
                    mainController.playSiren(3);
                }
            }
        });
        addState(new State<SemaforkyState>(SET_CANCELED, new SemaforkyState[]{ROUND_STOPPED, SET_STARTED}) {
            @Override
            public void run(State previous) {
                scheduler.CancelSet();
                mainController.getMainActivity().updateGui();
                mainController.playSiren(2);
            }
        });
        addState(new State<SemaforkyState>(ROUND_STOPPED, new SemaforkyState[]{SETTINGS, ROUND_STARTED}) {
            @Override
            public void run(State previous) {
                mainController.getMainActivity().updateGui();
                mainController.playSiren(4);
                scheduler.EndRound();
            }
        });
        addState(new State<SemaforkyState>(SETTINGS, new SemaforkyState[]{STARTED}) {
            @Override
            public void run(State previous) {
                mainController.getMainActivity().updateGui();
            }
        });
    }

    public int getCurrentLine() {
        return currentLine;
    }

    public int getCurrentSet() {
        return currentSet;
    }
};
