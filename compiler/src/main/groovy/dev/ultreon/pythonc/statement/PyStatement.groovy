package dev.ultreon.pythonc.statement

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.PythonCompiler
import dev.ultreon.pythonc.lang.PyAST
import org.objectweb.asm.Label

interface PyStatement extends PyAST {
    default void trackLast(PyStatement statement) {
        PythonCompiler.current.writer.lastLocation(statement.location)
    }

    @Override
    default final void writeCode(PythonCompiler compiler, JvmWriter writer) {
        if (compiler.rootInitMv != null || compiler.methodOut != null) {
            Label label = writer.newLabel()
            if (location == null) throw new IllegalStateException("Statement class '${getClass().simpleName}' has no location")
            writer.label(label)
            writer.lineNumber(location.lineStart, label)
        }
        writeStatement(compiler, writer)
        compiler.checkPop(location)
        trackLast(this)
    }

    abstract void writeStatement(PythonCompiler compiler, JvmWriter writer)
}
