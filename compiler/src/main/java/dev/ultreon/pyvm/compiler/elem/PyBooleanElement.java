package dev.ultreon.pyvm.compiler.elem;

import org.objectweb.asm.Type;

public class PyBooleanElement implements PyElement {
    private final boolean isTrue;

    public PyBooleanElement(boolean isTrue) {
        this.isTrue = isTrue;
    }

    @Override
    public Type resolveType() {
        return Type.getObjectType("python/lang/Bool");
    }

    public boolean isTrue() {
        return isTrue;
    }
}
