package dev.ultreon.pythonc;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public interface PyExpr {
    Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed);

    void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed);

    void expectReturnType(PythonCompiler compiler, JvmClass returnType, Location location);

    Type type(PythonCompiler compiler);

    Location location();
}
