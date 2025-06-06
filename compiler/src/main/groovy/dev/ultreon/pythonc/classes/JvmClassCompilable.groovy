package dev.ultreon.pythonc.classes

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.PythonCompiler
import dev.ultreon.pythonc.expr.PySymbol
import dev.ultreon.pythonc.functions.FunctionDefiner

interface JvmClassCompilable extends FunctionDefiner, PySymbol {
    void writeClass(PythonCompiler compiler, JvmWriter writer)
}
