package dev.ultreon.pythonc

import dev.ultreon.pythonc.statement.PyStatement
import org.objectweb.asm.Label

class JumpStatement extends PyStatement {
    private final Label toLabel

    JumpStatement(Label toLabel) {
        super()
        this.toLabel = toLabel
    }

    Label getToLabel() {
        return toLabel
    }

    @Override
    void writeStatement(PythonCompiler compiler, JvmWriter writer) {
        writer.jump(toLabel)
    }

    @Override
    Location getLocation() {
        return null
    }
}
