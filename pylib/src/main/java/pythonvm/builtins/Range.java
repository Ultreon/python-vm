package pythonvm.builtins;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public class Range implements Iterable<Long> {
    public long start;
    public long stop;
    public long step;

    public Range(long stop) {
        this.start = 0L;
        this.stop = stop;
        this.step = 1L;
    }

    public Range(long start, long stop) {
        this.start = start;
        this.stop = stop;
        this.step = 1L;
    }

    public Range(long start, long stop, long step) {
        this.start = start;
        this.stop = stop;
        this.step = step;
    }

    @Override
    public @NotNull Iterator<Long> iterator() {
        return new RangeIterator(this);
    }

    private static class RangeIterator implements Iterator<Long> {
        private final Range range;
        private long current;

        public RangeIterator(Range range) {
            this.range = range;

            this.current = range.start;
        }

        @Override
        public boolean hasNext() {
            return current + range.step <= range.stop;
        }

        @Override
        public Long next() {
            long result = current;
            current += range.step;
            return result;
        }
    }
}
