package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

public class PyFStringMiddleReplacementNode implements PyFStringMiddleNode {
    private final PyExprNode expr;

    public PyFStringMiddleReplacementNode(PyExprNode expr) {
        this.expr = expr;
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        this.expr.compile(method, context);
        method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false);
    }

    public PyExprNode getExpr() {
        return expr;
    }
}
