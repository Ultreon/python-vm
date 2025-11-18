package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

public class PyIsNotNode implements PyComparisionNode {
    private PyExprNode left;
    private final PyExprNode right;

    public PyIsNotNode(PyExprNode left, PyExprNode right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        this.left.compile(method, context);
        this.right.compile(method, context);
        method.visitMethodInsn(Opcodes.INVOKESTATIC, "python/_core/Py", "isNot", "(Ljava/lang/Object;Ljava/lang/Object;)Z", false);
    }

    @Override
    public void setLeft(PyExprNode expr) {
        this.left = expr;
    }
}
