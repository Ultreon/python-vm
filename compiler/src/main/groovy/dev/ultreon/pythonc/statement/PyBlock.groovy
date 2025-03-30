package dev.ultreon.pythonc.statement

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler

final class PyBlock extends PyStatement {
    private final List<PyStatement> statements
    private final Location location

    PyBlock(List<PyStatement> statements, Location location) {
        this.statements = statements
        this.location = location
    }

    static Builder builder(Location location) {
        return new Builder(location)
    }

    List<PyStatement> getStatements() {
        return statements
    }

    @Override
    void writeStatement(PythonCompiler compiler, JvmWriter writer) {
        compiler.checkPop(location)
        for (PyStatement statement : statements) {
            statement.write(compiler, writer)
            compiler.checkPop(statement.location)
        }
        compiler.checkPop(location)
    }

    @Override
    Location getLocation() {
        return location
    }

    static class Builder {
        private List<PyStatement> statements = new ArrayList<>()
        private Location location

        private Builder(Location location) {
            this.location = location
        }

        Builder statement(PyStatement statement) {
            statements.add(statement)
            return this
        }

        PyBlock build() {
            return new PyBlock(statements, location)
        }
    }
}
