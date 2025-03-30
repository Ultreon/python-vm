package dev.ultreon.pythonc

import org.objectweb.asm.Label

class WhileLoopContext extends AbstractContext implements LoopContext {
    public final Label startLabel
    public final Label endLabel

    WhileLoopContext(Label startLabel, Label endLabel) {
        this.startLabel = startLabel
        this.endLabel = endLabel
    }

    @Override
    Label getContinuationLabel() {
        return startLabel
    }

    @Override
    Label getBreakLabel() {
        return endLabel
    }
}
