package dev.ultreon.pythonc;

import org.antlr.v4.runtime.ParserRuleContext;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.List;

import static org.objectweb.asm.Opcodes.*;

class PyEval implements PyExpr {
    private final PythonCompiler compiler;
    private final ParserRuleContext ctx;
    private final Operator operator;
    private final Object finalValue;
    private final Object finalAddition;
    private Location location;

    public enum Operator {
        ADD, SUB, MUL, DIV, MOD, FLOORDIV, AND, LSHIFT, RSHIFT, OR, XOR, UNARY_NOT, UNARY_PLUS, UNARY_MINUS, POW
    }

    public PyEval(PythonCompiler compiler, ParserRuleContext ctx, Operator operator, Object finalValue, Object finalAddition, Location location) {
        this.compiler = compiler;
        this.ctx = ctx;
        this.operator = operator;
        this.finalValue = finalValue;
        this.finalAddition = finalAddition;
        this.location = location;
    }

    @Override
    public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        return null;
    }

    @Override
    public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
        if (finalAddition != null) {
            loadValue(mv, compiler, expr(finalValue));
            loadAddition(compiler);
            doOperation(mv, compiler);
            return;
        }
        loadValue(mv, compiler, expr(finalValue));
    }

    private PyExpr expr(Object finalValue) {
        return switch (finalValue) {
            case PyExpr expr -> expr;
            case Byte aByte -> new PyConstant(aByte, location);
            case Short aShort -> new PyConstant(aShort, location);
            case Integer anInt -> new PyConstant(anInt, location);
            case Long aLong -> new PyConstant(aLong, location);
            case Float aFloat -> new PyConstant(aFloat, location);
            case Double aDouble -> new PyConstant(aDouble, location);
            case String s -> new PyConstant(s, location);
            case Boolean aBoolean -> new PyConstant(aBoolean, location);
            case Character aChar -> new PyConstant(aChar, location);
            case null, default -> throw new RuntimeException("No supported matching expr found for:\n" + ctx.getText());
        };
    }

    private void loadConst(MethodVisitor mv, PythonCompiler compiler, Object aChar) {
        compiler.loadConstant(ctx, aChar, mv);
        if (this.operator == Operator.POW) {
            compiler.writer.smartCast(Type.DOUBLE_TYPE);
        }
    }

    private Type loadValue(MethodVisitor mv, PythonCompiler compiler, PyExpr pyExpr) {
        pyExpr.load(mv, compiler, pyExpr.preload(mv, compiler, false), false);

        return pyExpr.type(compiler);
    }

    private Type loadAddition(PythonCompiler compiler) {
        PyExpr pyExpr = compiler.loadExpr(ctx, finalAddition);
        if (this.operator == Operator.POW) {
            compiler.writer.smartCast(Type.DOUBLE_TYPE);
        }

        return pyExpr.type(compiler);
    }

    private void doOperation(MethodVisitor mv, PythonCompiler compiler) {
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
            case null, default ->
                    throw new RuntimeException("No supported matching operator found for:\n" + ctx.getText());
        }
    }

    private Type typeOf(Object finalAddition, PythonCompiler compiler) {
        if (finalAddition instanceof PyExpr expr) {
            return expr.type(compiler);
        } else if (finalAddition instanceof Integer) {
            return Type.INT_TYPE;
        } else if (finalAddition instanceof Long) {
            return Type.LONG_TYPE;
        } else if (finalAddition instanceof Float) {
            return Type.FLOAT_TYPE;
        } else if (finalAddition instanceof Double) {
            return Type.DOUBLE_TYPE;
        } else if (finalAddition instanceof String) {
            return Type.getType(String.class);
        } else if (finalAddition instanceof Boolean) {
            return Type.BOOLEAN_TYPE;
        } else if (finalAddition instanceof Character) {
            return Type.CHAR_TYPE;
        } else if (finalAddition instanceof Unit) {
            return Type.VOID_TYPE;
        } else if (finalAddition instanceof Byte) {
            return Type.BYTE_TYPE;
        } else if (finalAddition instanceof Short) {
            return Type.SHORT_TYPE;
        }
        throw new RuntimeException("No supported matching typeOf found for:\n" + ctx.getText());
    }

    @Override
    public void expectReturnType(PythonCompiler compiler, JvmClass returnType, Location location) {
        Type type = type(compiler);
        if (type == Type.VOID_TYPE) {
            return;
        }
        if (!returnType.doesInherit(compiler, PythonCompiler.classCache.require(compiler, type))) {
            throw new CompilerException("Expected " + returnType + " but got " + type, location);
        }
    }

    @Override
    public Type type(PythonCompiler compiler) {
        if (finalAddition != null) {
            if (finalAddition instanceof PyExpr expr) {
                Type type = expr.type(compiler);
                if (finalValue instanceof PyExpr expr2) {
                    Type type2 = expr2.type(compiler);
                    if (type.equals(Type.LONG_TYPE) && type2.equals(Type.LONG_TYPE)) {
                        return Type.LONG_TYPE;
                    }
                    if (type.equals(Type.DOUBLE_TYPE) && type2.equals(Type.DOUBLE_TYPE)) {
                        return Type.DOUBLE_TYPE;
                    }
                } else if (finalValue instanceof Integer integer) {
                    Type longType = castInt(type);
                    if (longType != null) return longType;
                } else if (finalValue instanceof Long l) {
                    if (type.equals(Type.DOUBLE_TYPE)) {
                        return Type.DOUBLE_TYPE;
                    }
                    if (type.equals(Type.FLOAT_TYPE)) {
                        return Type.DOUBLE_TYPE;
                    }
                    return Type.LONG_TYPE;
                } else if (finalValue instanceof Float f) {
                    if (type.equals(Type.DOUBLE_TYPE)) {
                        return Type.DOUBLE_TYPE;
                    } else if (type.equals(Type.LONG_TYPE)) {
                        return Type.DOUBLE_TYPE;
                    }
                    return Type.FLOAT_TYPE;
                } else if (finalValue instanceof Double d) {
                    return Type.DOUBLE_TYPE;
                }
                return Type.DOUBLE_TYPE;
            }
        }

        if (finalValue instanceof PyExpr expr) {
            Type type = expr.type(compiler);
            if (type == null) {
                throw new RuntimeException("No type for: " + expr.getClass().getName());
            }
            return type;
        } else if (finalValue instanceof Integer integer) {
            return Type.INT_TYPE;
        } else if (finalValue instanceof Long l) {
            return Type.LONG_TYPE;
        } else if (finalValue instanceof Float f) {
            return Type.FLOAT_TYPE;
        } else if (finalValue instanceof Double d) {
            return Type.DOUBLE_TYPE;
        } else if (finalValue instanceof Boolean b) {
            return Type.BOOLEAN_TYPE;
        } else if (finalValue instanceof Byte b) {
            return Type.BYTE_TYPE;
        } else if (finalValue instanceof Short s) {
            return Type.SHORT_TYPE;
        } else if (finalValue instanceof Character c) {
            return Type.CHAR_TYPE;
        } else if (finalValue instanceof String s) {
            return Type.getType(String.class);
        } else if (finalValue instanceof Unit u) {
            return Type.VOID_TYPE;
        } else if (finalValue == None.None) {
            return Type.VOID_TYPE;
        }

        throw new RuntimeException("No supported matching owner found for:\n" + ctx.getText());
    }

    @Override
    public Location location() {
        return location;
    }

    private static @Nullable Type castInt(Type type) {
        if (type.equals(Type.LONG_TYPE)) {
            return Type.LONG_TYPE;
        } else if (type.equals(Type.DOUBLE_TYPE)) {
            return Type.DOUBLE_TYPE;
        } else if (type.equals(Type.INT_TYPE)) {
            return Type.INT_TYPE;
        } else if (type.equals(Type.FLOAT_TYPE)) {
            return Type.FLOAT_TYPE;
        }
        return null;
    }
}
