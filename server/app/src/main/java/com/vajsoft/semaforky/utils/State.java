package com.vajsoft.semaforky.utils;

/** State abstraction. State name and names of states it can change to.
 * */
abstract public class State {
    public String name;
    public String[] next;
    public State(String state_name, String[] next_states) {
        name = state_name;
        next = next_states;
    }
    public abstract void run(State previous);
};


