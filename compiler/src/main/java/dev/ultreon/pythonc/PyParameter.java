package dev.ultreon.pythonc;

import org.objectweb.asm.Type;

public record PyParameter(TypedName typedName, int line, Type type, PyExpr defaultValue, boolean varArgs, boolean kwargs) {
    public PyParameter(TypedName name, int line, Type type, PyExpr defaultValue) {
        this(name, line, type, defaultValue, false);
    }

    public PyParameter(TypedName name, int line, Type type, PyExpr defaultValue, boolean varArgs) {
        this(name, line, type, defaultValue, varArgs, false);
    }
}
