package dev.ultreon.pythonc.expr

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler
import org.objectweb.asm.Type

class TupleExpr extends PyExpression {
    private final List<PyExpression> expressions

    TupleExpr(List<PyExpression> expressions, Location location) {
        super(location)
        this.expressions = expressions
    }

    @Override
    void writeCode(PythonCompiler compiler, JvmWriter writer) {
        writer.createArgs(expressions)
        compiler.checkNoPop(location)
        writer.cast(Type.getType(Object))
    }
}
