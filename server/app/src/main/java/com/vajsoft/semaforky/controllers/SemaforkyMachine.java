package com.vajsoft.semaforky.controllers;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import android.content.Context;
import android.os.PowerManager;

import com.vajsoft.semaforky.Semaforky;
import com.vajsoft.semaforky.data.Settings;
import com.vajsoft.semaforky.scheduler.Scheduler;
import com.vajsoft.semaforky.utils.State;
import com.vajsoft.semaforky.utils.StateMachine;

import static com.vajsoft.semaforky.controllers.SemaforkyState.*;

/** Semaforky state machine. Defines sttate names and state change implementation. */
public class SemaforkyMachine extends StateMachine {

    private int currentSet = 1;
    private int currentLine = 0;
    private Semaforky semaforky;
    private Settings settings;

    public SemaforkyMachine(Semaforky semaforky, final Settings settings) {
        this.semaforky = semaforky;
        this.settings = settings;
        initializeStates();
    }

    public int getCurrentLine() {
        return currentLine;
    }

    public int getCurrentSet() {
        return currentSet;
    }

    private void initializeStates() {

        setCurrent(addState(new State<SemaforkyState>(STARTED, new SemaforkyState[]{ROUND_STARTED, SETTINGS}) {
            @Override
            public void run(State previous) {
                //Nothing to do
            }
        }));
        addState(new State<SemaforkyState>(ROUND_STARTED, new SemaforkyState[]{SET_STARTED, ROUND_STOPPED}) {
            @Override
            public void run(State previous) {
                semaforky.getScheduler().StartRound();
                semaforky.getMainActivity().updateGui();
                currentSet = 1;
                currentLine = 0;
                moveTo(SET_STARTED);
            }
        });
        addState(new State<SemaforkyState>(SET_STARTED, new SemaforkyState[]{READY}) {
            @Override
            public void run(State previous) {
                semaforky.getMainActivity().updateGui();
                semaforky.getMainController().playSiren(2);
                semaforky.getScheduler().StartSet();
            }
        });
        addState(new State<SemaforkyState>(READY, new SemaforkyState[]{FIRE, SET_CANCELED}) {
            @Override
            public void run(State previous) {
                semaforky.getMainActivity().updateGui();
            }
        });
        addState(new State<SemaforkyState>(FIRE, new SemaforkyState[]{SET_STOPPED, SET_CANCELED, WARNING}) {
            @Override
            public void run(State previous) {
                semaforky.getMainActivity().updateGui();
                semaforky.getMainController().playSiren(1);
            }
        });
        addState(new State<SemaforkyState>(WARNING, new SemaforkyState[]{SET_CANCELED, SET_STOPPED}) {
            @Override
            public void run(State previous) {
                semaforky.getMainActivity().updateGui();
            }
        });
        addState(new State<SemaforkyState>(SET_STOPPED, new SemaforkyState[]{SET_STARTED, ROUND_STOPPED}) {
            @Override
            public void run(State previous) {
                semaforky.getScheduler().StopSet();

                if ((currentLine + 1) < settings.getLines()) {
                    // if number of line is higher than current line, increase line
                    currentLine++;
                } else {
                    // otherwise, increase set
                    currentSet++;
                    currentLine = 0;
                }
                semaforky.getMainActivity().updateGui();

                // if number of line was increased from zero, go on
                if (currentLine > 0) {
                    moveTo(SET_STARTED);
                } else {
                    semaforky.getMainController().playSiren(3);
                    if (settings.getContinuous()) {
                        if (currentSet <= settings.getNumberOfSets()) {
                            moveTo(SET_STARTED);
                        } else {
                            moveTo(ROUND_STOPPED);
                        }
                    }
                }
            }
        });
        addState(new State<SemaforkyState>(SET_CANCELED, new SemaforkyState[]{ROUND_STOPPED, SET_STARTED}) {
            @Override
            public void run(State previous) {
                semaforky.getScheduler().CancelSet();
                semaforky.getMainActivity().updateGui();
                semaforky.getMainController().playSiren(2);
            }
        });
        addState(new State<SemaforkyState>(ROUND_STOPPED, new SemaforkyState[]{SETTINGS, ROUND_STARTED}) {
            @Override
            public void run(State previous) {
                semaforky.getMainActivity().updateGui();
                semaforky.getMainController().playSiren(4);
                semaforky.getScheduler().EndRound();
            }
        });
        addState(new State<SemaforkyState>(SETTINGS, new SemaforkyState[]{STARTED}) {
            @Override
            public void run(State previous) {
                semaforky.getMainActivity().updateGui();
            }
        });
    }
};
