package org.python.builtins;

import org.python._internal.PythonException;

public class TypeException extends PythonException {
    public TypeException(String message) {
        super(message);
    }
}
