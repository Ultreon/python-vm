package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

public class PyReturnNode implements PyStatementNode {
    private final PyExprNode expr;

    public PyReturnNode(PyExprNode expr) {
        this.expr = expr;
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        this.expr.compile(method, context);
        method.visitInsn(Opcodes.ARETURN);
    }
}
