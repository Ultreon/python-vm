package dev.ultreon.pythonc.statement

import org.objectweb.asm.Label
import org.objectweb.asm.Type

class CatchBlock {
    private final Type exceptionType
    private final PyBlock block
    private final Label startLabel = new Label()
    private final Label endLabel = new Label()
    private final String variableName

    CatchBlock(Type exceptionType, PyBlock block, String variableName = null) {
        this.exceptionType = exceptionType
        this.block = block
        this.variableName = variableName
    }

    Type getExceptionType() {
        return exceptionType
    }

    PyBlock getBlock() {
        return block
    }

    Label getStartLabel() {
        return startLabel
    }

    Label getEndLabel() {
        return endLabel
    }

    String getVariableName() {
        return variableName
    }
}
