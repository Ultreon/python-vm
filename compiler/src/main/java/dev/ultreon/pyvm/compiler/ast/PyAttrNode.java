package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

public class PyAttrNode implements PyExprNode {
    private final PyExprNode expr;
    private final String attr;

    public PyAttrNode(PyExprNode expr, String attr) {
        this.expr = expr;
        this.attr = attr;
    }

    @Override
    public Type getResolvedType(PyCompileContext context) {
        return Type.getType(Object.class);
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        this.expr.compile(method, context);
        context.getAttribute(this.attr);
    }
}
