package org.python._internal;

public class PythonVMBug extends Error {
    public PythonVMBug() {
    }

    public PythonVMBug(String message) {
        super(message);
    }

    public PythonVMBug(String message, Throwable cause) {
        super(message, cause);
    }

    public PythonVMBug(Throwable cause) {
        super(cause);
    }
}
