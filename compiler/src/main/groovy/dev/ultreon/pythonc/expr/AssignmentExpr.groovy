package dev.ultreon.pythonc.expr

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler

class AssignmentExpr extends PyExpression {
    private final Settable[] targets
    private final PyExpression value
    private final Location location

    AssignmentExpr(Settable[] targets, PyExpression value, Location location) {
        super(location)
        this.targets = targets
        this.value = value
        this.location = location
    }

    Settable[] targets() {
        return targets
    }

    PyExpression value() {
        return value
    }

    @Override
    void writeCode(PythonCompiler compiler, JvmWriter writer) {
        value.write(compiler, writer)
        int i = 0
        for (Settable target : targets) {
            writer.dup()
            target.set(compiler, writer, value)
            i++
        }
    }

    @Override
    String toString() {
        StringBuilder builder = new StringBuilder()

        builder.append(Location.ANSI_RED).append("Assignment").append(Location.ANSI_WHITE).append("[Expression] ").append(Location.ANSI_RESET).append("(").append(targets[0]).append(")").append(Location.ANSI_RED).append(" = ").append(Location.ANSI_RESET).append("(").append(value).append(")").append(Location.ANSI_RESET)
        return builder.toString()
    }
}
