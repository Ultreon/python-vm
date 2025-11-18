package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

public class PyNamedExpressionNode implements PyExprNode {
    private PyExprNode expr;

    public PyNamedExpressionNode(PyExprNode expr) {
        this.expr = expr;
    }

    @Override
    public Type getResolvedType(PyCompileContext context) {
        return expr.getResolvedType(context);
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        this.expr.compile(method, context);
    }
}
