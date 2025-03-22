package dev.ultreon.pythonc;

import org.antlr.v4.runtime.ParserRuleContext;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.*;

class PyComparison implements PyExpr {
    private final ParserRuleContext context;
    private final Comparison comparator;
    private final PythonParser.Compare_op_bitwise_or_pairContext ctx;

    public enum Comparison {
        EQ, NE, LT, LTE, GT, GTE
    }

    public PyComparison(ParserRuleContext context, Comparison comparator, PythonParser.Compare_op_bitwise_or_pairContext ctx) {
        this.context = context;
        this.comparator = comparator;
        this.ctx = ctx;
    }

    @Override
    public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        return null;
    }

    @Override
    public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
        PythonParser.Bitwise_orContext bitwiseOrContext = switch (context) {
            case PythonParser.Eq_bitwise_orContext ctx -> ctx.bitwise_or();
            case PythonParser.Noteq_bitwise_orContext ctx -> ctx.bitwise_or();
            case PythonParser.Lt_bitwise_orContext ctx -> ctx.bitwise_or();
            case PythonParser.Lte_bitwise_orContext ctx -> ctx.bitwise_or();
            case PythonParser.Gt_bitwise_orContext ctx -> ctx.bitwise_or();
            case PythonParser.Gte_bitwise_orContext ctx -> ctx.bitwise_or();
            default -> throw new RuntimeException("Unknown comparison context: " + context.getText());
        };
        if (bitwiseOrContext != null) {
            Object visit = compiler.visit(bitwiseOrContext);

            if (visit == null) {
                throw new RuntimeException("Unknown visitArgs for expression context: " + bitwiseOrContext.getText());
            }

            switch (visit) {
                case String s -> {
                    compiler.writer.loadConstant(s);

                    cmpString(mv, compiler);
                }
                case Integer s -> {
                    compiler.writer.loadConstant(s);

                    cmpInt(mv, compiler);
                }
                case Float s -> {
                    compiler.writer.loadConstant(s);

                    cmpFloat(mv, compiler);
                }
                case Long s -> {
                    compiler.writer.loadConstant(s);

                    cmpLong(mv, compiler);
                }
                case Double s -> {
                    compiler.writer.loadConstant(s);

                    cmpDouble(mv, compiler);
                }
                case Character s -> {
                    compiler.writer.loadConstant(s);

                    cmpInt(mv, compiler);
                }
                case Byte s -> {
                    compiler.writer.loadConstant(s);

                    cmpInt(mv, compiler);
                }
                case Short s -> {
                    compiler.writer.loadConstant(s);

                    cmpInt(mv, compiler);
                }
                case Boolean s -> {
                    compiler.writer.loadConstant(s);

                    cmpInt(mv, compiler);
                }
                case PyExpr expr -> {
                    expr.load(mv, compiler, expr.preload(mv, compiler, false), false);

                    cmpExpr(mv, compiler, expr);
                }
                case null, default -> throw new UnsupportedOperationException("Not implemented: " + visit.getClass());
            }

            Context context = compiler.getContext(Context.class);
            if (!(context instanceof ConditionContext conditionContext)) {
                context.push(Type.BOOLEAN_TYPE);
            }

            return;
        }
        throw new RuntimeException("compare_op_bitwise_or_pair not supported for:\n" + ctx.getText());
    }

    private void cmpExpr(MethodVisitor mv, PythonCompiler compiler, PyExpr expr) {
        if (expr.type(compiler).equals(Type.LONG_TYPE)) {
            cmpLong(mv, compiler);
        } else if (expr.type(compiler).equals(Type.DOUBLE_TYPE)) {
            cmpDouble(mv, compiler);
        } else if (expr.type(compiler).equals(Type.INT_TYPE)) {
            cmpInt(mv, compiler);
        } else if (expr.type(compiler).equals(Type.FLOAT_TYPE)) {
            cmpFloat(mv, compiler);
        } else if (expr.type(compiler).equals(Type.CHAR_TYPE)) {
            cmpInt(mv, compiler);
        } else if (expr.type(compiler).equals(Type.BYTE_TYPE)) {
            cmpInt(mv, compiler);
        } else if (expr.type(compiler).equals(Type.SHORT_TYPE)) {
            cmpInt(mv, compiler);
        } else if (expr.type(compiler).equals(Type.BOOLEAN_TYPE)) {
            cmpInt(mv, compiler);
        } else if (expr.type(compiler).equals(Type.getType(String.class))) {
            cmpString(mv, compiler);
        } else {
            cmpObject(mv, compiler);
        }
    }

    private void cmpString(MethodVisitor mv, PythonCompiler compiler) {
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);

        Context context = compiler.getContext(Context.class);
        if (context instanceof ConditionContext conditionContext) {
            if (comparator == Comparison.NE) {
                Label labelIfFalse = conditionContext.ifFalse();
                Label labelIfTrue = conditionContext.ifTrue();

                if (labelIfFalse != null) mv.visitJumpInsn(IFEQ, labelIfFalse);
                if (labelIfTrue != null) mv.visitJumpInsn(IFNE, labelIfTrue);
                return;
            }
        }

        if (comparator == Comparison.NE) {
            Label isFalse = new Label();
            Label end = new Label();

            // If value == 0 (false), jump to isFalse
            compiler.writer.jumpIfEqual(isFalse);

            // If we reach here, the value was 1 (true), so push 0 (false)
            mv.visitInsn(ICONST_0);
            compiler.writer.jump(end);

            // If value was 0 (false), we push 1 (true)
            compiler.writer.label(isFalse);
            mv.visitInsn(ICONST_1);

            context.push(Type.BOOLEAN_TYPE);

            compiler.writer.label(end);
        }
    }

    private void cmpFloat(MethodVisitor mv, PythonCompiler compiler) {
        Context context = compiler.getContext(Context.class);

        Type pop1 = context.pop();
        Type pop2 = context.pop();

        context.push(pop2);
        context.push(pop1);

        if (!pop1.equals(pop2)) {
            if (pop2 == Type.LONG_TYPE) {
                compiler.writer.swap();
                mv.visitInsn(L2D);
                compiler.writer.swap();
                mv.visitInsn(F2D);
            } else if (pop2 == Type.DOUBLE_TYPE) {
                mv.visitInsn(F2D);
            } else if (pop2 == Type.INT_TYPE) {
                mv.visitInsn(I2F);
            }
        }

        Label labelTrue = new Label();
        Label labelEnd = new Label();

        mv.visitInsn(FCMPG);
        if (context instanceof ConditionContext conditionContext) {
            if (comparator == Comparison.EQ) {
                Label labelIfFalse = conditionContext.ifFalse();
                Label labelIfTrue = conditionContext.ifTrue();
                context.pop();
                context.pop();
                if (labelIfFalse != null) mv.visitJumpInsn(IFEQ, labelIfFalse);
                if (labelIfTrue != null) mv.visitJumpInsn(IFNE, labelIfTrue);
                return;
            } else if (comparator == Comparison.NE) {
                Label labelIfFalse = conditionContext.ifFalse();
                Label labelIfTrue = conditionContext.ifTrue();
                context.pop();
                context.pop();
                if (labelIfFalse != null) mv.visitJumpInsn(IFNE, labelIfFalse);
                if (labelIfTrue != null) mv.visitJumpInsn(IFEQ, labelIfTrue);
                return;
            } else if (comparator == Comparison.LT) {
                Label labelIfFalse = conditionContext.ifFalse();
                Label labelIfTrue = conditionContext.ifTrue();
                context.pop();
                context.pop();
                if (labelIfFalse != null) mv.visitJumpInsn(IFGE, labelIfFalse);
                if (labelIfTrue != null) mv.visitJumpInsn(IFLT, labelIfTrue);
                return;
            } else if (comparator == Comparison.LTE) {
                Label labelIfFalse = conditionContext.ifFalse();
                Label labelIfTrue = conditionContext.ifTrue();
                context.pop();
                context.pop();
                if (labelIfFalse != null) mv.visitJumpInsn(IFGT, labelIfFalse);
                if (labelIfTrue != null) mv.visitJumpInsn(IFLE, labelIfTrue);
                return;
            } else if (comparator == Comparison.GT) {
                Label labelIfFalse = conditionContext.ifFalse();
                Label labelIfTrue = conditionContext.ifTrue();
                context.pop();
                context.pop();
                if (labelIfFalse != null) mv.visitJumpInsn(IFLE, labelIfFalse);
                if (labelIfTrue != null) mv.visitJumpInsn(IFGT, labelIfTrue);
                return;
            } else if (comparator == Comparison.GTE) {
                Label labelIfFalse = conditionContext.ifFalse();
                Label labelIfTrue = conditionContext.ifTrue();
                context.pop();
                context.pop();
                if (labelIfFalse != null) mv.visitJumpInsn(IFLT, labelIfFalse);
                if (labelIfTrue != null) mv.visitJumpInsn(IFGE, labelIfTrue);
                return;
            } else {
                throw new IllegalStateException("Unknown comparator: " + comparator);
            }
        } else if (comparator == Comparison.EQ) {
            compiler.writer.jumpIfEqual(labelTrue);
        } else if (comparator == Comparison.NE) {
            compiler.writer.jumpIfNotEqual(labelTrue);
        } else if (comparator == Comparison.LT) {
            compiler.writer.jumpIfLessThan(labelTrue);
        } else if (comparator == Comparison.LTE) {
            compiler.writer.jumpIfLessThanOrEqual(labelTrue);
        } else if (comparator == Comparison.GT) {
            compiler.writer.jumpIfGreaterThan(labelTrue);
        } else if (comparator == Comparison.GTE) {
            compiler.writer.jumpIfGreaterThanOrEqual(labelTrue);
        }

        mv.visitInsn(ICONST_0);
        compiler.writer.jump(labelEnd);

        compiler.writer.label(labelTrue);
        mv.visitInsn(ICONST_1);

        compiler.writer.label(labelEnd);
    }

    private void cmpLong(MethodVisitor mv, PythonCompiler compiler) {
        Context context = compiler.getContext(Context.class);

        Type pop1 = context.pop();
        Type pop2 = context.pop();

        context.push(pop2);
        context.push(pop1);

        if (!pop1.equals(pop2)) {
            if (pop2 == Type.INT_TYPE) {
                compiler.writer.swap();
                compiler.writer.cast(Type.LONG_TYPE);
                compiler.writer.swap();
            } else if (pop2 == Type.DOUBLE_TYPE) {
                compiler.writer.cast(Type.DOUBLE_TYPE);
            } else if (pop2 == Type.FLOAT_TYPE) {
                compiler.writer.swap();
                compiler.writer.cast(Type.DOUBLE_TYPE);
                compiler.writer.swap();
                compiler.writer.cast(Type.DOUBLE_TYPE);
            }
        }

        Label labelTrue = new Label();
        Label labelEnd = new Label();

        if (pop2 == Type.LONG_TYPE) mv.visitInsn(LCMP);
        else if (pop2 == Type.FLOAT_TYPE) mv.visitInsn(FCMPG);
        else if (pop2 == Type.DOUBLE_TYPE) mv.visitInsn(DCMPG);
        if (context instanceof ConditionContext conditionContext) {
            if (comparator == Comparison.EQ) {
                Label labelIfFalse = conditionContext.ifFalse();
                Label labelIfTrue = conditionContext.ifTrue();
                context.pop();
                context.pop();
                if (labelIfFalse != null) mv.visitJumpInsn(IFEQ, labelIfFalse);
                if (labelIfTrue != null) mv.visitJumpInsn(IFNE, labelIfTrue);
                return;
            } else if (comparator == Comparison.NE) {
                Label labelIfFalse = conditionContext.ifFalse();
                Label labelIfTrue = conditionContext.ifTrue();
                context.pop();
                context.pop();
                if (labelIfFalse != null) mv.visitJumpInsn(IFNE, labelIfFalse);
                if (labelIfTrue != null) mv.visitJumpInsn(IFEQ, labelIfTrue);
                return;
            } else if (comparator == Comparison.LT) {
                Label labelIfFalse = conditionContext.ifFalse();
                Label labelIfTrue = conditionContext.ifTrue();
                context.pop();
                context.pop();
                if (labelIfFalse != null) mv.visitJumpInsn(IFGE, labelIfFalse);
                if (labelIfTrue != null) mv.visitJumpInsn(IFLT, labelIfTrue);
                return;
            } else if (comparator == Comparison.LTE) {
                Label labelIfFalse = conditionContext.ifFalse();
                Label labelIfTrue = conditionContext.ifTrue();
                context.pop();
                context.pop();
                if (labelIfFalse != null) mv.visitJumpInsn(IFGT, labelIfFalse);
                if (labelIfTrue != null) mv.visitJumpInsn(IFLE, labelIfTrue);
                return;
            } else if (comparator == Comparison.GT) {
                Label labelIfFalse = conditionContext.ifFalse();
                Label labelIfTrue = conditionContext.ifTrue();
                context.pop();
                context.pop();
                if (labelIfFalse != null) mv.visitJumpInsn(IFLE, labelIfFalse);
                if (labelIfTrue != null) mv.visitJumpInsn(IFGT, labelIfTrue);
                return;
            } else if (comparator == Comparison.GTE) {
                Label labelIfFalse = conditionContext.ifFalse();
                Label labelIfTrue = conditionContext.ifTrue();
                context.pop();
                context.pop();
                if (labelIfFalse != null) mv.visitJumpInsn(IFLT, labelIfFalse);
                if (labelIfTrue != null) mv.visitJumpInsn(IFGE, labelIfTrue);
                return;
            } else {
                throw new RuntimeException("Unsupported comparator: " + comparator);
            }
        } else if (comparator == Comparison.EQ) {
            compiler.writer.jumpIfEqual(labelTrue);
        } else if (comparator == Comparison.NE) {
            compiler.writer.jumpIfNotEqual(labelTrue);
        } else if (comparator == Comparison.LT) {
            compiler.writer.jumpIfLessThan(labelTrue);
        } else if (comparator == Comparison.LTE) {
            compiler.writer.jumpIfLessThanOrEqual(labelTrue);
        } else if (comparator == Comparison.GT) {
            compiler.writer.jumpIfGreaterThan(labelTrue);
        } else if (comparator == Comparison.GTE) {
            compiler.writer.jumpIfGreaterThanOrEqual(labelTrue);
        }

        mv.visitInsn(ICONST_0);
        compiler.writer.jump(labelEnd);

        compiler.writer.label(labelTrue);
        mv.visitInsn(ICONST_1);

        context.push(Type.BOOLEAN_TYPE);


        compiler.writer.label(labelEnd);
    }

    private void cmpObject(MethodVisitor mv, PythonCompiler compiler) {
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/Objects", "equals", "(Ljava/lang/Object;)Z", false);
        if (comparator == Comparison.NE) {
            Label isFalse = new Label();
            Label end = new Label();

            // If value == 0 (false), jump to isFalse
            compiler.writer.jumpIfEqual(isFalse);

            // If we reach here, the value was 1 (true), so push 0 (false)
            mv.visitInsn(ICONST_0);
            compiler.writer.jump(end);

            // If value was 0 (false), we push 1 (true)
            compiler.writer.label(isFalse);
            mv.visitInsn(ICONST_1);

            compiler.writer.getContext().push(Type.BOOLEAN_TYPE);

            compiler.writer.label(end);
        }
    }

    private void cmpInt(MethodVisitor mv, PythonCompiler compiler) {
        Context context = compiler.getContext(Context.class);

        Type pop1 = context.pop();
        Type pop2 = context.pop();

        context.push(pop2);
        context.push(pop1);

        if (!pop1.equals(pop2)) {
            if (pop2 == Type.LONG_TYPE) {
                compiler.writer.cast(Type.LONG_TYPE);
            } else if (pop2 == Type.DOUBLE_TYPE) {
                compiler.writer.cast(Type.DOUBLE_TYPE);
            } else if (pop2 == Type.FLOAT_TYPE) {
                compiler.writer.cast(Type.FLOAT_TYPE);
            }

            Label labelTrue = new Label();
            Label labelEnd = new Label();

            if (pop2 == Type.LONG_TYPE) {
                mv.visitInsn(LCMP);
                if (context instanceof ConditionContext conditionContext) {
                    if (comparator == Comparison.EQ) {
                        Label labelIfFalse = conditionContext.ifFalse();
                        Label labelIfTrue = conditionContext.ifTrue();
                        context.pop();
                        context.pop();
                        if (labelIfFalse != null) mv.visitJumpInsn(IFEQ, labelIfFalse);
                        if (labelIfTrue != null) mv.visitJumpInsn(IFNE, labelIfTrue);
                        return;
                    } else if (comparator == Comparison.NE) {
                        Label labelIfFalse = conditionContext.ifFalse();
                        Label labelIfTrue = conditionContext.ifTrue();
                        context.pop();
                        context.pop();
                        if (labelIfFalse != null) mv.visitJumpInsn(IFNE, labelIfFalse);
                        if (labelIfTrue != null) mv.visitJumpInsn(IFEQ, labelIfTrue);
                        return;
                    } else if (comparator == Comparison.LT) {
                        Label labelIfFalse = conditionContext.ifFalse();
                        Label labelIfTrue = conditionContext.ifTrue();
                        context.pop();
                        context.pop();
                        if (labelIfFalse != null) mv.visitJumpInsn(IFGE, labelIfFalse);
                        if (labelIfTrue != null) mv.visitJumpInsn(IFLT, labelIfTrue);
                        return;
                    } else if (comparator == Comparison.LTE) {
                        Label labelIfFalse = conditionContext.ifFalse();
                        Label labelIfTrue = conditionContext.ifTrue();
                        context.pop();
                        context.pop();
                        if (labelIfFalse != null) mv.visitJumpInsn(IFGT, labelIfFalse);
                        if (labelIfTrue != null) mv.visitJumpInsn(IFLE, labelIfTrue);
                        return;
                    } else if (comparator == Comparison.GT) {
                        Label labelIfFalse = conditionContext.ifFalse();
                        Label labelIfTrue = conditionContext.ifTrue();
                        context.pop();
                        context.pop();
                        if (labelIfFalse != null) mv.visitJumpInsn(IFLE, labelIfFalse);
                        if (labelIfTrue != null) mv.visitJumpInsn(IFGT, labelIfTrue);
                        return;
                    } else if (comparator == Comparison.GTE) {
                        Label labelIfFalse = conditionContext.ifFalse();
                        Label labelIfTrue = conditionContext.ifTrue();
                        context.pop();
                        context.pop();
                        if (labelIfFalse != null) mv.visitJumpInsn(IFLT, labelIfFalse);
                        if (labelIfTrue != null) mv.visitJumpInsn(IFGE, labelIfTrue);
                        return;
                    } else {
                        throw new IllegalStateException("Unknown comparator: " + comparator);
                    }
                } else if (comparator == Comparison.EQ) {
                    compiler.writer.jumpIfEqual(labelTrue);
                } else if (comparator == Comparison.NE) {
                    compiler.writer.jumpIfNotEqual(labelTrue);
                } else if (comparator == Comparison.LT) {
                    compiler.writer.jumpIfLessThan(labelTrue);
                } else if (comparator == Comparison.LTE) {
                    compiler.writer.jumpIfLessThanOrEqual(labelTrue);
                } else if (comparator == Comparison.GT) {
                    compiler.writer.jumpIfGreaterThan(labelTrue);
                } else if (comparator == Comparison.GTE) {
                    compiler.writer.jumpIfGreaterThanOrEqual(labelTrue);
                }
            } else if (pop2 == Type.DOUBLE_TYPE) {
                mv.visitInsn(DCMPG);
                if (context instanceof ConditionContext conditionContext) {
                    if (comparator == Comparison.EQ) {
                        Label labelIfFalse = conditionContext.ifFalse();
                        Label labelIfTrue = conditionContext.ifTrue();

                        if (labelIfFalse != null) mv.visitJumpInsn(IFEQ, labelIfFalse);
                        if (labelIfTrue != null) mv.visitJumpInsn(IFNE, labelIfTrue);
                        return;
                    } else if (comparator == Comparison.NE) {
                        Label labelIfFalse = conditionContext.ifFalse();
                        Label labelIfTrue = conditionContext.ifTrue();

                        if (labelIfFalse != null) mv.visitJumpInsn(IFNE, labelIfFalse);
                        if (labelIfTrue != null) mv.visitJumpInsn(IFEQ, labelIfTrue);
                        return;
                    } else if (comparator == Comparison.LT) {
                        Label labelIfFalse = conditionContext.ifFalse();
                        Label labelIfTrue = conditionContext.ifTrue();

                        if (labelIfFalse != null) mv.visitJumpInsn(IFGE, labelIfFalse);
                        if (labelIfTrue != null) mv.visitJumpInsn(IFLT, labelIfTrue);
                        return;
                    } else if (comparator == Comparison.LTE) {
                        Label labelIfFalse = conditionContext.ifFalse();
                        Label labelIfTrue = conditionContext.ifTrue();

                        if (labelIfFalse != null) mv.visitJumpInsn(IFGT, labelIfFalse);
                        if (labelIfTrue != null) mv.visitJumpInsn(IFLE, labelIfTrue);
                        return;
                    } else if (comparator == Comparison.GT) {
                        Label labelIfFalse = conditionContext.ifFalse();
                        Label labelIfTrue = conditionContext.ifTrue();

                        if (labelIfFalse != null) mv.visitJumpInsn(IFLE, labelIfFalse);
                        if (labelIfTrue != null) mv.visitJumpInsn(IFGT, labelIfTrue);
                        return;
                    } else if (comparator == Comparison.GTE) {
                        Label labelIfFalse = conditionContext.ifFalse();
                        Label labelIfTrue = conditionContext.ifTrue();

                        if (labelIfFalse != null) mv.visitJumpInsn(IFLT, labelIfFalse);
                        if (labelIfTrue != null) mv.visitJumpInsn(IFGE, labelIfTrue);
                        return;
                    } else {
                        throw new IllegalStateException("Unknown comparator: " + comparator);
                    }
                } else if (comparator == Comparison.EQ) {
                    compiler.writer.jumpIfEqual(labelTrue);
                } else if (comparator == Comparison.NE) {
                    compiler.writer.jumpIfNotEqual(labelTrue);
                } else if (comparator == Comparison.LT) {
                    compiler.writer.jumpIfLessThan(labelTrue);
                } else if (comparator == Comparison.LTE) {
                    compiler.writer.jumpIfLessThanOrEqual(labelTrue);
                } else if (comparator == Comparison.GT) {
                    compiler.writer.jumpIfGreaterThan(labelTrue);
                } else if (comparator == Comparison.GTE) {
                    compiler.writer.jumpIfGreaterThanOrEqual(labelTrue);
                }
            } else if (pop2 == Type.FLOAT_TYPE) {
                mv.visitInsn(FCMPG);
                if (context instanceof ConditionContext conditionContext) {
                    if (comparator == Comparison.EQ) {
                        Label labelIfFalse = conditionContext.ifFalse();
                        Label labelIfTrue = conditionContext.ifTrue();
                        context.pop();
                        context.pop();
                        if (labelIfFalse != null) mv.visitJumpInsn(IFEQ, labelIfFalse);
                        if (labelIfTrue != null) mv.visitJumpInsn(IFNE, labelIfTrue);
                        return;
                    } else if (comparator == Comparison.NE) {
                        Label labelIfFalse = conditionContext.ifFalse();
                        Label labelIfTrue = conditionContext.ifTrue();
                        context.pop();
                        context.pop();
                        if (labelIfFalse != null) mv.visitJumpInsn(IFNE, labelIfFalse);
                        if (labelIfTrue != null) mv.visitJumpInsn(IFEQ, labelIfTrue);
                        return;
                    } else if (comparator == Comparison.LT) {
                        Label labelIfFalse = conditionContext.ifFalse();
                        Label labelIfTrue = conditionContext.ifTrue();
                        context.pop();
                        context.pop();
                        if (labelIfFalse != null) mv.visitJumpInsn(IFGE, labelIfFalse);
                        if (labelIfTrue != null) mv.visitJumpInsn(IFLT, labelIfTrue);
                        return;
                    } else if (comparator == Comparison.LTE) {
                        Label labelIfFalse = conditionContext.ifFalse();
                        Label labelIfTrue = conditionContext.ifTrue();
                        context.pop();
                        context.pop();
                        if (labelIfFalse != null) mv.visitJumpInsn(IFGT, labelIfFalse);
                        if (labelIfTrue != null) mv.visitJumpInsn(IFLE, labelIfTrue);
                        return;
                    } else if (comparator == Comparison.GT) {
                        Label labelIfFalse = conditionContext.ifFalse();
                        Label labelIfTrue = conditionContext.ifTrue();
                        context.pop();
                        context.pop();
                        if (labelIfFalse != null) mv.visitJumpInsn(IFLE, labelIfFalse);
                        if (labelIfTrue != null) mv.visitJumpInsn(IFGT, labelIfTrue);
                        return;
                    } else if (comparator == Comparison.GTE) {
                        Label labelIfFalse = conditionContext.ifFalse();
                        Label labelIfTrue = conditionContext.ifTrue();
                        context.pop();
                        context.pop();
                        if (labelIfFalse != null) mv.visitJumpInsn(IFLT, labelIfFalse);
                        if (labelIfTrue != null) mv.visitJumpInsn(IFGE, labelIfTrue);
                        return;
                    } else {
                        throw new IllegalStateException("Unknown comparator: " + comparator);
                    }
                } else if (comparator == Comparison.EQ) {
                    compiler.writer.jumpIfEqual(labelTrue);
                } else if (comparator == Comparison.NE) {
                    compiler.writer.jumpIfNotEqual(labelTrue);
                } else if (comparator == Comparison.LT) {
                    compiler.writer.jumpIfLessThan(labelTrue);
                } else if (comparator == Comparison.LTE) {
                    compiler.writer.jumpIfLessThanOrEqual(labelTrue);
                } else if (comparator == Comparison.GT) {
                    compiler.writer.jumpIfGreaterThan(labelTrue);
                } else if (comparator == Comparison.GTE) {
                    compiler.writer.jumpIfGreaterThanOrEqual(labelTrue);
                }

            } else {
                throw new IllegalStateException("Unknown type: " + pop2);
            }
        } else if (context instanceof ConditionContext conditionContext) {
            if (comparator == Comparison.EQ) {
                Label labelIfFalse = conditionContext.ifFalse();
                Label labelIfTrue = conditionContext.ifTrue();
                context.pop();
                context.pop();
                if (labelIfFalse != null) mv.visitJumpInsn(IFEQ, labelIfFalse);
                if (labelIfTrue != null) mv.visitJumpInsn(IFNE, labelIfTrue);
                return;
            } else if (comparator == Comparison.NE) {
                Label labelIfFalse = conditionContext.ifFalse();
                Label labelIfTrue = conditionContext.ifTrue();
                context.pop();
                context.pop();
                if (labelIfFalse != null) mv.visitJumpInsn(IFNE, labelIfFalse);
                if (labelIfTrue != null) mv.visitJumpInsn(IFEQ, labelIfTrue);
                return;
            } else if (comparator == Comparison.LT) {
                Label labelIfFalse = conditionContext.ifFalse();
                Label labelIfTrue = conditionContext.ifTrue();
                context.pop();
                context.pop();
                if (labelIfFalse != null) mv.visitJumpInsn(IFGE, labelIfFalse);
                if (labelIfTrue != null) mv.visitJumpInsn(IFLT, labelIfTrue);
                return;
            } else if (comparator == Comparison.LTE) {
                Label labelIfFalse = conditionContext.ifFalse();
                Label labelIfTrue = conditionContext.ifTrue();
                context.pop();
                context.pop();
                if (labelIfFalse != null) mv.visitJumpInsn(IFGT, labelIfFalse);
                if (labelIfTrue != null) mv.visitJumpInsn(IFLE, labelIfTrue);
                return;
            } else if (comparator == Comparison.GT) {
                Label labelIfFalse = conditionContext.ifFalse();
                Label labelIfTrue = conditionContext.ifTrue();
                context.pop();
                context.pop();
                if (labelIfFalse != null) mv.visitJumpInsn(IFLE, labelIfFalse);
                if (labelIfTrue != null) mv.visitJumpInsn(IFGT, labelIfTrue);
                return;
            } else if (comparator == Comparison.GTE) {
                Label labelIfFalse = conditionContext.ifFalse();
                Label labelIfTrue = conditionContext.ifTrue();
                context.pop();
                context.pop();
                if (labelIfFalse != null) mv.visitJumpInsn(IFLT, labelIfFalse);
                if (labelIfTrue != null) mv.visitJumpInsn(IFGE, labelIfTrue);
                return;
            } else {
                throw new IllegalStateException("Unknown comparator: " + comparator);
            }
        } else if (comparator == Comparison.EQ) {
            mv.visitInsn(Opcodes.IF_ICMPEQ);
        } else if (comparator == Comparison.NE) {
            mv.visitInsn(Opcodes.IF_ICMPNE);
        } else if (comparator == Comparison.LT) {
            mv.visitInsn(Opcodes.IF_ICMPLT);
        } else if (comparator == Comparison.LTE) {
            mv.visitInsn(Opcodes.IF_ICMPLE);
        } else if (comparator == Comparison.GT) {
            mv.visitInsn(Opcodes.IF_ICMPGT);
        } else if (comparator == Comparison.GTE) {
            mv.visitInsn(Opcodes.IF_ICMPGE);
        }
    }

    private void cmpDouble(MethodVisitor mv, PythonCompiler compiler) {
        Context context = compiler.getContext(Context.class);

        Type pop1 = context.pop();
        Type pop2 = context.pop();

        context.push(pop2);
        context.push(pop1);

        Label labelTrue = new Label();
        Label labelEnd = new Label();

        mv.visitInsn(DCMPG);

        if (context instanceof ConditionContext conditionContext) {
            if (comparator == Comparison.EQ) {
                Label labelIfFalse = conditionContext.ifFalse();
                Label labelIfTrue = conditionContext.ifTrue();
                context.pop();
                context.pop();
                if (labelIfFalse != null) mv.visitJumpInsn(IFEQ, labelIfFalse);
                if (labelIfTrue != null) mv.visitJumpInsn(IFNE, labelIfTrue);
                return;
            } else if (comparator == Comparison.NE) {
                Label labelIfFalse = conditionContext.ifFalse();
                Label labelIfTrue = conditionContext.ifTrue();
                context.pop();
                context.pop();
                if (labelIfFalse != null) mv.visitJumpInsn(IFNE, labelIfFalse);
                if (labelIfTrue != null) mv.visitJumpInsn(IFEQ, labelIfTrue);
                return;
            } else if (comparator == Comparison.LT) {
                Label labelIfFalse = conditionContext.ifFalse();
                Label labelIfTrue = conditionContext.ifTrue();
                context.pop();
                context.pop();
                if (labelIfFalse != null) mv.visitJumpInsn(IFGE, labelIfFalse);
                if (labelIfTrue != null) mv.visitJumpInsn(IFLT, labelIfTrue);
                return;
            } else if (comparator == Comparison.LTE) {
                Label labelIfFalse = conditionContext.ifFalse();
                Label labelIfTrue = conditionContext.ifTrue();
                context.pop();
                context.pop();
                if (labelIfFalse != null) mv.visitJumpInsn(IFGT, labelIfFalse);
                if (labelIfTrue != null) mv.visitJumpInsn(IFLE, labelIfTrue);
                return;
            } else if (comparator == Comparison.GT) {
                Label labelIfFalse = conditionContext.ifFalse();
                Label labelIfTrue = conditionContext.ifTrue();
                context.pop();
                context.pop();
                if (labelIfFalse != null) mv.visitJumpInsn(IFLE, labelIfFalse);
                if (labelIfTrue != null) mv.visitJumpInsn(IFGT, labelIfTrue);
                return;
            } else if (comparator == Comparison.GTE) {
                Label labelIfFalse = conditionContext.ifFalse();
                Label labelIfTrue = conditionContext.ifTrue();
                context.pop();
                context.pop();
                if (labelIfFalse != null) mv.visitJumpInsn(IFLT, labelIfFalse);
                if (labelIfTrue != null) mv.visitJumpInsn(IFGE, labelIfTrue);
                return;
            } else {
                throw new IllegalStateException("Unknown comparator: " + comparator);
            }
        } else if (comparator == Comparison.EQ) {
            compiler.writer.jumpIfEqual(labelTrue);
        } else if (comparator == Comparison.NE) {
            compiler.writer.jumpIfNotEqual(labelTrue);
        } else if (comparator == Comparison.LT) {
            compiler.writer.jumpIfLessThan(labelTrue);
        } else if (comparator == Comparison.LTE) {
            compiler.writer.jumpIfLessThanOrEqual(labelTrue);
        } else if (comparator == Comparison.GT) {
            compiler.writer.jumpIfGreaterThan(labelTrue);
        } else if (comparator == Comparison.GTE) {
            compiler.writer.jumpIfGreaterThanOrEqual(labelTrue);
        } else {
            throw new RuntimeException("Unknown comparator: " + comparator);
        }

        mv.visitInsn(ICONST_0);
        compiler.writer.jump(labelEnd);

        compiler.writer.label(labelTrue);
        mv.visitInsn(ICONST_1);

        context.push(Type.BOOLEAN_TYPE);

        compiler.writer.label(labelEnd);
    }

    @Override
    public int lineNo() {
        return 0;
    }

    @Override
    public Type type(PythonCompiler compiler) {
        return Type.BOOLEAN_TYPE;
    }
}
