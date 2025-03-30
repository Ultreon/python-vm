package dev.ultreon.pythonc.statement

import dev.ultreon.pythonc.*

import java.util.concurrent.atomic.AtomicBoolean

class BreakStatement extends PyStatement {
    BreakStatement() {
    }

    @Override
    void writeStatement(PythonCompiler compiler, JvmWriter writer) {
        def outsideFunction = new AtomicBoolean()
        def outsideClass = new AtomicBoolean()
        LoopContext context = compiler.lastContext(ctx -> {
            if (ctx instanceof FunctionContext) {
                outsideFunction.set true
                return false
            }
            if (ctx instanceof FunctionDefiningContext) {
                outsideClass.set true
                return false
            }

            return ctx instanceof LoopContext
        }) as LoopContext

        if (outsideFunction.get()) throw new CompilerException("Can't break a loop though a function!", location)
        if (outsideClass.get()) throw new CompilerException("Can't break a loop though a class!", location)

        writer.label context.breakLabel
    }

    @Override
    Location getLocation() {
        return null
    }
}
