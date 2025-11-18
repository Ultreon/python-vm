package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Type;

public interface PyNode {
    default Type getResolvedType(PyCompileContext context) {
        return Type.VOID_TYPE;
    }
}
