package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

public class PyShiftExprNode implements PyExprNode {
    public final PyExprNode left;
    public final Dir dir;
    public final PyExprNode right;

    public PyShiftExprNode(PyExprNode left, Dir dir, PyExprNode right) {
        this.left = left;
        this.dir = dir;
        this.right = right;
    }

    @Override
    public Type getResolvedType(PyCompileContext context) {
        return Type.getType(Object.class);
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        this.left.compile(method, context);
        switch (this.dir) {
            case Left:
                context.getAttribute("__lshift__");
                break;
            case Right:
                context.getAttribute("__rshift__");
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + this.dir);
        }
        context.call(this.right);
    }

    public enum Dir {
        Left, Right;
    }
}
