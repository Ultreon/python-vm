package dev.ultreon.pyvm.compiler.util;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

public class Variable {
    private final String name;
    private final int slot;
    private AbstractInsnNode lastInteraction;
    private final LabelNode startLabel;
    private final LabelNode endLabel;

    public Variable(String name, int slot) {
        this.name = name;
        this.slot = slot;
        this.lastInteraction = null;
        this.startLabel = new LabelNode();
        this.endLabel = new LabelNode();
    }

    public String getName() {
        return this.name;
    }

    public int getSlot() {
        return this.slot;
    }

    public AbstractInsnNode getLastInteraction() {
        return this.lastInteraction;
    }

    public void interact(AbstractInsnNode insn) {
        this.lastInteraction = insn;
    }

    public LabelNode getStartLabel() {
        return this.startLabel;
    }

    public LabelNode getEndLabel() {
        return this.endLabel;
    }

    public void finish(MethodNode method) {
        method.instructions.insert(lastInteraction, this.endLabel);
        method.localVariables.add(new LocalVariableNode(this.name, "Ljava/lang/Object;", null, this.startLabel, this.endLabel, this.slot));
    }
}
