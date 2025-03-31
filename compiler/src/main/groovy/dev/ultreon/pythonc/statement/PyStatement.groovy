package dev.ultreon.pythonc.statement

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.PythonCompiler
import dev.ultreon.pythonc.lang.PyAST

trait PyStatement implements PyAST {
    @Override
    final void writeCode(PythonCompiler compiler, JvmWriter writer) {
        writeStatement(compiler, writer)
        compiler.checkPop(location)
    }

    abstract void writeStatement(PythonCompiler compiler, JvmWriter writer)
}
