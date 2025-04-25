package dev.ultreon.pythonc

import dev.ultreon.pythonc.expr.PyExpression
import dev.ultreon.pythonc.statement.PyStatement

class RaiseStatement implements PyStatement {
    private final PyExpression value
    private final Location location

    RaiseStatement(PyExpression value, Location location) {
        this.value = value
        this.location = location
    }

    @Override
    void writeStatement(PythonCompiler compiler, JvmWriter writer) {
        value.write(compiler, writer)
        writer.throwObject()
    }

    @Override
    Location getLocation() {
        return location
    }
}
