package dev.ultreon.pythonc;

import dev.ultreon.pythonc.expr.PyExpression;
import dev.ultreon.pythonc.lang.PyAST;

public class StarExpression implements PyAST {
    private final PyExpression expression;
    private final boolean star;
    private Location location;

    public StarExpression(PyExpression expression, boolean star, Location location) {
        this.expression = expression;
        this.star = star;
        this.location = location;
    }

    @Override
    public void writeCode(PythonCompiler compiler, JvmWriter writer) {
        if (star) throw new TODO();

        expression.write(compiler, writer);
    }

    @Override
    public Location location() {
        return location;
    }

    public PyExpression expression() {
        return expression;
    }

    public boolean star() {
        return star;
    }
}
