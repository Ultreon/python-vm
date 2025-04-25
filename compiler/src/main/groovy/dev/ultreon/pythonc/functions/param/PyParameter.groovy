package dev.ultreon.pythonc.functions.param

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.PythonCompiler
import dev.ultreon.pythonc.classes.JvmClass
import dev.ultreon.pythonc.expr.PyExpression
import dev.ultreon.pythonc.expr.PySymbol
import dev.ultreon.pythonc.lang.PyAST
import org.jetbrains.annotations.Nullable
import org.objectweb.asm.Type

abstract class PyParameter implements PyAST, PySymbol {
    public int index
    private final String name

    protected PyParameter(String name) {
        this.name = name
    }

    int index() {
        return index
    }

    String getName() {
        return name
    }

    @Override
    abstract void writeCode(PythonCompiler compiler, JvmWriter writer);

    abstract @Nullable Type type();

    final @Nullable JvmClass typeClass() {
        Type type = type()
        if (type == null) return null
        return PythonCompiler.classCache.require(PythonCompiler.current, type)
    }

    abstract SpecialParameterType specialType();

    @Override
    void writeCall(PythonCompiler compiler, JvmWriter writer, List<PyExpression> args, Map<String, PyExpression> kwargs) {
        write(compiler, writer)
        writer.createArgs(args)
        writer.createKwargs(kwargs)
        writer.dynamicCall()
    }

    @Override
    boolean equals(Object obj) {
        if (obj instanceof PyParameter) {
            return index == ((PyParameter) obj).index && type() == ((PyParameter) obj).type()
        }
        return false
    }

    @Override
    int hashCode() {
        def hash = 0
        hash = hash * 31 + index
        hash = hash * 31 + (type() == null ? Type.getType(Object).hashCode() : type().hashCode())
        return hash
    }

    @Override
    String toString() {
        return name
    }
}
