package dev.ultreon.pythonc;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

class PyFunction implements JvmCallable, Symbol {
    private final PyClass owner;
    private final String name;
    private final Type returnType;
    private final int lineNo;

    public PyFunction(PyClass owner, String name, Type returnType, int lineNo) {
        this.owner = owner;
        this.name = name;
        this.returnType = returnType;
        this.lineNo = lineNo;
    }

    @Override
    public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        return null;
    }

    @Override
    public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
        throw new RuntimeException("not supported");
    }

    @Override
    public int lineNo() {
        return lineNo;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public PyClass owner(PythonCompiler compiler) {
        return owner;
    }

    @Override
    public Type type(PythonCompiler compiler) {
        return returnType;
    }

    @Override
    public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit) {
        throw new RuntimeException("not supported");
    }
}
