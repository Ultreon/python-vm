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

    default void expectReturnType(PythonCompiler compiler, Type expectedReturnType, Location location) {
        if (PythonCompiler.classCache.require(compiler, expectedReturnType).doesInherit(compiler, returnClass(compiler))) {
            throw new CompilerException("Expected return type " + expectedReturnType + " but got " + returnType(compiler) + " at ", location);
        }
    }

    default boolean isAbstract() {
        throw new UnsupportedOperationException();
    }

    JvmClass ownerClass(PythonCompiler compiler);

    boolean isDynamicCall();
}
