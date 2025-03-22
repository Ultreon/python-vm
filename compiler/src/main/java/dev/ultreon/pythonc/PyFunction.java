package dev.ultreon.pythonc;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class PyFunction implements JvmFunction, Symbol {
    private final PyClass owner;
    private final String name;
    private final Type[] paramTypes;
    private final Type returnType;
    private final int lineNo;
    private JvmClass returnClass;

    public PyFunction(PyClass owner, String name, Type[] paramTypes, Type returnType, int lineNo) {
        this.owner = owner;
        this.name = name;
        this.paramTypes = paramTypes;
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

    @Override
    public void invoke(Object callArgs, Runnable paramInit) {
        throw new AssertionError("DEBUG");
    }

    @Override
    public Type returnType(PythonCompiler compiler) {
        return returnType;
    }

    @Override
    public JvmClass returnClass(PythonCompiler compiler) {
        if (returnClass != null) return returnClass;
        if (!PythonCompiler.classCache.load(compiler, returnType)) {
            throw new CompilerException("Class '" + returnType.getClassName() + "' not found yet (" + compiler.getLocation(this) + ")");
        }
        return returnClass = PythonCompiler.classCache.get(returnType);
    }

    @Override
    public Type[] parameterTypes(PythonCompiler compiler) {
        return paramTypes;
    }

    @Override
    public JvmClass[] parameterClasses(PythonCompiler compiler) {
        JvmClass[] classes = new JvmClass[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            if (!PythonCompiler.classCache.load(compiler, paramTypes[i]))
                throw new CompilerException("Class '" + paramTypes[i].getClassName() + "' not found yet (" + compiler.getLocation(this) + ")");
            classes[i] = PythonCompiler.classCache.get(paramTypes[i]);
        }
        return classes;
    }
}
