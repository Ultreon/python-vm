package dev.ultreon.pythonc;

import org.objectweb.asm.MethodVisitor;

public interface JvmConstructor {

    void write(MethodVisitor mv, PythonCompiler compiler, Runnable paramInit);
}
