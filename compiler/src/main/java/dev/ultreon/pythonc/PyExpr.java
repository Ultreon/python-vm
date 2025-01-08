package dev.ultreon.pythonc;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

interface PyExpr {
    Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed);

    void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed);

    int lineNo();

    Type type(PythonCompiler compiler);
}
