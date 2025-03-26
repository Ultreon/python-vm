package org.python.builtins;

import java.lang.reflect.Array;
import java.util.Iterator;

class ArrayIterator implements Iterator<Object> {
    private final Object array;
    private int index = 0;

    public ArrayIterator(Object o) {
        this.array = o;
    }

    @Override
    public boolean hasNext() {
        return index < Array.getLength(array);
    }

    @Override
    public Object next() {
        return Array.get(array, index++);
    }
}
