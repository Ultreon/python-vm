package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Type;

public interface PyExprNode extends PyMethodContentNode {
    @Override
    Type getResolvedType(PyCompileContext context);
}
