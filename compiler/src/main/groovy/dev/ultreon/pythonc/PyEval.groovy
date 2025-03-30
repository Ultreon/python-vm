package dev.ultreon.pythonc

import dev.ultreon.pythonc.expr.PyExpression
import org.objectweb.asm.Type

import static dev.ultreon.pythonc.PyEval.Operator.*
import static org.objectweb.asm.Type.getType as type

class PyEval extends PyExpression {
    private final Operator operator
    private final PyExpression finalValue
    private final PyExpression finalAddition

    enum Operator {
        ADD, SUB, MUL, DIV, MOD, FLOORDIV, AND, LSHIFT, RSHIFT, OR, XOR, UNARY_NOT, UNARY_PLUS, UNARY_MINUS, POW, IS, IS_NOT, IN, NOT_IN, EQ, NE, LT, LE, GT, GE
    }

    PyEval(Operator operator, PyExpression finalValue, PyExpression finalAddition, Location location) {
        super(location)
        this.operator = operator
        this.finalValue = finalValue
        this.finalAddition = finalAddition
    }

    @Override
    void writeCode(PythonCompiler compiler, JvmWriter writer) {
        if (finalAddition != null) {
            finalValue.write(compiler, writer)
            compiler.writer.cast(type(Object.class))
            finalAddition.write(compiler, writer)
            compiler.writer.cast(type(Object.class))
            doOperation(compiler)
            return
        }
        finalValue.write(compiler, writer)
        compiler.writer.cast(type(Object.class))
    }

    private void doOperation(PythonCompiler compiler) {
        switch (operator) {
            case ADD:
                compiler.writer.dynamicAdd()
                break
            case SUB:
                compiler.writer.dynamicSub()
                break
            case MUL:
                compiler.writer.dynamicMul()
                break
            case DIV:
                compiler.writer.dynamicDiv()
                break
            case MOD:
                compiler.writer.dynamicMod()
                break
            case AND:
                compiler.writer.dynamicAnd()
                break
            case OR:
                compiler.writer.dynamicOr()
                break
            case XOR:
                compiler.writer.dynamicXor()
                break
            case LSHIFT:
                compiler.writer.dynamicLShift()
                break
            case RSHIFT:
                compiler.writer.dynamicRShift()
                break
            case FLOORDIV:
                compiler.writer.dynamicFloorDiv()
                break
            case POW:
                compiler.writer.dynamicPow()
                break
            case UNARY_NOT:
                compiler.writer.dynamicNot()
                break
            case UNARY_MINUS:
                compiler.writer.dynamicNeg()
                break
            case UNARY_PLUS:
                compiler.writer.dynamicPos()
                break
            case EQ:
                compiler.writer.dynamicEq()
                compiler.writer.cast(Type.BOOLEAN_TYPE)
                break
            case NE:
                compiler.writer.dynamicNe()
                compiler.writer.cast(Type.BOOLEAN_TYPE)
                break
            case LT:
                compiler.writer.dynamicLt()
                compiler.writer.cast(Type.BOOLEAN_TYPE)
                break
            case LE:
                compiler.writer.dynamicLe()
                compiler.writer.cast(Type.BOOLEAN_TYPE)
                break
            case GT:
                compiler.writer.dynamicGt()
                compiler.writer.cast(Type.BOOLEAN_TYPE)
                break
            case GE:
                compiler.writer.dynamicGe()
                compiler.writer.cast(Type.BOOLEAN_TYPE)
                break
            case IN:
                compiler.writer.dynamicIn()
                compiler.writer.cast(Type.BOOLEAN_TYPE)
                break
            case NOT_IN:
                compiler.writer.dynamicNotIn()
                compiler.writer.cast(Type.BOOLEAN_TYPE)
                break
            case IS:
                compiler.writer.dynamicIs()
                compiler.writer.cast(Type.BOOLEAN_TYPE)
                break
            case IS_NOT:
                compiler.writer.dynamicIsNot()
                compiler.writer.cast(Type.BOOLEAN_TYPE)
                break
            case null:
                break
            default:
                throw new RuntimeException("No supported matching operator found for:\n" + location)
        }
    }

    @Override
    Type getType() {
        switch (operator) {
            case IN:
            case NOT_IN:
            case IS:
            case IS_NOT:
            case EQ:
            case NE:
            case LT:
            case LE:
            case GT:
            case GE:
                return Type.BOOLEAN_TYPE
            default:
                return super.type
        }
    }
}
