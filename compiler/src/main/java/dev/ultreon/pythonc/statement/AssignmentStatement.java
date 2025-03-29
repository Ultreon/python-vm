package dev.ultreon.pythonc.statement;

import dev.ultreon.pythonc.JvmWriter;
import dev.ultreon.pythonc.Location;
import dev.ultreon.pythonc.PythonCompiler;
import dev.ultreon.pythonc.expr.PyExpression;
import dev.ultreon.pythonc.expr.Settable;
import dev.ultreon.pythonc.lang.PyAST;

public class AssignmentStatement extends PyStatement {
    private final Settable[] targets;
    private final PyExpression value;
    private final Location location;

    public AssignmentStatement(Settable[] targets, PyExpression value, Location location) {
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
    public Location location() {
        return location;
    }

    @Override
    public void writeStatement(PythonCompiler compiler, JvmWriter writer) {
        int i = 0;
        for (Settable target : targets) {
            target.set(compiler, writer, value);
            compiler.checkPop(target instanceof PyExpression ast ? ast.location() : location);
            i++;
        }

        compiler.checkPop(location);
    }
}
