package dev.ultreon.pythonc;

import dev.ultreon.pythonc.expr.PyExpression;
import org.objectweb.asm.Type;

class PyEval extends PyExpression {
    private final Operator operator;
    private final PyExpression finalValue;
    private final PyExpression finalAddition;

    public enum Operator {
        ADD, SUB, MUL, DIV, MOD, FLOORDIV, AND, LSHIFT, RSHIFT, OR, XOR, UNARY_NOT, UNARY_PLUS, UNARY_MINUS, POW, IS, IS_NOT, IN, NOT_IN, EQ, NE, LT, LE, GT, GE
    }

    public PyEval(Operator operator, PyExpression finalValue, PyExpression finalAddition, Location location) {
        super(location);
        this.operator = operator;
        this.finalValue = finalValue;
        this.finalAddition = finalAddition;
    }

    @Override
    public void writeCode(PythonCompiler compiler, JvmWriter writer) {
        if (finalAddition != null) {
            finalValue.write(compiler, writer);
            compiler.writer.cast(Type.getType(Object.class));
            finalAddition.write(compiler, writer);
            compiler.writer.cast(Type.getType(Object.class));
            doOperation(compiler);
            return;
        }
        finalValue.write(compiler, writer);
        compiler.writer.cast(Type.getType(Object.class));
    }

    private void doOperation(PythonCompiler compiler) {
        switch (operator) {
            case ADD -> compiler.writer.dynamicAdd();
            case SUB -> compiler.writer.dynamicSub();
            case MUL -> compiler.writer.dynamicMul();
            case DIV -> compiler.writer.dynamicDiv();
            case MOD -> compiler.writer.dynamicMod();
            case AND -> compiler.writer.dynamicAnd();
            case OR -> compiler.writer.dynamicOr();
            case XOR -> compiler.writer.dynamicXor();
            case LSHIFT -> compiler.writer.dynamicLShift();
            case RSHIFT -> compiler.writer.dynamicRShift();
            case FLOORDIV -> compiler.writer.dynamicFloorDiv();
            case POW -> compiler.writer.dynamicPow();
            case UNARY_NOT -> compiler.writer.dynamicNot();
            case UNARY_MINUS -> compiler.writer.dynamicNeg();
            case UNARY_PLUS -> compiler.writer.dynamicPos();
            case EQ -> {
                compiler.writer.dynamicEq();
                compiler.writer.cast(Type.BOOLEAN_TYPE);
            }
            case NE -> {
                compiler.writer.dynamicNe();
                compiler.writer.cast(Type.BOOLEAN_TYPE);
            }
            case LT -> {
                compiler.writer.dynamicLt();
                compiler.writer.cast(Type.BOOLEAN_TYPE);
            }
            case LE -> {
                compiler.writer.dynamicLe();
                compiler.writer.cast(Type.BOOLEAN_TYPE);
            }
            case GT -> {
                compiler.writer.dynamicGt();
                compiler.writer.cast(Type.BOOLEAN_TYPE);
            }
            case GE -> {
                compiler.writer.dynamicGe();
                compiler.writer.cast(Type.BOOLEAN_TYPE);
            }
            case IN -> {
                compiler.writer.dynamicIn();
                compiler.writer.cast(Type.BOOLEAN_TYPE);
            }
            case NOT_IN -> {
                compiler.writer.dynamicNotIn();
                compiler.writer.cast(Type.BOOLEAN_TYPE);
            }
            case IS -> {
                compiler.writer.dynamicIs();
                compiler.writer.cast(Type.BOOLEAN_TYPE);
            }
            case IS_NOT -> {
                compiler.writer.dynamicIsNot();
                compiler.writer.cast(Type.BOOLEAN_TYPE);
            }
            case null -> {

            }
            default -> throw new RuntimeException("No supported matching operator found for:\n" + location());
        }
    }

    @Override
    public Type type() {
        return switch (operator) {
            case IN, NOT_IN, IS, IS_NOT, EQ, NE, LT, LE, GT, GE -> Type.BOOLEAN_TYPE;
            default -> super.type();
        };
    }
}
