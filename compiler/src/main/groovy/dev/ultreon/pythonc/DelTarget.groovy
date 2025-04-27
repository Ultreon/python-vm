package dev.ultreon.pythonc

import dev.ultreon.pythonc.expr.PyExpression

class DelTarget extends Deletion {
    private PyExpression expr
    private Location location

    DelTarget(PyExpression expr, Location location) {
        this.expr = expr
        this.location = location
    }

    PyExpression getExpr() {
        return expr
    }

    @Override
    void writeCode(PythonCompiler compiler, JvmWriter writer) {
        expr.delete(compiler, writer)
    }

    Location getLocation() {
        return location
    }
}
