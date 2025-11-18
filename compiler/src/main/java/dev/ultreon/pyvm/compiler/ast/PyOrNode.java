package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import dev.ultreon.pyvm.compiler.ast.util.ToBooleanNode;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

public class PyOrNode implements PyExprNode {
    public final PyExprNode left;
    public final PyExprNode right;

    public PyOrNode(PyExprNode left, PyExprNode right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public Type getResolvedType(PyCompileContext context) {
        return Type.BOOLEAN_TYPE;
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        this.left.compile(method, context);
        context.getAttribute("__bool__");
        context.call();
        context.getAttribute("__or__");
        context.call(new ToBooleanNode(this.right));
        context.getAttribute("__bool__");
        context.call();
    }
}
