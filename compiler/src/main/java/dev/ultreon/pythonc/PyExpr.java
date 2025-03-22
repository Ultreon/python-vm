package dev.ultreon.pythonc;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public interface PyExpr {
    Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed);

    void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed);

    int lineNo();

    default int columnNo() {
        return -1;
    }

    Type type(PythonCompiler compiler);
}
