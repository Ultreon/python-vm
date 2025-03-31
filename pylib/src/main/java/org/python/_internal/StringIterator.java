package org.python._internal;

public class StringIterator {
    private final String self;
    private int index = 0;

    public StringIterator(String self) {
        this.self = self;
    }

    public boolean hasNext() {
        return index < self.length();
    }

    public String next() {
        return String.valueOf(self.charAt(index++));
    }
}
