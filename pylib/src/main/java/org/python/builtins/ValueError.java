package org.python.builtins;

public class ValueError extends PyException {
    public ValueError(Object... message) {
        super(message);
    }
}
