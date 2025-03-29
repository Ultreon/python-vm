package dev.ultreon.pythonc;

import dev.ultreon.pythonc.classes.JvmClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

public record TypedName(@NotNull String name, @Nullable Type type) {

    public JvmClass typeClass(PythonCompiler compiler) {
        return PythonCompiler.classCache.require(compiler, type == null ? Type.getType(Object.class) : type);
    }
}
