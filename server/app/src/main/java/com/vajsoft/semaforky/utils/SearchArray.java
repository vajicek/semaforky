package com.vajsoft.semaforky.utils;

import java.util.List;

/** Search array according to given comparator functor value.
 * */
public class SearchArray <T> {

    public interface Comparator <T, V> {
        public boolean isEqual(T item, V value);
    }

    public static <T, V, C extends Comparator<T,V>> T findFirst(List<T> list, V value, C comparator) {
        for (T item : list) {
            if(comparator.isEqual(item, value)) {
                return item;
            }
        }
        return null;
    }
}