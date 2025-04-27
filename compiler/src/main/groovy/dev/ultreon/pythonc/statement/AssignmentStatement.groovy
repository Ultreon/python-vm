package dev.ultreon.pythonc.statement

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler
import dev.ultreon.pythonc.TupleExtractionExpr
import dev.ultreon.pythonc.expr.PyExpression
import dev.ultreon.pythonc.expr.Settable

class AssignmentStatement implements PyStatement {
    private final Settable target
    private final PyExpression value
    private final Location location

    AssignmentStatement(Settable[] targets, PyExpression value, Location location) {
        if (targets.length != 1) {
            target = new TupleExtractionExpr(targets.collect { (PyExpression) it }.toArray(PyExpression[]::new), location)
        } else if (targets.length == 1) {
            target = targets[0]
        } else {
            throw new RuntimeException("AssignmentStatement must have at least one target")
        }
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
        target.set compiler, writer, value
        compiler.checkPop writer.lastLocation()
    }


    @Override
    String toString() {
        StringBuilder builder = new StringBuilder()

        builder.append(Location.ANSI_RED).append("Assignment").append(Location.ANSI_WHITE).append("[Statement] ").append(Location.ANSI_RESET).append("(").append(target).append(")").append(Location.ANSI_RED).append(" = ").append(Location.ANSI_RESET).append("(").append(value).append(")").append(Location.ANSI_RESET)
        return builder.toString()
    }
}
