package com.vajsoft.semaforky.utils;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

/**
 * State abstraction. State name and names of states it can change to.
 */
abstract public class State<T> {
    public T name;
    public T[] next;

    public State(T state_name, T[] next_states) {
        name = state_name;
        next = next_states;
    }

    public abstract void run(State previous);
};


