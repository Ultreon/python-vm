package dev.ultreon.pyvm.compiler.elem;

import org.objectweb.asm.Type;

public class PyFunctionElement implements PyElement {

    @Override
    public Type resolveType() {
        return Type.getObjectType("python/types/FunctionType");
    }
}
