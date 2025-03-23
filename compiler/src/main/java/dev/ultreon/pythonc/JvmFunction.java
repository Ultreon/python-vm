package dev.ultreon.pythonc;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public interface JvmFunction extends JvmCallable {
    void invoke(Object callArgs, Runnable paramInit);

    Type returnType(PythonCompiler compiler);

    JvmClass returnClass(PythonCompiler compiler);

    Type[] parameterTypes(PythonCompiler compiler);

    JvmClass[] parameterClasses(PythonCompiler compiler);

    default void write(MethodVisitor mv, PythonCompiler compiler) {
        load(mv, compiler, preload(mv, compiler, false), false);
    }

    default int parameterCount(PythonCompiler compiler) {
        return parameterTypes(compiler).length;
    }

    boolean isStatic();

    JvmClass ownerClass(PythonCompiler compiler);
}
