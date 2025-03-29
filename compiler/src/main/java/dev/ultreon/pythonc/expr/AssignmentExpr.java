package dev.ultreon.pythonc.expr;

import dev.ultreon.pythonc.JvmWriter;
import dev.ultreon.pythonc.Location;
import dev.ultreon.pythonc.PythonCompiler;
import dev.ultreon.pythonc.statement.PyStatement;

public class AssignmentExpr extends PyExpression {
    private final Settable[] targets;
    private final PyExpression value;
    private final Location location;

    public AssignmentExpr(Settable[] targets, PyExpression value, Location location) {
        super(location);
        this.targets = targets;
        this.value = value;
        this.location = location;
    }

    public Settable[] targets() {
        return targets;
    }

    public PyExpression value() {
        return value;
    }

    @Override
    public void writeCode(PythonCompiler compiler, JvmWriter writer) {
        value.write(compiler, writer);
        int i = 0;
        for (Settable target : targets) {
            writer.dup();
            target.set(compiler, writer, value);
            i++;
        }
    }
}
