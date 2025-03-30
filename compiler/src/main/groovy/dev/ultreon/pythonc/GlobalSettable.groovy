package dev.ultreon.pythonc

import dev.ultreon.pythonc.expr.PyExpression
import dev.ultreon.pythonc.expr.Settable

interface GlobalSettable extends Settable {
    void set(PythonCompiler compiler, JvmWriter writer, PyExpression expr)
    String getName()

    default void unlockGlobal(PythonCompiler compiler, JvmWriter writer) {
        compiler.unlock(this)
    }
}
