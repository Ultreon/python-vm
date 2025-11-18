package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.tree.MethodNode;

public class PyBlockNode implements PyStatementNode {
    private final PyStatementNode[] statements;

    public PyBlockNode(PyStatementNode[] statements) {
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

    public PyStatementNode[] getStatements() {
        return this.statements;
    }
}
