package com.vajsoft.semaforky.controllers;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import com.vajsoft.semaforky.Semaforky;
import com.vajsoft.semaforky.utils.State;
import com.vajsoft.semaforky.utils.StateMachine;

import static com.vajsoft.semaforky.controllers.SemaforkyState.FIRE;
import static com.vajsoft.semaforky.controllers.SemaforkyState.MANUAL_CONTROL;
import static com.vajsoft.semaforky.controllers.SemaforkyState.READY;
import static com.vajsoft.semaforky.controllers.SemaforkyState.ROUND_STARTED;
import static com.vajsoft.semaforky.controllers.SemaforkyState.ROUND_STOPPED;
import static com.vajsoft.semaforky.controllers.SemaforkyState.SETTINGS;
import static com.vajsoft.semaforky.controllers.SemaforkyState.SET_CANCELED;
import static com.vajsoft.semaforky.controllers.SemaforkyState.SET_STARTED;
import static com.vajsoft.semaforky.controllers.SemaforkyState.SET_STOPPED;
import static com.vajsoft.semaforky.controllers.SemaforkyState.STARTED;
import static com.vajsoft.semaforky.controllers.SemaforkyState.WARNING;

/**
 * Semaforky state machine. Defines sttate names and state change implementation.
 */
public class SemaforkyMachine extends StateMachine {

    private int currentSet = 1;
    private int currentLine = 0;
    private Semaforky semaforky;

    public SemaforkyMachine(Semaforky semaforky) {
        this.semaforky = semaforky;
        initializeStates();
    }

    public int getCurrentLine() {
        return currentLine;
    }

    public int getCurrentSet() {
        return currentSet;
    }

    private void initializeStates() {

        setCurrent(addState(new State<SemaforkyState>(STARTED, new SemaforkyState[]{ROUND_STARTED, SETTINGS, MANUAL_CONTROL}) {
            @Override
            public void run(State previous) {
                //Nothing to do
            }
        }));
        addState(new State<SemaforkyState>(ROUND_STARTED, new SemaforkyState[]{SET_STARTED, ROUND_STOPPED}) {
            @Override
            public void run(State previous) {
                semaforky.getScheduler().StartRound();
                semaforky.getGuiEventReceiver().updateGui();
                currentSet = 1;
                currentLine = 0;
                moveTo(SET_STARTED);
            }
        });
        addState(new State<SemaforkyState>(SET_STARTED, new SemaforkyState[]{READY}) {
            @Override
            public void run(State previous) {
                semaforky.getGuiEventReceiver().updateGui();
                semaforky.getMainController().playSiren(2);
                semaforky.getScheduler().StartSet();
            }
        });
        addState(new State<SemaforkyState>(READY, new SemaforkyState[]{FIRE, SET_CANCELED}) {
            @Override
            public void run(State previous) {
                semaforky.getGuiEventReceiver().updateGui();
            }
        });
        addState(new State<SemaforkyState>(FIRE, new SemaforkyState[]{SET_STOPPED, SET_CANCELED, WARNING}) {
            @Override
            public void run(State previous) {
                semaforky.getGuiEventReceiver().updateGui();
                semaforky.getMainController().playSiren(1);
            }
        });
        addState(new State<SemaforkyState>(WARNING, new SemaforkyState[]{SET_CANCELED, SET_STOPPED}) {
            @Override
            public void run(State previous) {
                semaforky.getGuiEventReceiver().updateGui();
            }
        });
        addState(new State<SemaforkyState>(SET_STOPPED, new SemaforkyState[]{SET_STARTED, ROUND_STOPPED}) {
            @Override
            public void run(State previous) {
                semaforky.getScheduler().StopSet();
                updateSetAndLine();
                semaforky.getGuiEventReceiver().updateGui();
                updateState();
            }

            private void updateState() {
                if (currentLine == 0) {
                    // remain stopped (or handle special cases) if set is over
                    semaforky.getMainController().playSiren(3);
                    semaforky.getMainController().updateClocks(0);
                    semaforky.getMainController().updateSemaphores(SemaphoreController.SemaphoreLight.RED);
                    if (semaforky.getSettings().getContinuous()) {
                        if (currentSet <= semaforky.getSettings().getNumberOfSets()) {
                            moveTo(SET_STARTED);
                        } else {
                            moveTo(ROUND_STOPPED);
                        }
                    }
                } else {
                    // otherwise continue
                    moveTo(SET_STARTED);
                }
            }

            private void updateSetAndLine() {
                if ((currentLine + 1) < semaforky.getSettings().getLines()) {
                    // if number of line is higher than current line, increase line
                    currentLine++;
                } else {
                    // otherwise, increase set
                    currentSet++;
                    currentLine = 0;
                }
            }
        });
        addState(new State<SemaforkyState>(SET_CANCELED, new SemaforkyState[]{ROUND_STOPPED, SET_STARTED}) {
            @Override
            public void run(State previous) {
                semaforky.getScheduler().CancelSet();
                semaforky.getGuiEventReceiver().updateGui();
                semaforky.getMainController().playSiren(2);
            }
        });
        addState(new State<SemaforkyState>(ROUND_STOPPED, new SemaforkyState[]{SETTINGS, ROUND_STARTED}) {
            @Override
            public void run(State previous) {
                semaforky.getGuiEventReceiver().updateGui();
                semaforky.getMainController().playSiren(4);
                semaforky.getScheduler().EndRound();
            }
        });
        addState(new State<SemaforkyState>(SETTINGS, new SemaforkyState[]{STARTED}) {
            @Override
            public void run(State previous) {
                semaforky.getGuiEventReceiver().updateGui();
            }
        });
        addState(new State<SemaforkyState>(MANUAL_CONTROL, new SemaforkyState[]{STARTED}) {
            @Override
            public void run(State previous) {
                semaforky.getGuiEventReceiver().updateGui();
            }
        });
    }
};
