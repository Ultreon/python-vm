package dev.ultreon.pythonc.expr;

import dev.ultreon.pythonc.JvmWriter;
import dev.ultreon.pythonc.Location;
import dev.ultreon.pythonc.PythonCompiler;
import dev.ultreon.pythonc.expr.compare.ComparisonType

import static dev.ultreon.pythonc.expr.compare.ComparisonType.*;

class PyComparisonExpr extends PyExpression {
    private final ComparisonType type;
    private final PyExpression left;
    private final PyExpression right;

    PyComparisonExpr(ComparisonType type, PyExpression left, PyExpression right, Location location) {
        super(location);
        this.type = type;
        this.left = left;
        this.right = right;
    }

    @Override
    void writeCode(PythonCompiler compiler, JvmWriter writer) {
        left.write(compiler, writer);
        compiler.checkNoPop(location);
        right.write(compiler, writer);
        compiler.checkNoPop(location);
        switch (type) {
            case EQ:
                writer.dynamicEq();
                break;
            case NE:
                writer.dynamicNe();
                break;
            case LT:
                writer.dynamicLt();
                break;
            case LE:
                writer.dynamicLe();
                break;
            case GT:
                writer.dynamicGt();
                break;
            case GE:
                writer.dynamicGe();
                break;
            case IS:
                writer.dynamicIs();
                break;
            case IN:
                writer.dynamicIn();
                break;
            case IS_NOT:
                writer.dynamicIsNot();
                break;
            case NOT_IN:
                writer.dynamicNotIn();
                break;
            case null:
            default:
                throw new IllegalStateException("DEBUG");
        }
    }
}
