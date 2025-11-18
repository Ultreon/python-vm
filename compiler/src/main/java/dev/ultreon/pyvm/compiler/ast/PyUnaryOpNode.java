package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

public class PyUnaryOpNode implements PyExprNode {
    public final Op op;
    public final PyExprNode left;

    public PyUnaryOpNode(Op op, PyExprNode left) {
        this.op = op;
        this.left = left;
    }

    @Override
    public Type getResolvedType(PyCompileContext context) {
        return Type.getType(Object.class);
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        this.left.compile(method, context);
        switch (this.op) {
            case UNot:
                context.getAttribute("__not__");
                break;
            case UAdd:
                context.getAttribute("__pos__");
                break;
            case USub:
                context.getAttribute("__neg__");
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + this.op);
        }
    }

    public enum Op {
        UNot, UAdd, USub;
    }
}
