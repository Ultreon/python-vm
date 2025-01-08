package dev.ultreon.pythonc;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

interface Symbol extends PyExpr {
    void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed);

    String name();

    Type type(PythonCompiler compiler);

    void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit);
}
