package dev.ultreon.pythonc.functions

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.PythonCompiler
import dev.ultreon.pythonc.lang.PyAST

trait PyRootStatement implements PyAST {
    abstract void writeStatement(PythonCompiler compiler);

    @Override
    final void writeCode(PythonCompiler compiler, JvmWriter writer) {
        writeStatement(compiler)
    }
}
