package com.vajsoft.semaforky.utils;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

/** State machine base. Holds state lists, current state and perform state transition. */
public class StateMachine<T> {
    private ArrayList<State<T>> states = new ArrayList<>();
    private State<T> currentState = null;

    public State<T> addState(State<T> state) {
        states.add(state);
        return state;
    }

    public void setCurrent(State<T> state) {
        assert (states.contains(state));
        Log.d(StateMachine.class.getName(), "setCurrent(" + state.name.toString() + ")");
        currentState = state;
    }

    public State getCurrenState() {
        Log.d(StateMachine.class.getName(), "getCurrenState() = " + currentState.name.toString());
        return currentState;
    }

    public boolean moveTo(T stateName) {
        if (!Arrays.asList(currentState.next).contains(stateName)) {
            Log.d(StateMachine.class.getName(), "Failed to change state from " + currentState.name + " to " + stateName);
            return false;
        }
        State<T> state = SearchArray.findFirst(states, stateName,
                new SearchArray.Comparator<State<T>, T>() {
                    public boolean isEqual(State<T> item, T value) {
                        return item.name.equals(value);
                    }
                }
        );
        if (state != null) {
            State previousState = currentState;
            setCurrent(state);
            state.run(previousState);
            return true;
        }
        return false;
    }
}

