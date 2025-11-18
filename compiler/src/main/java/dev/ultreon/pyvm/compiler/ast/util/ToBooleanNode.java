package dev.ultreon.pyvm.compiler.ast.util;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import dev.ultreon.pyvm.compiler.ast.PyExprNode;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

public class ToBooleanNode implements PyExprNode {
    private final PyExprNode expr;

    public ToBooleanNode(PyExprNode expr) {
        this.expr = expr;
    }

    @Override
    public Type getResolvedType(PyCompileContext context) {
        return Type.BOOLEAN_TYPE;
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        expr.compile(method, context);
        context.getAttribute("__bool__");
        context.call();
    }
}
