package dev.ultreon.pythonc

import dev.ultreon.pythonc.expr.PyExpression
import dev.ultreon.pythonc.expr.Settable

class TupleExtractionExpr extends PyExpression implements Settable {
    final PyExpression[] targets

    TupleExtractionExpr(PyExpression[] targets, Location location) {
        super(location)

        this.targets = targets
    }

    @Override
    void writeCode(PythonCompiler compiler, JvmWriter writer) {
        throw new TODO(location.formattedText)
    }

    @Override
    void set(PythonCompiler compiler, JvmWriter writer, PyExpression expr) {
        def size = writer.context.stackSize
        expr.write(compiler, writer)
        if (size + 1 != writer.context.stackSize) {
            throw new CompilerException("Expression provided no value (" + size + " != " + writer.context.stackSize + ")", location)
        }
        writer.cast Object
        writer.dynamicTuple(targets.length)
        if (size + 1 != writer.context.stackSize) {
            throw new RuntimeException("Invalid stack size after tuple creation (" + (size + 1) + " != " + writer.context.stackSize + "):\n" + location.formattedText)
        }

        for (int i = 0; i < targets.length; i++) {
            writer.dup()
            writer.cast Object[]
            def expression = targets[i]
            if (expression instanceof Settable) {
                ((Settable) expression).set(compiler, writer, new ExtractedTupleElementExpr(i, location))
            }
        }

        writer.pop()
        if (size != writer.context.stackSize) {
            throw new RuntimeException("Invalid stack size after tuple extraction (" + size + " != " + writer.context.stackSize + "):\n" + location.formattedText)
        }
    }

    @Override
    String getName() {
        throw new TODO(location.formattedText)
    }

    static class ExtractedTupleElementExpr extends PyExpression {
        final int index

        ExtractedTupleElementExpr(int index, Location location) {
            super(location)
            this.index = index
        }

        @Override
        void writeCode(PythonCompiler compiler, JvmWriter writer) {
            writer.getArrayElement(index)
        }
    }
}
