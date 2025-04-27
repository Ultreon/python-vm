package dev.ultreon.pythonc.statement

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler
import org.objectweb.asm.Label

final class PyBlock implements PyStatement {
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
        compiler.checkPop(writer.lastLocation())
        trackLast(this)
        for (PyStatement statement : statements) {
            statement.write(compiler, writer)
            trackLast(statement)
            compiler.checkPop(statement.location)
        }
        compiler.checkPop(location)
        trackLast(this)
    }

    @Override
    Location getLocation() {
        return location
    }

    String toString() {
        StringBuilder builder = new StringBuilder()
        if (statements.size() == 0) return builder.append(Location.ANSI_RED).append("Block ").append(Location.ANSI_RESET).append("{}").toString()
        if (statements.size() == 1) return statements.get(0).toString()
        builder.append(Location.ANSI_RED).append("Block ")
        builder.append(Location.ANSI_RESET).append("{\n")
        for (PyStatement statement : statements) {
            builder.append("  ").append(statement.toString().replace("\n", "\n  ")).append("\n")
        }
        builder.append(Location.ANSI_RESET).append("}")
        return builder.toString()
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
