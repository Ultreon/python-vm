package dev.ultreon.pythonc.functions;

import dev.ultreon.pythonc.JvmWriter;
import dev.ultreon.pythonc.PythonCompiler;
import dev.ultreon.pythonc.classes.JvmClass;
import dev.ultreon.pythonc.expr.PySymbol;
import dev.ultreon.pythonc.functions.param.PyParameter;
import dev.ultreon.pythonc.statement.PyStatement;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

import java.util.StringJoiner;

public abstract class PyBaseFunction extends PyStatement implements PySymbol {
    private final String name;
    private final PyParameter[] parameters;
    protected JvmClass returnType;

    public PyBaseFunction(String name, PyParameter[] parameters, JvmClass returnType) {
        this.name = name;
        this.parameters = parameters;
        this.returnType = returnType;
    }

    public abstract void writeFunction(PythonCompiler compiler, JvmWriter writer);

    public void writeContent(PythonCompiler compiler, MethodNode node) {

    }

    public abstract boolean isStatic();

    @Override
    public void writeStatement(PythonCompiler compiler, JvmWriter writer) {
        throw new AssertionError("Can't write this function" + writer.lastLocation().toAdvancedString());
    }

    public String signature() {
        StringJoiner joiner = new StringJoiner("");
        for (PyParameter parameter : parameters()) {
            Type type = parameter.type();
            if (type == null) type = Type.getType(Object.class);
            String descriptor = type.getDescriptor();
            joiner.add(descriptor);
        }
        return "(" + joiner + ")" + (returnType == null ? "V" : returnType.type().getDescriptor());
    }

    public String name() {
        return name;
    }

    public PyParameter[] parameters() {
        return parameters;
    }

    public final Type type() {
        return returnType();
    }

    public Type returnType() {
        if (returnType == null) return Type.VOID_TYPE;
        return returnType.type();
    }

    public JvmClass returnClass() {
        return returnType;
    }
}
