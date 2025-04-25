package dev.ultreon.pythonc.statement

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler
import dev.ultreon.pythonc.SymbolContext
import dev.ultreon.pythonc.expr.PySymbol
import dev.ultreon.pythonc.expr.VariableExpr
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes

class TryExceptStatement implements PyStatement {
    private final PyBlock content
    private final PyBlock finallyBlock
    private final PyBlock elseBlock
    private final CatchBlock[] catchBlock

    TryExceptStatement(PyBlock content, PyBlock finallyBlock = null, PyBlock elseBlock = null, CatchBlock... catchBlock) {
        this.content = content
        this.finallyBlock = finallyBlock
        this.elseBlock = elseBlock
        this.catchBlock = catchBlock
    }

    @Override
    void writeStatement(PythonCompiler compiler, JvmWriter writer) {
        Label startLabel = writer.newLabel()
        Label endLabel = writer.newLabel()
        Label finalLabel = finallyBlock == null ? endLabel : writer.newLabel()
        Label finalExceptionLabel = finallyBlock == null ? null : writer.newLabel()
        Label elseLabel = elseBlock == null ? null : writer.newLabel()

        for (CatchBlock catchBlock : catchBlock) {
            writer.mv().visitTryCatchBlock(startLabel, endLabel, catchBlock.startLabel, catchBlock.exceptionType.internalName)
        }

        if (elseBlock != null) {
            writer.mv().visitTryCatchBlock(startLabel, endLabel, elseLabel, "java/lang/Throwable")
        } else if (finallyBlock != null) {
            writer.mv().visitTryCatchBlock(startLabel, endLabel, finalExceptionLabel, null)
        }

        for (CatchBlock catchBlock : catchBlock) {
            writer.label(catchBlock.startLabel)
            if (catchBlock.variableName != null) {
                def varIdx = compiler.currentVariableIndex++
                def set = SymbolContext.current().getSymbolToSet(catchBlock.variableName)
                if (set == null) {
                    compiler.setSymbol(catchBlock.variableName, new VariableExpr(varIdx, catchBlock.variableName, catchBlock.block.location))
                }
                writer.mv().visitVarInsn(Opcodes.ASTORE, varIdx)
                writer.mv().visitLocalVariable(catchBlock.variableName, catchBlock.exceptionType.descriptor, null, new Label(), new Label(), varIdx)
            } else {
                writer.mv().visitInsn(Opcodes.POP)
            }

            catchBlock.block.write(compiler, writer)
            writer.mv().visitJumpInsn(Opcodes.GOTO, finalLabel == null ? endLabel : finalLabel)
            writer.label(catchBlock.endLabel)

            if (finallyBlock != null) {
                writer.mv().visitJumpInsn(Opcodes.GOTO, finalLabel == null ? endLabel : finalLabel)
            }
        }

        if (elseBlock != null) {
            writer.label(elseLabel)
            elseBlock.write(compiler, writer)
            writer.mv().visitJumpInsn(Opcodes.GOTO, finalLabel == null ? endLabel : finalLabel)
        }

        if (finallyBlock != null) {
            writer.label(finalExceptionLabel)
            writer.mv().visitInsn(Opcodes.POP)
            writer.label(finalLabel)
            finallyBlock.write(compiler, writer)
            writer.mv().visitJumpInsn(Opcodes.GOTO, endLabel)
        }

        writer.label(startLabel)
        content.write(compiler, writer)
        compiler.checkPop(content.location)
        writer.jump(finalLabel)
        writer.label(endLabel)
        writer.mv().visitInsn(Opcodes.POP)
        writer.label(finalLabel)
    }

    @Override
    Location getLocation() {
        return null
    }
}
