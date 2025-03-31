package dev.ultreon.pythonc.functions

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.PythonCompiler
import dev.ultreon.pythonc.classes.JvmClass
import dev.ultreon.pythonc.expr.PySymbol
import dev.ultreon.pythonc.functions.param.PyParameter
import dev.ultreon.pythonc.statement.PyStatement
import org.objectweb.asm.Type
import org.objectweb.asm.tree.MethodNode

abstract class PyBaseFunction implements PyStatement, PySymbol {
    private final String name
    private final PyParameter[] parameters
    protected JvmClass returnType

    PyBaseFunction(String name, PyParameter[] parameters, JvmClass returnType) {
        this.name = name
        this.parameters = parameters
        this.returnType = returnType
    }

    abstract void writeFunction(PythonCompiler compiler, JvmWriter writer);

    void writeContent(PythonCompiler compiler, MethodNode node) {

    }

    abstract boolean isStatic();

    @Override
    void writeStatement(PythonCompiler compiler, JvmWriter writer) {
        throw new IllegalStateException("Can't write this function" + writer.lastLocation().formattedText)
    }

    String signature() {
        StringJoiner joiner = new StringJoiner("")
        for (PyParameter parameter : parameters()) {
            Type type = parameter.type()
            if (type == null) type = Type.getType(Object)
            String descriptor = type.descriptor
            joiner.add(descriptor)
        }
        return "(" + joiner + ")" + (returnType == null ? "V" : returnType.type.descriptor)
    }

    String getName() {
        return name
    }

    PyParameter[] parameters() {
        return parameters
    }

    final Type type() {
        return returnType()
    }

    Type returnType() {
        if (returnType == null) return Type.VOID_TYPE
        return returnType.type
    }

    JvmClass returnClass() {
        return returnType
    }
}
