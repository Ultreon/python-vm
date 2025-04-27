package dev.ultreon.pythonc

import dev.ultreon.pythonc.expr.PyExpression
import org.objectweb.asm.Type

import static dev.ultreon.pythonc.PyEval.Operator.*
import static org.objectweb.asm.Type.getType as type

class PyEval extends PyExpression {
    private final Operator operator
    private final PyExpression finalValue
    private final PyExpression finalAddition

    PyEval(Operator operator, PyExpression finalValue, PyExpression finalAddition, Location location) {
        super(location)
        this.operator = operator
        this.finalValue = finalValue
        this.finalAddition = finalAddition
    }

    enum Operator {
        ADD, SUB, MUL, DIV, MOD, FLOORDIV, AND, LSHIFT, RSHIFT, OR, XOR, UNARY_NOT, UNARY_PLUS, UNARY_MINUS, POW, IS, IS_NOT, IN, NOT_IN, EQ, NE, LT, LE, GT, GE

        boolean isUnary() {
            switch (this) {
                case UNARY_NOT: return true
                case UNARY_PLUS: return true
                case UNARY_MINUS: return true
                default: return false
            }
        }

        String toString(PyExpression left, PyExpression right) {
            switch (this) {
                case ADD: return "[$left]$Location.ANSI_RED + $Location.ANSI_RESET[$right]"
                case SUB: return "[$left]$Location.ANSI_RED - $Location.ANSI_RESET[$right]"
                case MUL: return "[$left]$Location.ANSI_RED * $Location.ANSI_RESET[$right]"
                case DIV: return "[$left]$Location.ANSI_RED / $Location.ANSI_RESET[$right]"
                case MOD: return "[$left]$Location.ANSI_RED % $Location.ANSI_RESET[$right]"
                case FLOORDIV: return "[$left]$Location.ANSI_RED // $Location.ANSI_RESET[$right]"
                case AND: return "[$left]$Location.ANSI_RED and $Location.ANSI_RESET[$right]"
                case LSHIFT: return "[$left]$Location.ANSI_RED << $Location.ANSI_RESET[$right]"
                case RSHIFT: return "[$left]$Location.ANSI_RED >> $Location.ANSI_RESET[$right]"
                case OR: return "[$left]$Location.ANSI_RED or $Location.ANSI_RESET[$right]"
                case XOR: return "[$left]$Location.ANSI_RED xor $Location.ANSI_RESET[$right]"
                case UNARY_NOT: return "${Location.ANSI_RED}not $Location.ANSI_RESET($left)$Location.ANSI_RESET"
                case UNARY_PLUS: return "$Location.ANSI_RED+$Location.ANSI_RESET[$left]"
                case UNARY_MINUS: return "$Location.ANSI_RED-$Location.ANSI_RESET[$left]"
                case POW: return "[$left]$Location.ANSI_RED ** $Location.ANSI_RESET[$right]"
                case IS: return "[$left]$Location.ANSI_RED is $Location.ANSI_RESET[$right]"
                case IS_NOT: return "[$left]$Location.ANSI_RED is not $Location.ANSI_RESET[$right]"
                case IN: return "[$left]$Location.ANSI_RED in $Location.ANSI_RESET[$right]"
                case NOT_IN: return "[$left]$Location.ANSI_RED not in $Location.ANSI_RESET[$right]"
                case EQ: return "[$left]$Location.ANSI_RED == $Location.ANSI_RESET[$right]"
                case NE: return "[$left]$Location.ANSI_RED != $Location.ANSI_RESET[$right]"
                case LT: return "[$left]$Location.ANSI_RED < $Location.ANSI_RESET[$right]"
                case LE: return "[$left]$Location.ANSI_RED <= $Location.ANSI_RESET[$right]"
                case GT: return "[$left]$Location.ANSI_RED > $Location.ANSI_RESET[$right]"
                case GE: return "[$left]$Location.ANSI_RED >= $Location.ANSI_RESET[$right]"
                default: throw new IllegalStateException()
            }
        }
    }

    @Override
    void writeCode(PythonCompiler compiler, JvmWriter writer) {
        def stackSize = compiler.context.stackSize
        if (finalAddition != null) {
            finalValue.write(compiler, writer)
            compiler.writer.cast(type(Object))
            if (!operator.unary) {
                finalAddition.write(compiler, writer)
                compiler.writer.cast(type(Object))
            }
            doOperation(compiler)
            if (writer.context.peek() != type(void)) {
                if (stackSize + 1 != compiler.context.stackSize) {
                    throw new CompilerException("Invalid stack size after 'eval' expression", location)
                }
            }
            return
        }
        finalValue.write(compiler, writer)
        if (operator != null && !operator.unary) {
            doUnaryOperation(compiler)
            return
        }
        compiler.writer.cast(type(Object))
        if (writer.context.peek() != type(void)) {
            if (stackSize + 1 != compiler.context.stackSize) {
                throw new CompilerException("Invalid stack size after 'eval' expression", location)
            }
        }
    }

    void doUnaryOperation(PythonCompiler pythonCompiler) {
        switch (operator) {
            case UNARY_NOT:
                pythonCompiler.writer.dynamicNot()
                break
            case UNARY_PLUS:
                pythonCompiler.writer.dynamicPos()
                break
            case UNARY_MINUS:
                pythonCompiler.writer.dynamicNeg()
                break
            default:
                throw new IllegalStateException()
        }
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

    String toString() {
        if (finalAddition != null) {
            return operator.toString(finalValue, finalAddition)
        }
        if (operator in [UNARY_MINUS, UNARY_PLUS, UNARY_NOT]) {
//            return operator.toString(finalValue)
        }

        return finalValue.toString()
    }
}
