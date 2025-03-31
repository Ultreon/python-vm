package dev.ultreon.pythonc

import dev.ultreon.pythonc.expr.PyExpression
import dev.ultreon.pythonc.lang.PyAST
import org.objectweb.asm.Type

class StarExpression implements PyAST {
    private final PyExpression expression
    private final boolean star
    private Location location

    StarExpression(PyExpression expression, boolean star, Location location) {
        this.expression = expression
        this.star = star
        this.location = location
    }

    @Override
    void writeCode(PythonCompiler compiler, JvmWriter writer) {
        if (star) throw new TODO()

        expression.write(compiler, writer)
    }

    @Override
    Location getLocation() {
        return location
    }

    PyExpression expression() {
        return expression
    }

    boolean star() {
        return star
    }

    String toString() {
        StringBuilder builder = new StringBuilder()

        if (star) {
            builder.append(Location.ANSI_RED).append("[*]").append(Location.ANSI_RESET).append(" ")
        } else {
            builder.append(Location.ANSI_RED).append("[_]").append(Location.ANSI_RESET).append(" ")
        }
        builder.append(expression)

        return builder.toString()
    }
}
