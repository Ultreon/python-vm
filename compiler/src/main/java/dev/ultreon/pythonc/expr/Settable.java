package dev.ultreon.pythonc.expr;

import dev.ultreon.pythonc.JvmWriter;
import dev.ultreon.pythonc.PythonCompiler;

public interface Settable {
    void set(PythonCompiler compiler, JvmWriter writer, PyExpression expr);
}
