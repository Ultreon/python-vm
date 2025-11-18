package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.scope.IfScope;
import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public class PyIfStmtNode implements PyCompoundStatementNode {
    private final PyExprNode condition;
    private final PyBlockNode body;
    private final PyElifStmtNode elifStmt;
    private final PyElseBlockNode elseBlock;

    public PyIfStmtNode(PyExprNode condition, PyBlockNode body, PyElifStmtNode elifStmt, PyElseBlockNode elseBlock) {
        this.condition = condition;
        this.body = body;
        this.elifStmt = elifStmt;
        this.elseBlock = elseBlock;
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        IfScope scope = new IfScope(context.getScope());
        context.pushScope(scope);
        LabelNode nextLabel = this.elifStmt == null ? context.getScope(IfScope.class).getExitLabel() : this.elifStmt.getConditionLabel();
        this.condition.compile(method, context);
        context.call();
        context.getAttribute("__bool__");
        context.call();
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "python/builtins/Bool", "getValue", "()Z", false));
        method.instructions.add(new JumpInsnNode(Opcodes.IFEQ, nextLabel));
        this.body.compile(method, context);
        method.instructions.add(new JumpInsnNode(Opcodes.GOTO, context.getScope(IfScope.class).getExitLabel()));
        if (this.elifStmt != null) {
            this.elifStmt.compile(method, context);
            method.instructions.add(new JumpInsnNode(Opcodes.GOTO, context.getScope(IfScope.class).getExitLabel()));
        }
        if (this.elseBlock != null) {
            this.elseBlock.compile(method, context);
        }
        method.instructions.add(context.getScope(IfScope.class).getExitLabel());
        context.popScope();
    }
}
