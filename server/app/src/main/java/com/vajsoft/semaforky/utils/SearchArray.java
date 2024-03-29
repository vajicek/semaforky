package com.vajsoft.semaforky.utils;

/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

import java.util.Collection;

/**
 * Search array according to given comparator functor value.
 */
public class SearchArray {

    public static <T, V, C extends Comparator<T, V>> T findFirst(final Collection<T> list, final V value, final C comparator) {
        for (T item : list) {
            if (comparator.isEqual(item, value)) {
                return item;
            }
        }
        return null;
    }

    public interface Comparator<T, V> {
        boolean isEqual(final T item, final V value);
    }
}