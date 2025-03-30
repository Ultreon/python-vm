package dev.ultreon.pythonc

import org.objectweb.asm.Label

class ForLoopContext extends AbstractContext implements LoopContext {
    def startLabel
    def endLabel

    ForLoopContext(Label startLabel, Label endLabel) {
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
