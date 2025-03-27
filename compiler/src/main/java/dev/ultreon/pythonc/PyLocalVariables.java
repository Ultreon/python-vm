package dev.ultreon.pythonc;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.Map;

public class PyLocalVariables {
    private final Map<String, PyVariable> byName = new HashMap<>();
    private final PyFunction function;
    private int currentVariableIndex = 0;

    public PyLocalVariables(PyFunction function) {
        this.function = function;
    }

    public PyVariable get(String name) {
        return byName.get(name);
    }

    public PyVariable createVariable(PythonCompiler compiler, String name, PyExpr expr, boolean boxed, Location location) {
        MethodVisitor mv = compiler.mv == null ? compiler.rootInitMv : compiler.mv;
        Label label = new Label();
        compiler.writer.localVariable(name, "Ljava/lang/Object;", null, location.lineStart(), label, label, currentVariableIndex);
        PyVariable value = new PyVariable(name, currentVariableIndex, label, location);
        byName.put(name, value);

        Object preloaded = expr.preload(mv, compiler, false);
        expr.load(mv, compiler, preloaded, boxed);
        compiler.writer.box(compiler.writer.getContext().peek());
        compiler.writer.storeObject(currentVariableIndex, Type.getType(Object.class));
        currentVariableIndex++;

        return value;
    }

    @Deprecated
    public PyVariable createVariable(PythonCompiler compiler, String name, boolean boxed, Location location) {
        int index = 1;
        MethodVisitor mv = compiler.mv == null ? compiler.rootInitMv : compiler.mv;
        Label label = new Label();
        compiler.writer.localVariable(name, Type.getType(Object.class).getDescriptor(), null, location.lineStart(), label, label, currentVariableIndex);
        PyVariable value = new PyVariable(name, currentVariableIndex, label, location);
        byName.put(name, value);

        currentVariableIndex++;

        return value;
    }

    public Self createThis(PythonCompiler compiler, Location location) {
        createVariable(compiler, "self", true, location);
        return new Self(function.owner.type(compiler), location);
    }

    public void createParam(String name, PyVariable pyVariable) {
        byName.put(name, pyVariable);
        currentVariableIndex += switch (pyVariable.type().getSort()) {
            case Type.LONG, Type.DOUBLE -> 2;
            default -> 1;
        };
    }
}
