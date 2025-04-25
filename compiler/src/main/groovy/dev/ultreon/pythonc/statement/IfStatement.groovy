package dev.ultreon.pythonc.statement

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler
import dev.ultreon.pythonc.expr.PyExpression
import org.jetbrains.annotations.Nullable
import org.objectweb.asm.Label
import org.objectweb.asm.Type

class IfStatement implements PyStatement {
    private final PyExpression condition
    private final PyBlock trueBlock
    private final @Nullable PyBlock falseBlock
    private final Location location

    IfStatement(PyExpression condition, PyBlock trueBlock, @Nullable PyBlock falseBlock, Location location) {
        this.condition = condition
        this.trueBlock = trueBlock
        this.falseBlock = falseBlock
        this.location = location
    }

    PyExpression condition() {
        return condition
    }

    PyBlock trueBlock() {
        return trueBlock
    }

    PyBlock falseBlock() {
        return falseBlock
    }

    @Override
    void writeStatement(PythonCompiler compiler, JvmWriter writer) {
        Label endLabel = new Label()

        Type conditionType = condition.write(compiler, writer)
        if (conditionType != Type.BOOLEAN_TYPE) {
            writer.cast(Type.BOOLEAN_TYPE)
        }

        if (falseBlock == null) {
            writer.jumpIfNotEqual(endLabel)
            compiler.checkPop(condition.location)
            trueBlock.write(compiler, writer)
        } else {
            Label elseLabel = new Label()
            writer.jumpIfNotEqual(elseLabel)
            compiler.checkPop(condition.location)
            trueBlock.write(compiler, writer)
            writer.jump(endLabel)
            writer.label(elseLabel)
            falseBlock.write(compiler, writer)
        }

        writer.label(endLabel)
    }

    @Override
    Location getLocation() {
        return location
    }
}
