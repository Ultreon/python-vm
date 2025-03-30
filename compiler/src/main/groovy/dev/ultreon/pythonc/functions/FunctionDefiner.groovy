package dev.ultreon.pythonc.functions

import dev.ultreon.pythonc.PythonCompiler
import dev.ultreon.pythonc.lang.PyAST
import org.objectweb.asm.Type

interface FunctionDefiner extends PyAST {
    void addFunction(PyFunction function)
    default void addProperty(String name, Type type) { addProperty PythonCompiler.current, name, type }
    void addProperty(PythonCompiler compiler, String name, Type type)
    Type getType()
    boolean isModule()
}
