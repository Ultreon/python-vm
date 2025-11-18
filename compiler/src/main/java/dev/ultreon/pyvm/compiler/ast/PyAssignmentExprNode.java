package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

public class PyAssignmentExprNode implements PyExprNode {
    private final PyTargetNode target;
    private final PyExprNode value;

    public PyAssignmentExprNode(PyTargetNode target, PyExprNode value) {
        this.target = target;
        this.value = value;
    }

    @Override
    public Type getResolvedType(PyCompileContext context) {
        return this.value.getResolvedType(context);
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        this.target.compileSet(method, context, this.value);
    }
}
