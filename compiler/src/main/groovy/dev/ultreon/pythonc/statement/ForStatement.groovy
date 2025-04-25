package dev.ultreon.pythonc.statement

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler
import dev.ultreon.pythonc.WhileLoopContext
import dev.ultreon.pythonc.expr.PyExpression
import dev.ultreon.pythonc.expr.Settable
import org.jetbrains.annotations.Nullable
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes

class ForStatement implements PyStatement {
    private final PyExpression iterable
    private final Settable variable
    private final PyBlock content
    private final @Nullable PyBlock elseBlock
    private final Location location

    ForStatement(PyExpression iterable, Settable variable, PyBlock content, Location location) {
        this(iterable, variable, content, null, location)
    }

    ForStatement(PyExpression iterable, Settable variable, PyBlock content, @Nullable PyBlock elseBlock, Location location) {
        this.iterable = iterable
        this.variable = variable
        this.content = content
        this.elseBlock = elseBlock
        this.location = location
    }

    PyExpression iterable() {
        return iterable
    }

    Settable variable() {
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
            Label nextLabel = new Label();
            Label nextCatchLabel = new Label();
            Label endCatchLabel = new Label();
            writer.mv().visitTryCatchBlock(nextLabel, endCatchLabel, nextCatchLabel, "org/python/builtins/StopIteration")
            writer.mv().visitLabel(nextLabel)
            writer.mv().visitInsn(Opcodes.DUP)
            writer.hiddenInvokeDynamic("__next__", "(Ljava/lang/Object;)Ljava/lang/Object;")
            writer.mv().visitVarInsn(Opcodes.ASTORE, variable.index)
            writer.mv().visitJumpInsn(Opcodes.GOTO, endCatchLabel)

            writer.mv().visitLabel(nextCatchLabel)
            writer.mv().visitJumpInsn(Opcodes.GOTO, endLabel)
            writer.mv().visitLabel(endCatchLabel)

            content.write(compiler, writer)
            compiler.checkPop(content.location)
            writer.jump(startLabel)
        } else {
            Label elseLabel = new Label()
            Label nextLabel = new Label();
            Label nextCatchLabel = new Label();
            Label endCatchLabel = new Label();
            writer.mv().visitTryCatchBlock(nextLabel, endCatchLabel, nextCatchLabel, "org/python/builtins/StopIteration")
            writer.mv().visitLabel(nextLabel)
            writer.mv().visitInsn(Opcodes.DUP)
            writer.hiddenInvokeDynamic("__next__", "(Ljava/lang/Object;)Ljava/lang/Object;")
            writer.mv().visitVarInsn(Opcodes.ASTORE, variable.index)
            writer.mv().visitJumpInsn(Opcodes.GOTO, endCatchLabel)

            writer.mv().visitLabel(nextCatchLabel)
            writer.mv().visitJumpInsn(Opcodes.GOTO, elseLabel)
            writer.mv().visitLabel(endCatchLabel)

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
        return location
    }
}
