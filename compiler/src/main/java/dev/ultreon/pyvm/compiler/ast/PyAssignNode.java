package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import dev.ultreon.pyvm.compiler.util.PyType;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.MethodNode;

public class PyAssignNode implements PyStatementNode {
    private final PyTargetNode target;
    private final PyExprNode expr;
    private final @Nullable PyType type;

    public PyAssignNode(PyTargetNode target, @Nullable PyType type, PyExprNode expr) {
        this.target = target;
        this.type = type;
        this.expr = expr;
    }

    public PyAssignNode(PyTargetNode target, PyExprNode expr) {
        this(target, null, expr);
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        this.target.compileSet(method, context, this.expr);
    }

    public PyTargetNode getTarget() {
        return target;
    }

    public PyExprNode getExpr() {
        return expr;
    }

    public @Nullable PyType getType() {
        return type;
    }
}
