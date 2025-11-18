package dev.ultreon.pyvm.compiler.elem;

import org.objectweb.asm.Type;

public interface PyElement {
    Type resolveType();
}
