package dev.ultreon.pythonc

import dev.ultreon.pythonc.expr.PyExpression

class PyStarExpression extends PyExpression {
    private final PyExpression expression
    private final boolean star

    PyStarExpression(PyExpression expression, boolean star, Location location) {
        super(location)
        this.expression = expression
        this.star = star
    }

    @Override
    void writeCode(PythonCompiler compiler, JvmWriter writer) {
        if (star) throw new TODO()

        expression.write(compiler, writer)
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
