package dev.ultreon.pythonc

import dev.ultreon.pythonc.expr.PyExpression

class DelSubscript extends Deletion {
    private final PyExpression expr
    private final List<PyExpression> slices
    private final Location location

    DelSubscript(PyExpression expr, List<PyExpression> slices, Location location) {
        this.expr = expr
        this.slices = slices
        this.location = location
    }

    PyExpression getExpr() {
        return expr
    }

    List<PyExpression> getSlices() {
        return slices
    }

    @Override
    void writeCode(PythonCompiler compiler, JvmWriter writer) {
        expr.write(compiler, writer)

        if (slices.size() == 1) {
            slices[0].write(compiler, writer)
            writer.dynamicDelItem()
        } else if (slices.size() > 1) {
            throw new CompilerException("Multi-dimensional deletion is not supported!", location)
        } else if (slices.empty) {
            throw new CompilerException("Deletion of empty slice is not supported!", location)
        }
    }

    Location getLocation() {
        return location
    }
}
