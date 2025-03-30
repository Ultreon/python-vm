package dev.ultreon.pythonc.classes

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.PythonCompiler
import dev.ultreon.pythonc.functions.FunctionDefiner

interface JvmClassCompilable extends FunctionDefiner {
    void writeClass(PythonCompiler compiler, JvmWriter writer)
}
