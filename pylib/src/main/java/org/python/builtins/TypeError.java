package org.python.builtins;

public class TypeError extends PyException {
    public TypeError(Object... message) {
        super(message);
    }
}
