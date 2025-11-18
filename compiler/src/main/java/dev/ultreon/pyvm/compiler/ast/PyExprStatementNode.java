package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

public class PyExprStatementNode implements PyStatementNode {
    private final PyExprNode expr;

    public PyExprStatementNode(PyExprNode expr) {
        this.expr = expr;
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        this.expr.compile(method, context);
        method.visitInsn(Opcodes.POP);
    }
}
