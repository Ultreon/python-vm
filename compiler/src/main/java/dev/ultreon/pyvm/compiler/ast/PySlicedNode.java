package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

public class PySlicedNode implements PyExprNode {
    private final PyExprNode owner;
    private final PySliceNode slice;

    public PySlicedNode(PyExprNode owner, PySliceNode slice) {
        this.owner = owner;
        this.slice = slice;
    }

    @Override
    public Type getResolvedType(PyCompileContext context) {
        return Type.getType(Object.class);
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        this.owner.compile(method, context);
        context.getAttribute("__getitem__");
        context.call(slice);
    }
}
