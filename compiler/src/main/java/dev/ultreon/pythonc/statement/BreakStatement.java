package dev.ultreon.pythonc.statement;

import dev.ultreon.pythonc.*;
import org.objectweb.asm.Label;

import java.util.concurrent.atomic.AtomicBoolean;

public class BreakStatement extends PyStatement {
    public BreakStatement() {
    }

    @Override
    public void writeStatement(PythonCompiler compiler, JvmWriter writer) {
        AtomicBoolean outsideFunction = new AtomicBoolean();
        AtomicBoolean outsideClass = new AtomicBoolean();
        LoopContext context = (LoopContext) compiler.lastContext((ctx) -> {
            if (ctx instanceof FunctionContext) {
                outsideFunction.set(true);
                return false;
            }
            if (ctx instanceof FunctionDefiningContext) {
                outsideClass.set(true);
                return false;
            }

            return ctx instanceof LoopContext;
        });

        if (outsideFunction.get()) throw new CompilerException("Can't break a loop though a function!", location());
        if (outsideClass.get()) throw new CompilerException("Can't break a loop though a class!", location());

        writer.label(context.getBreakLabel());
    }

    @Override
    public Location location() {
        return null;
    }
}
