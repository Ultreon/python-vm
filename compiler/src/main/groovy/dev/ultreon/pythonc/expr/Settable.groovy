package dev.ultreon.pythonc.expr;

import dev.ultreon.pythonc.JvmWriter;
import dev.ultreon.pythonc.PythonCompiler;

interface Settable {
    void set(PythonCompiler compiler, JvmWriter writer, PyExpression expr)
    String getName()
}
