package dev.ultreon.pythonc.statement

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler
import dev.ultreon.pythonc.expr.PyExpression
import dev.ultreon.pythonc.expr.Settable

class AssignmentStatement implements PyStatement {
    private final Settable[] targets
    private final PyExpression value
    private final Location location

    AssignmentStatement(Settable[] targets, PyExpression value, Location location) {
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
    Location getLocation() {
        return location
    }

    @Override
    void writeStatement(PythonCompiler compiler, JvmWriter writer) {
        int i = 0
        for (target in targets) {
            target.set compiler, writer, value
            compiler.checkPop target instanceof PyExpression ? ((PyExpression) target).location : location
            i++
        }

        compiler.checkPop location
    }


    @Override
    String toString() {
        StringBuilder builder = new StringBuilder()

        builder.append(Location.ANSI_RED).append("Assignment").append(Location.ANSI_WHITE).append("[Statement] ").append(Location.ANSI_RESET).append("(").append(targets[0]).append(")").append(Location.ANSI_RED).append(" = ").append(Location.ANSI_RESET).append("(").append(value).append(")").append(Location.ANSI_RESET)
        return builder.toString()
    }
}
