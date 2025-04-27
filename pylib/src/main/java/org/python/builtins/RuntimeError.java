package org.python.builtins;

public class RuntimeError extends PyException {
    public RuntimeError(Object... message) {
        super(message);
    }
}
