package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

public class PyBitwiseXorNode implements PyExprNode {
    public final PyExprNode left;
    public final PyExprNode right;

    public PyBitwiseXorNode(PyExprNode left, PyExprNode right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public Type getResolvedType(PyCompileContext context) {
        return Type.getType(Object.class);
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        this.left.compile(method, context);
        context.getAttribute("__xor__");
        context.call(this.right);
    }
}
