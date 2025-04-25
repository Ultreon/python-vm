package org.python._internal;

import org.python.builtins.StopIteration;

import java.util.Iterator;
import java.util.Map;

public class EnumerateIterator implements PyObject {
    private final Iterable<?> iterable;
    private final Iterator<?> iterator;
    private int index;

    public EnumerateIterator(Iterable<?> iterable) {
        this.iterable = iterable;
        this.index = 0;

        this.iterator = iterable.iterator();
    }

    public PyObject __iter__() {
        return this;
    }

    public PyObject __next__() {
        if (!iterable.iterator().hasNext()) throw new StopIteration();
        return new PyTuple(new Object[]{index++, iterator.next()}, Map.of());
    }
}
