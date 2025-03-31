package dev.ultreon.pythonc.statement

import dev.ultreon.pythonc.*

import java.util.concurrent.atomic.AtomicBoolean

class BreakStatement implements PyStatement {
    Location location

    BreakStatement(Location location) {
        this.location = location
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

        writer.jump context.breakLabel
    }
}
