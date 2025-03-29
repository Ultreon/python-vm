package dev.ultreon.pythonc;

import org.objectweb.asm.Label;

public class ForLoopContext extends AbstractContext implements LoopContext {
    private final Label startLabel;
    private final Label endLabel;

    public ForLoopContext(Label startLabel, Label endLabel) {
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
