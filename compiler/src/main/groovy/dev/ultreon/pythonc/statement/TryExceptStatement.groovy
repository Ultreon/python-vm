package dev.ultreon.pythonc.statement

import dev.ultreon.pythonc.*
import dev.ultreon.pythonc.expr.VariableExpr
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes

import static org.objectweb.asm.Type.getType

class TryExceptStatement implements PyStatement {
    private final PyBlock content
    private final PyBlock finallyBlock
    private final PyBlock elseBlock
    private final CatchBlock[] catchBlock
    private final Location location

    TryExceptStatement(PyBlock content, PyBlock finallyBlock = null, PyBlock elseBlock = null, CatchBlock[] catchBlock, Location location) {
        this.content = content
        this.finallyBlock = finallyBlock
        this.elseBlock = elseBlock
        this.catchBlock = catchBlock
        this.location = location
    }

    @Override
    void writeStatement(PythonCompiler compiler, JvmWriter writer) {
        Label startLabel = writer.newLabel()
        Label endLabel = writer.newLabel()
        Label finalLabel = finallyBlock == null ? endLabel : writer.newLabel()
        Label finalExceptionLabel = finallyBlock == null ? null : writer.newLabel()
        Label elseLabel = elseBlock == null ? null : writer.newLabel()

        // Register all catch blocks
        for (CatchBlock catchBlock : catchBlock) {
            def require = PythonCompiler.classCache.require(catchBlock.exceptionType)
            if (!require.doesInherit(compiler, PythonCompiler.classCache.require(compiler, getType(Throwable)))) {
                throw new CompilerException("Exception type ${catchBlock.exceptionType.className} is not a subclass of java.lang.Throwable", catchBlock.block.location)
            }
            writer.mv().visitTryCatchBlock(startLabel, endLabel, catchBlock.startLabel, catchBlock.exceptionType.internalName)
        }

        // Register else/finally handlers
        if (elseBlock != null) {
            writer.mv().visitTryCatchBlock(startLabel, endLabel, elseLabel, "java/lang/Throwable")
        } else if (finallyBlock != null) {
            writer.mv().visitTryCatchBlock(startLabel, endLabel, finalExceptionLabel, null)
        }

        // Try block
        writer.label(startLabel)
        content.write(compiler, writer)
        compiler.checkPop(content.location)
        writer.mv().visitJumpInsn(Opcodes.GOTO, finallyBlock != null ? finalLabel : endLabel)

        // Catch blocks
        for (CatchBlock catchBlock : catchBlock) {
            writer.label(catchBlock.startLabel)
            if (catchBlock.variableName != null) {
                def varIdx = compiler.currentVariableIndex++
                def set = SymbolContext.current().getSymbolToSet(catchBlock.variableName)
                if (set == null) {
                    compiler.setSymbol(catchBlock.variableName, new VariableExpr(varIdx, catchBlock.variableName, catchBlock.block.location))
                }
                writer.mv().visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Object")
                writer.mv().visitVarInsn(Opcodes.ASTORE, varIdx)
                writer.mv().visitLocalVariable(catchBlock.variableName, catchBlock.exceptionType.descriptor, null, catchBlock.startLabel, catchBlock.endLabel, varIdx)
            } else {
                writer.mv().visitInsn(Opcodes.POP)
            }

            catchBlock.block.write(compiler, writer)
            compiler.checkPop(content.location)
            writer.mv().visitJumpInsn(Opcodes.GOTO, finallyBlock != null ? finalLabel : endLabel)
            writer.label(catchBlock.endLabel)
        }

        // Else block
        if (elseBlock != null) {
            writer.label(elseLabel)
            writer.mv().visitInsn(Opcodes.POP)  // Pop the exception
            elseBlock.write(compiler, writer)
            compiler.checkPop(content.location)
            writer.mv().visitJumpInsn(Opcodes.GOTO, finallyBlock != null ? finalLabel : endLabel)
        }

        // Finally block
        if (finallyBlock != null) {
            writer.label(finalExceptionLabel)
            writer.mv().visitInsn(Opcodes.POP)  // Pop the exception
            writer.label(finalLabel)
            finallyBlock.write(compiler, writer)
            compiler.checkPop(content.location)
            writer.mv().visitJumpInsn(Opcodes.GOTO, endLabel)
        }

        writer.label(endLabel)
    }

    @Override
    Location getLocation() {
        return location
    }
}