package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

public class PyStarExpressionNode implements PyExprNode {
    private final PyExprNode expr;
    private final boolean starred;

    public PyStarExpressionNode(boolean starred, PyExprNode expr) {
        this.starred = starred;
        this.expr = expr;
    }

    @Override
    public Type getResolvedType(PyCompileContext context) {
        if (this.starred) {
            throw new UnsupportedOperationException("Starred expressions are not supported yet");
        }
        return expr.getResolvedType(context);
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        this.expr.compile(method, context);
    }
}
