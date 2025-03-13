package dev.ultreon.pythonc;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

class PyBuiltinClass implements Symbol {
    public final Type jvmName;
    public final Type jvmUnboxed;
    public final String extName;
    public final String pyName;

    public PyBuiltinClass(String jvmDesc, String extName, String pyName) {
        this(Type.getType(jvmDesc), extName, pyName);
    }

    public PyBuiltinClass(String jvmDesc, String jvmUnboxedDesc, String extName, String pyName) {
        this(Type.getType(jvmDesc), Type.getType(jvmUnboxedDesc), extName, pyName);
    }

    public PyBuiltinClass(Type jvmName, String extName, String pyName) {
        this(jvmName, jvmName, extName, pyName);
    }

    public PyBuiltinClass(Type jvmName, Type jvmUnboxed, String extName, String pyName) {
        this.jvmName = jvmName;
        this.jvmUnboxed = jvmUnboxed;
        this.extName = extName;
        this.pyName = pyName;
    }

    @Override
    public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        return null;
    }

    @Override
    public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
        if (jvmName.getSort() == Type.OBJECT) {
            compiler.writer.loadClass(jvmName);
            return;
        }
        throw new RuntimeException("Unknown JVM name: " + jvmName.getClassName());
    }

    @Override
    public int lineNo() {
        return 0;
    }

    @Override
    public String name() {
        return pyName;
    }

    @Override
    public Type type(PythonCompiler compiler) {
        return jvmUnboxed;
    }

    @Override
    public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit) {
        throw new CompilerException("Can't set class: " + pyName);
    }
}
