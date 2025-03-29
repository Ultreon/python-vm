package dev.ultreon.pythonc.statement;

import dev.ultreon.pythonc.JvmWriter;
import dev.ultreon.pythonc.Location;
import dev.ultreon.pythonc.PythonCompiler;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;

public final class PyBlock extends PyStatement {
    private final List<PyStatement> statements;
    private final Location location;

    public PyBlock(List<PyStatement> statements, Location location) {
        this.statements = statements;
        this.location = location;
    }

    public static PyBlock.Builder builder(Location location) {
        return new PyBlock.Builder(location);
    }

    public List<PyStatement> getStatements() {
        return statements;
    }

    @Override
    public void writeStatement(PythonCompiler compiler, JvmWriter writer) {
        compiler.checkPop(location);
        for (PyStatement statement : statements) {
            statement.write(compiler, writer);
            compiler.checkPop(statement.location());
        }
        compiler.checkPop(location);
    }

    @Override
    public Location location() {
        return location;
    }

    public static class Builder {
        private List<PyStatement> statements = new ArrayList<>();
        private Location location;

        private Builder(Location location) {
            this.location = location;
        }

        public Builder statement(PyStatement statement) {
            statements.add(statement);
            return this;
        }

        public PyBlock build() {
            return new PyBlock(statements, location);
        }
    }
}
