package dev.ultreon.pyvm.compiler.elem;

import org.objectweb.asm.Type;

public class PyClassElement implements PyElement {
    private final String name;

    public PyClassElement(String name) {
        this.name = name;
    }

    @Override
    public Type resolveType() {
        return Type.getObjectType("python/types/Type");
    }

    public String getName() {
        return name;
    }
}
