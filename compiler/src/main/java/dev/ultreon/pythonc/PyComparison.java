package dev.ultreon.pythonc;

import org.antlr.v4.runtime.ParserRuleContext;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

class PyComparison implements PyExpr {
    private final ParserRuleContext context;
    private final Comparison comparator;
    private final PythonParser.Compare_op_bitwise_or_pairContext ctx;
    private final Location location;

    public enum Comparison {
        EQ, NE, LT, LTE, GT, NOT_IN, IN, IS, IS_NOT, GTE
    }

    public PyComparison(ParserRuleContext context, Comparison comparator, PythonParser.Compare_op_bitwise_or_pairContext ctx, Location location) {
        this.context = context;
        this.comparator = comparator;
        this.ctx = ctx;
        this.location = location;
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
            case PythonParser.In_bitwise_orContext ctx -> ctx.bitwise_or();
            case PythonParser.Notin_bitwise_orContext ctx -> ctx.bitwise_or();
            case PythonParser.Is_bitwise_orContext ctx -> ctx.bitwise_or();
            case PythonParser.Isnot_bitwise_orContext ctx -> ctx.bitwise_or();
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
                }
                case Integer s -> {
                    compiler.writer.loadConstant(s);
                    compiler.writer.cast(Type.getType(Object.class));
                }
                case Float s -> {
                    compiler.writer.loadConstant(s);
                    compiler.writer.cast(Type.getType(Object.class));
                }
                case Long s -> {
                    compiler.writer.loadConstant(s);
                    compiler.writer.cast(Type.getType(Object.class));
                }
                case Double s -> {
                    compiler.writer.loadConstant(s);
                    compiler.writer.cast(Type.getType(Object.class));
                }
                case Character s -> {
                    compiler.writer.loadConstant(s);
                    compiler.writer.cast(Type.getType(Object.class));
                }
                case Byte s -> {
                    compiler.writer.loadConstant(s);
                    compiler.writer.cast(Type.getType(Object.class));
                }
                case Short s -> {
                    compiler.writer.loadConstant(s);
                    compiler.writer.cast(Type.getType(Object.class));
                }
                case Boolean s -> {
                    compiler.writer.loadConstant(s);
                    compiler.writer.cast(Type.getType(Object.class));
                }
                case PyExpr expr -> {
                    expr.load(mv, compiler, expr.preload(mv, compiler, false), false);
                    compiler.writer.cast(Type.getType(Object.class));
                }
                case null, default -> throw new UnsupportedOperationException("Not implemented: " + visit.getClass());
            }

            Context context = compiler.getContext(Context.class);
            context.push(Type.getType(Object.class));

            switch (comparator) {
                case EQ -> compiler.writer.dynamicEq();
                case NE -> compiler.writer.dynamicNe();
                case LT -> compiler.writer.dynamicLt();
                case LTE -> compiler.writer.dynamicLe();
                case GT -> compiler.writer.dynamicGt();
                case GTE -> compiler.writer.dynamicGe();
                case IN -> compiler.writer.dynamicIn();
                case NOT_IN -> compiler.writer.dynamicNotIn();
                case IS -> compiler.writer.dynamicIs();
                case IS_NOT -> compiler.writer.dynamicIsNot();
            }

            return;
        }
        throw new RuntimeException("compare_op_bitwise_or_pair not supported for:\n" + ctx.getText());
    }

    @Override
    public void expectReturnType(PythonCompiler compiler, JvmClass returnType, Location location) {
        PythonCompiler.classCache.require(compiler, Type.BOOLEAN_TYPE).expectReturnType(compiler, returnType, location);
    }

    @Override
    public Type type(PythonCompiler compiler) {
        return Type.BOOLEAN_TYPE;
    }

    @Override
    public Location location() {
        return location;
    }
}
