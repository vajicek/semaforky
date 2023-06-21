package com.vajsoft.semaforky.utils;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * State machine base. Holds state lists, current state and perform state transition.
 */
public class StateMachine<T> {
    private static final Logger LOGGER = Logger.getLogger(StateMachine.class.getName());
    private final ArrayList<State<T>> states = new ArrayList<>();
    private State<T> currentState = null;

    public State<T> addState(State<T> state) {
        states.add(state);
        return state;
    }

    public void setCurrent(State<T> state) {
        assert (states.contains(state));
        LOGGER.log(Level.CONFIG, "setCurrent({0})", state.name.toString());
        currentState = state;
    }

    public State<T> getCurrenState() {
        LOGGER.log(Level.CONFIG, "getCurrenState() = {0})", currentState.name.toString());
        return currentState;
    }

    public void moveTo(T stateName) {
        if (!Arrays.asList(currentState.next).contains(stateName)) {
            LOGGER.log(Level.CONFIG, "Failed to change state from {0} to {1}", new Object[]{currentState.name, stateName});
            return;
        }
        State<T> state = SearchArray.findFirst(states, stateName,
                new SearchArray.Comparator<State<T>, T>() {
                    public boolean isEqual(State<T> item, T value) {
                        return item.name.equals(value);
                    }
                }
        );
        if (state != null) {
            State<T> previousState = currentState;
            setCurrent(state);
            state.run(previousState);
        }
    }
}

