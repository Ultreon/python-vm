package dev.ultreon.pythonc.statement

import dev.ultreon.pythonc.*
import dev.ultreon.pythonc.expr.PyExpression
import org.jetbrains.annotations.Nullable
import org.objectweb.asm.Label
import org.objectweb.asm.Type

class WhileStatement implements PyStatement {
    private final PyExpression condition
    private final PyBlock content
    private final @Nullable PyBlock elseBlock

    WhileStatement(PyExpression condition, PyBlock content, Location location) {
        this(condition, content, null, location)
    }

    WhileStatement(PyExpression condition, PyBlock content, @Nullable PyBlock elseBlock, Location location) {
        this.condition = condition
        this.content = content
        this.elseBlock = elseBlock
        this.location = location
    }

    PyExpression condition() {
        return condition
    }

    PyBlock content() {
        return content
    }

    PyBlock elseBlock() {
        return elseBlock
    }

    @Override
    void writeStatement(PythonCompiler compiler, JvmWriter writer) {
        Label startLabel = new Label()
        Label endLabel = new Label()

        compiler.pushContext(new WhileLoopContext(startLabel, endLabel))

        writer.label(startLabel)

        Type conditionType = condition.write(compiler, writer)
        if (conditionType != Type.BOOLEAN_TYPE) {
            writer.cast(Type.BOOLEAN_TYPE)
        }

        if (elseBlock == null) {
            writer.jumpIfNotEqual(endLabel)
            content.write(compiler, writer)
            compiler.checkPop(location)
            writer.jump(startLabel)
        } else {
            Label elseLabel = new Label()
            writer.jumpIfNotEqual(elseLabel)
            content.write(compiler, writer)
            compiler.checkPop(location)
            writer.jump(startLabel)
            writer.label(elseLabel)
            elseBlock.write(compiler, writer)
            compiler.checkPop(location)
        }
        writer.label(endLabel)
    }

    Location location

    String toString() {
        StringBuilder builder = new StringBuilder()

        builder.append(Location.ANSI_RED).append("While ").append(Location.ANSI_RESET).append("(").append(condition).append(") {\n")
        builder.append("  ").append(content.toString().replace("\n", "\n  ")).append("\n")
        builder.append(Location.ANSI_RESET).append("}")
        return builder.toString()
    }
}
