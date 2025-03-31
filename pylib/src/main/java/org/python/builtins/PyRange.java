package org.python.builtins;

import org.python._internal.PyObject;

public class PyRange implements PyObject {
    private int start;
    private int stop;
    private int step;

    public PyRange(int stop) {
        this(0, stop);
    }

    public PyRange(int start, int stop) {
        this(start, stop, 1);
    }

    public PyRange(int start, int stop, int step) {
        this.start = start;
        this.stop = stop;
        this.step = step;
    }

    public int __len__() {
        return (stop - start) / step;
    }

    public PyObject __iter__() {
        return new RangeIterator(this);
    }

    private static class RangeIterator implements PyObject {
        private final PyRange pyRange;
        int current;

        public RangeIterator(PyRange pyRange) {
            this.pyRange = pyRange;
            this.current = pyRange.start;
        }

        public Object __next__() {
            if (current >= pyRange.stop) {
                throw new StopIteration();
            }
            int result = current;
            current += pyRange.step;
            return result;
        }
    }
}
