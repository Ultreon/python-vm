package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.tree.MethodNode;

public class PyElseBlockNode implements PyCompoundStatementNode {
    private final PyStatementNode[] statements;

    public PyElseBlockNode(PyStatementNode[] statements) {
        this.statements = statements;
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        for (PyStatementNode statement : this.statements) {
            if (statement == null) {
                continue;
            }
            statement.compile(method, context);
        }
    }
}
