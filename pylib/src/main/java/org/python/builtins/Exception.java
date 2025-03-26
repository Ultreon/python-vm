package org.python.builtins;

import org.python._internal.PythonException;

import java.io.PrintStream;
import java.io.PrintWriter;

public class Exception extends PythonException {
    public Exception(String message) {
        super(message);
    }
}
