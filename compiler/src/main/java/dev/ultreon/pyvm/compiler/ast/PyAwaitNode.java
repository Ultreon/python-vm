package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

public class PyAwaitNode implements PyExprNode {
    private final PyExprNode expr;

    public PyAwaitNode(PyExprNode expr) {
        this.expr = expr;
    }

    @Override
    public Type getResolvedType(PyCompileContext context) {
        return Type.getType(Object.class);
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        this.expr.compile(method, context);
        context.getAttribute("__await__");
        context.call();
    }
}
