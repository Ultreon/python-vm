package dev.ultreon.pythonc;

import org.objectweb.asm.Label;

public class WhileLoopContext extends AbstractContext implements LoopContext {
    public final Label startLabel;
    public final Label endLabel;

    public WhileLoopContext(Label startLabel, Label endLabel) {
        this.startLabel = startLabel;
        this.endLabel = endLabel;
    }

    @Override
    public Label getContinuationLabel() {
        return startLabel;
    }

    @Override
    public Label getBreakLabel() {
        return endLabel;
    }
}
