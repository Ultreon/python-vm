package dev.ultreon.pythonc;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

public interface JvmClass extends Symbol {
    @Nullable JvmField field(PythonCompiler compiler, String name);

    @Nullable JvmFunction function(PythonCompiler compiler, String name, Type[] paramTypes);

    String className();

    boolean isInterface();

    boolean isAbstract();

    boolean isEnum();

    boolean doesInherit(PythonCompiler compiler, Type type);

    boolean isPrimitive();

    JvmFunction constructor(PythonCompiler compiler, Type[] paramTypes);
}
