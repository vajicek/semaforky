package com.vajsoft.semaforky.utils;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import java.util.Collection;

/** Search array according to given comparator functor value.
 * */
public class SearchArray <T> {

    public interface Comparator <T, V> {
        public boolean isEqual(T item, V value);
    }

    public static <T, V, C extends Comparator<T,V>> T findFirst(Collection<T> list, V value, C comparator) {
        for (T item : list) {
            if(comparator.isEqual(item, value)) {
                return item;
            }
        }
        return null;
    }
}