package dev.ultreon.pythonc;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public interface JvmClass extends Symbol {
    @Nullable JvmField field(PythonCompiler compiler, String name);

    @Nullable JvmFunction function(PythonCompiler compiler, String name, Type[] paramTypes);

    String className();

    boolean isInterface();

    boolean isAbstract();

    boolean isEnum();

    boolean doesInherit(PythonCompiler compiler, Type type);

    boolean isPrimitive();

    @Nullable JvmConstructor constructor(PythonCompiler compiler, Type[] paramTypes);

    default boolean doesInherit(PythonCompiler compiler, Class<?> type) {
        return doesInherit(compiler, Type.getType(type));
    }

    JvmClass superClass(PythonCompiler compiler);

    JvmClass[] interfaces(PythonCompiler compiler);

    Map<String, List<JvmFunction>> methods(PythonCompiler compiler);

    JvmConstructor[] constructors(PythonCompiler compiler);

    default boolean doesInherit(PythonCompiler compiler, JvmClass checkAgainst) {
        if (this.type(compiler).equals(Type.getType(Object.class))) return true;
        if (this.equals(checkAgainst)) return true;
        JvmClass superClass = superClass(compiler);
        if (superClass == null) return false;
        if (superClass.equals(checkAgainst)) return true;
        for (JvmClass anInterface : interfaces(compiler)) {
            if (anInterface == null) continue;
            if (anInterface.equals(checkAgainst)) return true;
            if (anInterface.doesInherit(compiler, checkAgainst)) return true;
        }
        return superClass.doesInherit(compiler, checkAgainst);
    }

    boolean isArray();
}
