package com.vajsoft.semaforky.utils;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

/** State machine base. Holds state lists, current state and perform state transition.
 * */
public class StateMachine {
    ArrayList<State> states = new ArrayList<State>();
    State currentState = null;
    public State AddState(State state)  {
        states.add(state);
        return state;
    }
    public void SetCurrent(State state) {
        assert(states.contains(state));
        currentState = state;
    }
    public State GetCurrenState() {
        return currentState;
    }
    public boolean MoveTo(String stateName) {
        if(!Arrays.asList(currentState.next).contains(stateName))
        {
            Log.d("Failed to change state from " + currentState.name + " to " + stateName, "");
            return false;
        }
        State state = SearchArray.findFirst(states, stateName,
                new SearchArray.Comparator<State, String>() {
                    public boolean isEqual(State item, String value) {
                        return item.name.equals(value);
                    }
                }
        );
        if (state != null) {
            State previousState = currentState;
            currentState = state;
            state.run(previousState);
            return true;
        }
        return false;
    }
}

