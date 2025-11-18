package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

public class PyTermNode implements PyExprNode {
    public final PyExprNode left;
    public final Op op;
    public final PyExprNode right;

    public PyTermNode(PyExprNode left, Op op, PyExprNode right) {
        this.left = left;
        this.op = op;
        this.right = right;
    }

    @Override
    public Type getResolvedType(PyCompileContext context) {
        return Type.getType(Object.class);
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        this.left.compile(method, context);
        switch (this.op) {
            case Mult:
                context.getAttribute("__mul__");
                break;
            case Div:
                context.getAttribute("__div__");
                break;
            case FloorDiv:
                context.getAttribute("__floordiv__");
                break;
            case Mod:
                context.getAttribute("__mod__");
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + this.op);
        }
        context.call(this.right);
    }

    public enum Op {
        Mult, Div, Mod, FloorDiv
    }
}
