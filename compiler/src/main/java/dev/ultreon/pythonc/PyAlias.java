package dev.ultreon.pythonc;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

public record PyAlias(String name, @Nullable String alias) {
    public PyAlias(String name) {
        this(name, null);
    }

    public Type asType(ModulePath path) {
        return path.getClass(name).asType();
    }
}
