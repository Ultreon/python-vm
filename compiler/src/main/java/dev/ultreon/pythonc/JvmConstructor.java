package dev.ultreon.pythonc;

import org.objectweb.asm.MethodVisitor;

public interface JvmConstructor extends JvmFunction {

    void write(MethodVisitor mv, PythonCompiler compiler, Runnable paramInit);

    @Override
    default boolean isStatic() {
        return false;
    }

    @Override
    default JvmClass ownerClass(PythonCompiler compiler) {
        return owner(compiler);
    }
}
