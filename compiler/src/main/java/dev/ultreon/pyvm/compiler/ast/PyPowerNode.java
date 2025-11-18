package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

public class PyPowerNode implements PyExprNode {
    public final PyExprNode left;
    public final PyExprNode right;

    public PyPowerNode(PyExprNode left, PyExprNode right) {
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
        context.getAttribute("__pow__");
        context.call(this.right);
    }

    public enum Op {
        Mult, Div, Mod, FloorDiv
    }
}
