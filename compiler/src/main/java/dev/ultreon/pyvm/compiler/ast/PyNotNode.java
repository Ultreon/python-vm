package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

public class PyNotNode implements PyExprNode {
    public final PyExprNode left;

    public PyNotNode(PyExprNode left) {
        this.left = left;
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
        context.getAttribute("__not__");
        context.call();
    }
}
