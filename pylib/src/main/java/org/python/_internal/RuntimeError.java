package org.python._internal;

public class RuntimeError extends Error {
    public RuntimeError() {
    }

    public RuntimeError(String message) {
        super(message);
    }

    public RuntimeError(String message, Throwable cause) {
        super(message, cause);
    }

    public RuntimeError(Throwable cause) {
        super(cause);
    }
}
