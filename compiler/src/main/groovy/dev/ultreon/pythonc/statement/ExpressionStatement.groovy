package dev.ultreon.pythonc.statement

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler
import dev.ultreon.pythonc.StarExpression
import org.objectweb.asm.Type

class ExpressionStatement implements PyStatement {
    private final List<StarExpression> expression
    private Location location

    ExpressionStatement(List<StarExpression> expression, Location location) {
        super()
        this.expression = expression
        this.location = location
    }

    @Override
    void writeStatement(PythonCompiler compiler, JvmWriter writer) {
        for (StarExpression expr : expression) {
            Type write = expr.write(compiler, writer)
            if (!write.equals(Type.VOID_TYPE)) {
                writer.pop()
            }

            compiler.checkPop(location)
        }
    }

    @Override
    Location getLocation() {
        return location
    }

    String toString() {
        StringBuilder builder = new StringBuilder()

        builder.append(Location.ANSI_RED).append("Expression ").append(Location.ANSI_RESET).append("(").append(expression).append(")").append(Location.ANSI_RESET)
        return builder.toString()
    }
}
