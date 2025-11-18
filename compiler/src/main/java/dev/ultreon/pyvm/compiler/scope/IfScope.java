package dev.ultreon.pyvm.compiler.scope;

import org.objectweb.asm.tree.LabelNode;

public class IfScope implements Scope {
    private final Scope parent;
    private final LabelNode exitLabel = new LabelNode();
    private LabelNode nextLabel = new LabelNode();

    public IfScope(Scope parent) {
        this.parent = parent;
    }

    public LabelNode getExitLabel() {
        return exitLabel;
    }

    public LabelNode getNextLabel() {
        return nextLabel;
    }

    public void setNextLabel(LabelNode nextLabel) {
        this.nextLabel = nextLabel;
    }

    @Override
    public Scope getParent() {
        return this.parent;
    }
}
