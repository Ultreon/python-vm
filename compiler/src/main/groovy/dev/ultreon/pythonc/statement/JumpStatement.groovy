package dev.ultreon.pythonc.statement

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler
import org.objectweb.asm.Label

class JumpStatement implements PyStatement {
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
