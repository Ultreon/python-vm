package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Type;

public interface PyComparisionNode extends PyExprNode {
    @Override
    default Type getResolvedType(PyCompileContext context) {
        return Type.BOOLEAN_TYPE;
    }

    void setLeft(PyExprNode expr);
}
