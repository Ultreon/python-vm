package dev.ultreon.pythonc.statement

import dev.ultreon.pythonc.*
import dev.ultreon.pythonc.expr.PyExpression
import dev.ultreon.pythonc.functions.PyFunction
import org.objectweb.asm.Type

class ReturnStatement implements PyStatement {
    private final List<StarExpression> starredExpressions
    private final PyFunction function
    private Location location

    ReturnStatement(List<StarExpression> starredExpressions, PyFunction function, Location location) {
        this.starredExpressions = starredExpressions
        this.function = function
        this.location = location
    }

    @Override
    void writeStatement(PythonCompiler compiler, JvmWriter writer) {
        if (starredExpressions.size() > 1) {
            throw new CompilerException("Too many expressions!", starredExpressions.get(1).location)
        }

        if (function.returnType().equals(Type.VOID_TYPE)) {
            if (starredExpressions.size() == 1) {
                throw new CompilerException("Can't return a value when the function is returning nothing!", location)
            }

            compiler.checkPop(location)
            writer.returnVoid()
            return
        }

        Type type = function.returnType()

        if (starredExpressions.size() == 1) {
            PyExpression expression = starredExpressions.get(0).expression()
            expression.write(compiler, writer)
            writer.returnValue(type, location)
            compiler.checkPop(location)
            return
        }

        writer.pushNull()
        writer.returnValue(type, location)
        compiler.checkPop(location)
    }

    @Override
    Location getLocation() {
        return location
    }
}
