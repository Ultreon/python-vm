package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.scope.IfScope;
import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class PyElifStmtNode implements PyCompoundStatementNode {
    private final PyExprNode condition;
    private final PyBlockNode block;
    private final PyElifStmtNode next;
    private final PyElseBlockNode elseBlock;
    private final LabelNode conditionLabel = new LabelNode();

    public PyElifStmtNode(PyExprNode condition, PyBlockNode block, PyElifStmtNode next, PyElseBlockNode elseBlock) {
        this.condition = condition;
        this.block = block;
        this.next = next;
        this.elseBlock = elseBlock;
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        PyCompoundStatementNode nextBlock = next == null ? elseBlock : next;
        LabelNode exitLabel = context.getScope(IfScope.class).getExitLabel();
        LabelNode nextLabel = nextBlock == null ? exitLabel : new LabelNode();
        method.instructions.add(conditionLabel);
        this.condition.compile(method, context);

        context.getAttribute("__bool__");
        context.call();
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "python/builtins/Bool", "getValue", "()Z", false));
        method.instructions.add(new JumpInsnNode(Opcodes.IFEQ, nextLabel));

        this.block.compile(method, context);
        method.instructions.add(new JumpInsnNode(Opcodes.GOTO, exitLabel));
        if (nextBlock != null) {
            method.instructions.add(nextLabel);
            nextBlock.compile(method, context);
            method.instructions.add(new JumpInsnNode(Opcodes.GOTO, exitLabel));
        }
    }

    public LabelNode getConditionLabel() {
        return conditionLabel;
    }
}
