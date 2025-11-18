package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class PyEqualNode implements PyComparisionNode {
    private PyExprNode left;
    private final PyExprNode right;

    public PyEqualNode(PyExprNode left, PyExprNode right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        this.left.compile(method, context);
        this.right.compile(method, context);
//        method.visitMethodInsn(Opcodes.INVOKESTATIC, "python/_core/Py", "eq", "(Ljava/lang/Object;Ljava/lang/Object;)Z", false);
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "eq", "(Ljava/lang/Object;Ljava/lang/Object;)Z", false));
    }

    @Override
    public void setLeft(PyExprNode expr) {
        this.left = expr;
    }
}
