package dev.ultreon.pythonc.expr;

import dev.ultreon.pythonc.JvmWriter;
import dev.ultreon.pythonc.Location;
import dev.ultreon.pythonc.PythonCompiler;
import dev.ultreon.pythonc.expr.compare.ComparisonType;
import org.objectweb.asm.Type;

public class PyComparisonExpr extends PyExpression {

    private final ComparisonType type;
    private final PyExpression left;
    private final PyExpression right;

    public PyComparisonExpr(ComparisonType type, PyExpression left, PyExpression right, Location location) {
        super(location);
        this.type = type;
        this.left = left;
        this.right = right;
    }

    @Override
    public void writeCode(PythonCompiler compiler, JvmWriter writer) {
        left.write(compiler, writer);
        compiler.checkNoPop(location());
        right.write(compiler, writer);
        compiler.checkNoPop(location());
        switch (type) {
            case EQ -> writer.dynamicEq();
            case NE -> writer.dynamicNe();
            case LT -> writer.dynamicLt();
            case LE -> writer.dynamicLe();
            case GT -> writer.dynamicGt();
            case GE -> writer.dynamicGe();
            case IS -> writer.dynamicIs();
            case IN -> writer.dynamicIn();
            case IS_NOT -> writer.dynamicIsNot();
            case NOT_IN -> writer.dynamicNotIn();
            case null, default -> throw new AssertionError("DEBUG");
        }
    }
}
