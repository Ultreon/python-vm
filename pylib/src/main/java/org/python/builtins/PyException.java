package org.python.builtins;

import org.python._internal.PythonException;

import java.util.Arrays;
import java.util.stream.Collectors;

public class PyException extends PythonException {
    public PyException(Object... args) {
        super(Arrays.stream(args).map(o -> o == null ? "NoneType" : o.toString()).collect(Collectors.joining(" ")));
    }
}
