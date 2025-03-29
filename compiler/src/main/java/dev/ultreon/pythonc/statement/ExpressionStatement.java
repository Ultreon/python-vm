package dev.ultreon.pythonc.statement;

import dev.ultreon.pythonc.JvmWriter;
import dev.ultreon.pythonc.Location;
import dev.ultreon.pythonc.PythonCompiler;
import dev.ultreon.pythonc.StarExpression;
import dev.ultreon.pythonc.expr.PyExpression;
import org.objectweb.asm.Type;

import java.util.List;

public class ExpressionStatement extends PyStatement {
    private final List<StarExpression> expression;
    private Location location;

    public ExpressionStatement(List<StarExpression> expression, Location location) {
        super();
        this.expression = expression;
        this.location = location;
    }

    @Override
    public void writeStatement(PythonCompiler compiler, JvmWriter writer) {
        for (StarExpression expr : expression) {
            Type write = expr.write(compiler, writer);
            if (!write.equals(Type.VOID_TYPE)) {
                writer.pop();
            }

            compiler.checkPop(location);
        }
    }

    @Override
    public Location location() {
        return location;
    }
}
