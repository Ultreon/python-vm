package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Type;

public interface PyParamNode extends PyNode {
    String getName();

    Type getResolvedType(PyCompileContext context);
}
