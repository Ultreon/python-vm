package dev.ultreon.pyvm.compiler.scope;

import org.objectweb.asm.tree.LabelNode;

public class ForScope implements LoopScope {
    private Scope parent;
    private LabelNode continueLabel;
    private LabelNode exitLabel;

    public ForScope(Scope parent) {
        this(parent, new LabelNode(), new LabelNode());
    }

    public ForScope(Scope parent, LabelNode continueLabel, LabelNode exitLabel) {
        this.parent = parent;
        this.continueLabel = continueLabel;
        this.exitLabel = exitLabel;
    }

    @Override
    public LabelNode getContinueLabel() {
        return continueLabel;
    }

    @Override
    public Scope getParent() {
        return parent;
    }

    @Override
    public LabelNode getExitLabel() {
        return exitLabel;
    }
}
