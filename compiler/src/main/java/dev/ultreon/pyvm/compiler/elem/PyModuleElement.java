package dev.ultreon.pyvm.compiler.elem;

import org.objectweb.asm.Type;

public class PyModuleElement implements PyElement {
    public PyModuleElement(String name) {

    }

    @Override
    public Type resolveType() {
        return Type.getObjectType("python/types/ModuleType");
    }
}
