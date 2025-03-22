package dev.ultreon.pythonc;

import org.antlr.v4.runtime.ParserRuleContext;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.*;

class PyEval implements PyExpr {
    private final PythonCompiler compiler;
    private final ParserRuleContext ctx;
    private final Operator operator;
    private final Object finalValue;
    private final Object finalAddition;

    public enum Operator {
        ADD, SUB, MUL, DIV, MOD, FLOORDIV, AND, LSHIFT, RSHIFT, OR, XOR, UNARY_NOT, UNARY_PLUS, UNARY_MINUS, POW
    }

    public PyEval(PythonCompiler compiler, ParserRuleContext ctx, Operator operator, Object finalValue, Object finalAddition) {
        this.compiler = compiler;
        this.ctx = ctx;
        this.operator = operator;
        this.finalValue = finalValue;
        this.finalAddition = finalAddition;
    }

    @Override
    public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        return null;
    }

    @Override
    public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
        switch (finalValue) {
            case PyExpr pyExpr -> {
                loadValue(mv, compiler, pyExpr);
                if (finalAddition != null) {
                    if (pyExpr.type(compiler) == Type.INT_TYPE) {
                        Type type = typeOf(finalAddition, compiler);
                        if (type == Type.LONG_TYPE) {
                            compiler.writer.smartCast(Type.LONG_TYPE);
                        } else if (type == Type.FLOAT_TYPE) {
                            compiler.writer.smartCast(Type.FLOAT_TYPE);
                        } else if (type == Type.DOUBLE_TYPE) {
                            compiler.writer.smartCast(Type.DOUBLE_TYPE);
                        }
                    } else if (pyExpr.type(compiler) == Type.LONG_TYPE) {
                        Type type = typeOf(finalAddition, compiler);
                        if (type == Type.FLOAT_TYPE) {
                            compiler.writer.smartCast(Type.DOUBLE_TYPE);
                            loadAddition(compiler);
                            compiler.writer.smartCast(Type.DOUBLE_TYPE);
                            doOperation(mv);
                            return;
                        } else if (type == Type.DOUBLE_TYPE) {
                            compiler.writer.smartCast(Type.DOUBLE_TYPE);
                        }
                    } else if (pyExpr.type(compiler) == Type.FLOAT_TYPE) {
                        Type type = typeOf(finalAddition, compiler);
                        if (type == Type.DOUBLE_TYPE) {
                            compiler.writer.smartCast(Type.DOUBLE_TYPE);
                        } else if (type == Type.LONG_TYPE) {
                            compiler.writer.smartCast(Type.DOUBLE_TYPE);
                            loadAddition(compiler);
                            compiler.writer.smartCast(Type.DOUBLE_TYPE);
                            doOperation(mv);
                            return;
                        } else if (type == Type.INT_TYPE) {
                            loadAddition(compiler);
                            compiler.writer.smartCast(Type.FLOAT_TYPE);
                            doOperation(mv);
                            return;
                        }
                    } else if (pyExpr.type(compiler) == Type.DOUBLE_TYPE) {
                        Type type = typeOf(finalAddition, compiler);
                        if (type == Type.LONG_TYPE) {
                            compiler.writer.smartCast(Type.DOUBLE_TYPE);
                        } else if (type == Type.INT_TYPE) {
                            loadAddition(compiler);
                            compiler.writer.smartCast(Type.DOUBLE_TYPE);
                            doOperation(mv);
                            return;
                        } else if (type == Type.FLOAT_TYPE) {
                            loadAddition(compiler);
                            compiler.writer.smartCast(Type.DOUBLE_TYPE);
                            doOperation(mv);
                            return;
                        }
                    } else {
                        throw new RuntimeException("Unknown type: " + pyExpr.type(compiler));
                    }
                }
            }
            case Integer integer -> {
                loadConst(mv, compiler, integer);
                if (finalAddition != null) {
                    Type type = typeOf(finalAddition, compiler);
                    if (type == Type.LONG_TYPE) {
                        mv.visitInsn(I2L);
                    } else if (type == Type.FLOAT_TYPE) {
                        mv.visitInsn(I2F);
                    } else if (type == Type.DOUBLE_TYPE) {
                        mv.visitInsn(I2D);
                    }
                }
            }
            case Long aLong -> {
                loadConst(mv, compiler, aLong);
                if (finalAddition != null) {
                    Type type = typeOf(finalAddition, compiler);
                    if (type == Type.FLOAT_TYPE) {
                        mv.visitInsn(L2D);
                        loadAddition(compiler);
                        mv.visitInsn(F2D);
                        doOperation(mv);
                        return;
                    } else if (type == Type.DOUBLE_TYPE) {
                        mv.visitInsn(L2D);
                    }
                }
            }
            case Float aFloat -> {
                loadConst(mv, compiler, aFloat);

                if (finalAddition != null) {
                    Type type = typeOf(finalAddition, compiler);
                    if (type == Type.DOUBLE_TYPE) {
                        mv.visitInsn(F2D);
                    } else if (type == Type.LONG_TYPE) {
                        mv.visitInsn(F2D);
                        loadAddition(compiler);
                        mv.visitInsn(L2D);
                        doOperation(mv);
                        return;
                    } else if (type == Type.INT_TYPE) {
                        loadAddition(compiler);
                        mv.visitInsn(I2F);
                        doOperation(mv);
                        return;
                    }
                }
            }
            case Double aDouble -> {
                loadConst(mv, compiler, aDouble);

                if (finalAddition != null) {
                    Type type = typeOf(finalAddition, compiler);
                    if (type == Type.LONG_TYPE) {
                        loadAddition(compiler);
                        compiler.writer.smartCast(Type.DOUBLE_TYPE);
                        doOperation(mv);
                        return;
                    } else if (type == Type.INT_TYPE) {
                        loadAddition(compiler);
                        compiler.writer.smartCast(Type.DOUBLE_TYPE);
                        doOperation(mv);
                        return;
                    } else if (type == Type.FLOAT_TYPE) {
                        loadAddition(compiler);
                        compiler.writer.smartCast(Type.DOUBLE_TYPE);
                        doOperation(mv);
                        return;
                    }
                }
            }
            case String s -> loadConst(mv, compiler, s);
            case Boolean aBoolean -> {
                loadConst(mv, compiler, aBoolean);

                if (finalAddition != null) {
                    Type type = typeOf(finalAddition, compiler);
                    if (type == Type.INT_TYPE) {
                        loadAddition(compiler);
                        mv.visitInsn(I2F);
                        doOperation(mv);
                        return;
                    } else if (type == Type.LONG_TYPE) {
                        loadAddition(compiler);
                        mv.visitInsn(L2D);
                        doOperation(mv);
                        return;
                    } else if (type == Type.FLOAT_TYPE) {
                        loadAddition(compiler);
                        mv.visitInsn(F2D);
                        doOperation(mv);
                        return;
                    }
                }
            }
            case Character aChar -> {
                loadConst(mv, compiler, aChar);

                if (finalAddition != null) {
                    Type type = typeOf(finalAddition, compiler);
                    if (type == Type.INT_TYPE) {
                        loadAddition(compiler);
                        mv.visitInsn(I2F);
                        doOperation(mv);
                        return;
                    } else if (type == Type.LONG_TYPE) {
                        loadAddition(compiler);
                        mv.visitInsn(L2D);
                        doOperation(mv);
                        return;
                    } else if (type == Type.FLOAT_TYPE) {
                        loadAddition(compiler);
                        mv.visitInsn(F2D);
                        doOperation(mv);
                        return;
                    }
                }
            }
            case Unit unit -> throw new RuntimeException("unit not supported for:\n" + ctx.getText());
            default -> throw new RuntimeException("No supported matching loadExpr found for:\n" + ctx.getText());
        }
        if (finalAddition != null) {
            loadAddition(compiler);
            doOperation(mv);
        }
    }

    private void loadConst(MethodVisitor mv, PythonCompiler compiler, Object aChar) {
        compiler.loadConstant(ctx, aChar, mv);
        if (this.operator == Operator.POW) {
            compiler.writer.smartCast(Type.DOUBLE_TYPE);
        }
    }

    private void loadValue(MethodVisitor mv, PythonCompiler compiler, PyExpr pyExpr) {
        pyExpr.load(mv, compiler, pyExpr.preload(mv, compiler, false), false);
        if (this.operator == Operator.POW) {
            compiler.writer.smartCast(Type.DOUBLE_TYPE);
        }
    }

    private void loadAddition(PythonCompiler compiler) {
        compiler.loadExpr(ctx, finalAddition);
        if (this.operator == Operator.POW) {
            compiler.writer.smartCast(Type.DOUBLE_TYPE);
        }
    }

    private void doOperation(MethodVisitor mv) {
        if (operator == Operator.ADD) {
            compiler.writer.addValues();
        } else if (operator == Operator.SUB) {
            compiler.writer.subtractValues();
        } else if (operator == Operator.MUL) {
            compiler.writer.multiplyValues();
        } else if (operator == Operator.DIV) {
            compiler.writer.divideValues();
        } else if (operator == Operator.MOD) {
            compiler.writer.modValues();
        } else if (operator == Operator.AND) {
            compiler.writer.andValues();
        } else if (operator == Operator.OR) {
            compiler.writer.orValues();
        } else if (operator == Operator.XOR) {
            compiler.writer.xorValues();
        } else if (operator == Operator.LSHIFT) {
            compiler.writer.leftShiftValues();
        } else if (operator == Operator.RSHIFT) {
            compiler.writer.rightShiftValues();
        } else if (operator == Operator.FLOORDIV) {
            compiler.writer.floorDivideValues();
        } else if (operator == Operator.POW) {
            compiler.writer.powValues();
        } else if (operator == Operator.UNARY_NOT) {
            compiler.writer.notValue();
        } else if (operator == Operator.UNARY_MINUS) {
            compiler.writer.negateValue();
        } else if (operator == Operator.UNARY_PLUS) {
            compiler.writer.positiveValue();
        } else {
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
    public int lineNo() {
        return ctx.getStop().getLine();
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
            return expr.type(compiler);
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
        }

        throw new RuntimeException("No supported matching sum type found for:\n" + ctx.getText());
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
