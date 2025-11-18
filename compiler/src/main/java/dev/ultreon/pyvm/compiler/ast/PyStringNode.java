package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Type;

public interface PyStringNode extends PyExprNode {
    @Override
    default Type getResolvedType(PyCompileContext context) {
        return Type.getObjectType("python/lang/Str");
    }
}
