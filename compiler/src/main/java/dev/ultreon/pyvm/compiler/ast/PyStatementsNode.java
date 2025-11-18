package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

public class PyStatementsNode implements PyStatementNode {
    private final List<PyStatementNode> statements;

    public PyStatementsNode(List<PyStatementNode> statements) {
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
        return this.statements.toArray(new PyStatementNode[0]);
    }

}
