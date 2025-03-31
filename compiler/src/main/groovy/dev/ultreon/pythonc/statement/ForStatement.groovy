package dev.ultreon.pythonc.statement

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler
import dev.ultreon.pythonc.WhileLoopContext
import dev.ultreon.pythonc.expr.PyExpression
import dev.ultreon.pythonc.expr.VariableExpr
import org.jetbrains.annotations.Nullable
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

class ForStatement implements PyStatement {
    private final PyExpression iterable
    private final VariableExpr variable
    private final PyBlock content
    private final @Nullable PyBlock elseBlock

    ForStatement(PyExpression iterable, VariableExpr variable, PyBlock content) {
        this(iterable, variable, content, null)
    }

    ForStatement(PyExpression iterable, VariableExpr variable, PyBlock content, @Nullable PyBlock elseBlock) {
        this.iterable = iterable
        this.variable = variable
        this.content = content
        this.elseBlock = elseBlock
    }

    PyExpression iterable() {
        return iterable
    }

    VariableExpr variable() {
        return variable
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

        iterable.write(compiler, writer)
        writer.context.pop()

        // Secretly have an iterator in the code (don't leak pls)
        writer.hiddenInvokeDynamic("__iter__", "(Ljava/lang/Object;)Ljava/lang/Object;")

        writer.label(startLabel)

        if (compiler.getSymbolToSet(variable.name) == null)
            compiler.setSymbol(variable.name, variable)

        if (elseBlock == null) {
            writer.mv().visitInsn(Opcodes.DUP)
            writer.hiddenInvokeDynamic("__hasnext__", "(Ljava/lang/Object;)Z")
            writer.mv().visitJumpInsn(Opcodes.IFEQ, endLabel)

            writer.mv().visitInsn(Opcodes.DUP)
            writer.hiddenInvokeDynamic("__next__", "(Ljava/lang/Object;)Ljava/lang/Object;")
            writer.mv().visitVarInsn(Opcodes.ASTORE, variable.index)

            content.write(compiler, writer)
            compiler.checkPop(content.location)
            writer.jump(startLabel)
        } else {
            Label elseLabel = new Label()
            writer.mv().visitInsn(Opcodes.DUP)
            writer.hiddenInvokeDynamic("__hasnext__", "(Ljava/lang/Object;)Z")
            writer.mv().visitJumpInsn(Opcodes.IFEQ, elseLabel)

            writer.mv().visitInsn(Opcodes.DUP)
            writer.hiddenInvokeDynamic("__next__", "(Ljava/lang/Object;)Ljava/lang/Object;")
            writer.mv().visitVarInsn(Opcodes.ASTORE, variable.index)

            content.write(compiler, writer)
            compiler.checkPop(content.location)
            writer.jump(startLabel)
            writer.label(elseLabel)
            elseBlock.write(compiler, writer)
            compiler.checkPop(elseBlock.location)
            writer.jump(endLabel)
        }

        writer.jump(startLabel)
        writer.label(endLabel)
        writer.mv().visitInsn(Opcodes.POP)
    }

    @Override
    Location getLocation() {
        return null
    }
}
