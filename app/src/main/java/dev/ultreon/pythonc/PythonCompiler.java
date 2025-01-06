package dev.ultreon.pythonc;

import com.google.common.base.CaseFormat;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import static org.objectweb.asm.Opcodes.*;

@SuppressWarnings("t")
public class PythonCompiler extends PythonParserBaseVisitor<Object> {
    public static final int F_CPL_FUNCTION = 0;
    public static final int F_CPL_CLASS = 1;
    public static final int F_CPL_DECORATOR = 2;
    public static final int F_CPL_IMPORT = 3;
    public static final int F_CPL_INSTANCE_FUNC = 4;
    public static final int F_CPL_STATIC_FUNC = 5;
    public static final int F_CPL_CLASS_FUNC = 6;

    private ClassWriter rootCw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    private ClassWriter cw = rootCw;
    private String path = "";
    private String fileName = "Main";
    private Map<String, String> imports = new HashMap<>();
    private State state = State.File;
    private BitSet flags = new BitSet();
    private Decorators decorators = new Decorators();
    private MethodVisitor mv;
    private int currentVariableIndex = 0;
    private Set<String> implementing = new HashSet<>();

    enum State {
        File,
        Class,
        Function,
        Decorators
    }

    @Override
    public Object visit(@NonNull ParseTree tree) {
        System.out.println("Visiting: " + tree.getClass().getSimpleName() + " " + tree.getText());

        Object visit = super.visit(tree);
        if (visit == null) {
            String simpleName = tree.getClass().getSimpleName();
            throw new RuntimeException("Visit unavailable for visit" + simpleName.substring(0, simpleName.length() - "Context".length()) + ":\n" + tree.getText());
        }
        return visit;
    }

    @Override
    public Object visitStatements(PythonParser.StatementsContext ctx) {
        for (int i = 0; i < ctx.statement().size(); i++) {
            Object visit = visit(ctx.statement(i));
            switch (visit) {
                case null -> throw new RuntimeException("statement not supported for:\n" + ctx.getText());
                case Unit unit -> {

                }
                case Decorators decorators1 -> {
                    throw new RuntimeException("statement not supported for:\n" + ctx.getText());
                }
                case FuncCallWithArgs funcCallWithArgs -> {
                    throw new RuntimeException("statement not supported for:\n" + ctx.getText());
                }
                case List<?> list -> {
                    for (Object o : list) {
                        if (o instanceof FuncCallWithArgs funcCallWithArgs) {
                            if (mv == null) {
                                throw new RuntimeException("Not writing a method:\n" + ctx.getText());
                            }

                            funcCallWithArgs.write(mv, this);
                        } else if (o instanceof List<?> list1) {
                            for (Object o1 : list1) {
                                if (o1 instanceof FuncCallWithArgs funcCallWithArgs) {
                                    if (mv == null) {
                                        throw new RuntimeException("Not writing a method:\n" + ctx.getText());
                                    }

                                    funcCallWithArgs.write(mv, this);
                                }
                            }
                        } else {
                            throw new RuntimeException("statement not supported for: " + o.getClass().getSimpleName());
                        }
                    }
                }
                default -> {
                    throw new RuntimeException("statement not supported for:\n" + ctx.getText());
                }
            }

        }
        return Unit.Instance;
    }

    @Override
    public Object visitStatement(PythonParser.StatementContext ctx) {
        PythonParser.Compound_stmtContext compoundStmtContext = ctx.compound_stmt();
        PythonParser.Simple_stmtsContext simpleStmtsContext = ctx.simple_stmts();
        if (simpleStmtsContext != null) {
            return visit(simpleStmtsContext);
        }
        return visit(compoundStmtContext);
    }

    @Override
    public Object visitCompound_stmt(PythonParser.Compound_stmtContext ctx) {
        PythonParser.Class_defContext classDefContext = ctx.class_def();
        if (classDefContext != null) {
            return visit(classDefContext);
        }

        PythonParser.Function_defContext functionDefContext = ctx.function_def();
        if (functionDefContext != null) {
            return visit(functionDefContext);
        }
        throw new RuntimeException("No supported matching compound_stmt found for:\n" + ctx.getText());
    }


    @Override
    public Object visitFunction_def(PythonParser.Function_defContext ctx) {
        flags.set(F_CPL_FUNCTION);
        try {
            PythonParser.DecoratorsContext decorators = ctx.decorators();
            if (decorators != null) {
                Object visit = visit(decorators);
                if (visit == null) {
                    throw new RuntimeException("Decorators not supported for:\n" + ctx.getText());
                }
            }
            PythonParser.Function_def_rawContext functionDefRawContext = ctx.function_def_raw();
            if (functionDefRawContext != null) {
                return visit(functionDefRawContext);
            }
            throw new RuntimeException("No supported matching function_def_raw found for:\n" + ctx.getText());
        } finally {
            flags.clear(F_CPL_FUNCTION);
        }
    }

    @Override
    public Object visitFunction_def_raw(PythonParser.Function_def_rawContext ctx) {
        TerminalNode def = ctx.DEF();
        if (def == null) {
            throw new RuntimeException("No DEF found for:\n" + ctx.getText());
        }

        TerminalNode name = ctx.NAME();
        if (name == null) {
            throw new RuntimeException("No NAME found for:\n" + ctx.getText());
        }

        PythonParser.@Nullable ParamsContext params = ctx.params();

        PythonParser.BlockContext block = ctx.block();
        if (block == null) {
            throw new RuntimeException("No block found for:\n" + ctx.getText());
        }

        if (decorators.byJvmName.containsKey("pythonvm/utils/Override")) {
            // Ignore for now
        }

        boolean static_ = decorators.byJvmName.containsKey("python/builtins/Staticmethod") || cw == rootCw;
        boolean class_ = decorators.byJvmName.containsKey("python/builtins/Classmethod");

        if (static_) {
            flags.set(F_CPL_STATIC_FUNC);
        } else if (class_) {
            flags.set(F_CPL_CLASS_FUNC);
        } else {
            flags.set(F_CPL_INSTANCE_FUNC);
        }

        List<TypedName> parmeters = new ArrayList<>();
        if (params != null) {
            Object visit = visit(params);
            if (visit == null) {
                throw new RuntimeException("params not supported for:\n" + ctx.getText());
            }
            if (!(visit instanceof List<?> list)) {
                throw new RuntimeException("Not a list:\n" + visit.getClass().getSimpleName());
            }
            for (Object o : list) {
                if (!(o instanceof TypedName typedName)) {
                    throw new RuntimeException("Not a typed name:\n" + ctx.getText());
                }
                parmeters.add(typedName);
            }
        }

        StringBuilder signature = new StringBuilder();
        for (TypedName typedName : parmeters) {
            String s = imports.get(typedName.type);
            if (s == null) {
                throw new RuntimeException("No import found for:\n" + ctx.getText());
            }
            signature.append(s).append("L").append(typedName.name).append(";");
        }
        String sig = "";
        if (!signature.isEmpty())
            sig = signature.substring(0, signature.length() - 1);

        if (name.getText().startsWith("__"))
            mv = cw.visitMethod(ACC_PRIVATE + (static_ ? ACC_STATIC : 0) + (class_ ? ACC_STATIC : 0), name.getText().substring(2), "(" + sig + ")V", null, null);
        else if (name.getText().startsWith("_"))
            mv = cw.visitMethod(ACC_PROTECTED + (static_ ? ACC_STATIC : 0) + (class_ ? ACC_STATIC : 0), name.getText().substring(1), "(" + sig + ")V", null, null);
        else
            mv = cw.visitMethod(ACC_PUBLIC + (static_ ? ACC_STATIC : 0) + (class_ ? ACC_STATIC : 0), name.getText(), "()V", null, null);

        try {
            mv.visitCode();

            if (decorators.byJvmName.containsKey("pythonvm/utils/Override")) {
                // Ignore for now
            }

            mv.visitLineNumber(ctx.getStart().getLine(), new Label());
            visit(block);

            mv.visitLineNumber(ctx.getStop().getLine(), new Label());
            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        } finally {
            flags.clear(F_CPL_STATIC_FUNC);
            flags.clear(F_CPL_CLASS_FUNC);
            flags.clear(F_CPL_INSTANCE_FUNC);
        }

        return Unit.Instance;
    }

    @Override
    public Object visitBlock(PythonParser.BlockContext ctx) {
        PythonParser.StatementsContext statementsContext = ctx.statements();
        if (statementsContext != null) {
            return visit(statementsContext);
        }
        throw new RuntimeException("No supported matching statements found for:\n" + ctx.getText());
    }

    @Override
    public Object visitParams(PythonParser.ParamsContext ctx) {
        return visit(ctx.parameters());
    }

    @Override
    public Object visitParameters(PythonParser.ParametersContext ctx) {
        List<TypedName> typedNames = new ArrayList<>();
        for (int i = 0; i < ctx.param_no_default().size(); i++) {
            Object visit = visit(ctx.param_no_default(i));
            if (visit == null) {
                throw new RuntimeException("param_no_default not supported for:\n" + ctx.getText());
            }

            if (!(visit instanceof TypedName typedName)) {
                if (visit instanceof Self self) {
                    continue;
                }
                throw new RuntimeException("param_no_default not supported for:\n" + ctx.getText());
            }

            typedNames.add(typedName);

            if (flags.get(F_CPL_FUNCTION)) {
                if (visit.equals("self")) {
                    if (!flags.get(F_CPL_INSTANCE_FUNC)) {
                        throw new CompilerException("self on a non-instance method:\n" + ctx.getText());
                    } else {
                        return Unit.Instance;
                    }
                }
            } else {
                throw new RuntimeException("param_no_default not supported for:\n" + ctx.getText());
            }
        }

        for (int i = 0; i < ctx.param_with_default().size(); i++) {
            throw new RuntimeException("param_with_default not supported for:\n" + ctx.getText());
        }
        return typedNames;
    }

    @Override
    public Object visitParam_no_default(PythonParser.Param_no_defaultContext ctx) {
        PythonParser.ParamContext param = ctx.param();
        TerminalNode terminalNode = ctx.TYPE_COMMENT();
        if (visit(param).equals("self")) {
            if (!flags.get(F_CPL_INSTANCE_FUNC)) {
                throw new CompilerException("self on a non-instance method:\n" + ctx.getText());
            } else {
                return new Self(param.getStart().getLine());
            }
        }
        if (terminalNode != null) {
            Object visit = visit(param);
            if (visit == null) {
                throw new RuntimeException("param not supported for:\n" + ctx.getText());
            }
            if (!(visit instanceof TypedName typedName)) {
                throw new RuntimeException("Not a typed name:\n" + ctx.getText());
            }
            return visit;
        }

        return visit(param);
    }

    @Override
    public Object visitParam(PythonParser.ParamContext ctx) {
        PythonParser.AnnotationContext annotation = ctx.annotation();
        if (annotation != null) {
            throw new RuntimeException("annotation not supported for:\n" + ctx.getText());
        }

        TerminalNode name = ctx.NAME();
        if (name == null) {
            throw new RuntimeException("No NAME found for:\n" + ctx.getText());
        }
        return name.getText();
    }

    @Override
    public Object visitClass_def(PythonParser.Class_defContext ctx) {
        PythonParser.DecoratorsContext decorators = ctx.decorators();
        if (decorators != null) {
            Object visit = visit(decorators);

            if (visit instanceof List<?> list) {
                for (Object o : list) {
                    if (o instanceof FuncCallWithArgs(
                            PythonParser.PrimaryContext atom, PythonParser.PrimaryContext primaryContext,
                            PythonParser.ArgumentsContext arguments
                    )) if (arguments != null) {
                        String text = primaryContext.getText();
                        if (text.equals("implements")) {
                            Object visit1 = visit(arguments);
                            if (visit1 == null) {
                                throw new RuntimeException("Decorators not supported for:\n" + ctx.getText());
                            }
                            if (visit1 instanceof List<?> list1) {
                                for (Object o1 : list1) {
                                    if (o1 instanceof PyObjectRef(String name, int lineNo)) {
                                        implementing.add(imports.get(name));
                                    } else {
                                        throw new CompilerException("Invalid implements(...) decorator:\n" + ctx.getText());
                                    }
                                }
                            } else {
                                throw new CompilerException("Invalid implements(...) decorator:\n" + ctx.getText());
                            }
                        }
                    }
                }
            }
        }
        PythonParser.Class_def_rawContext classDefRawContext = ctx.class_def_raw();
        if (classDefRawContext != null) {
            return visit(classDefRawContext);
        }
        throw new RuntimeException("No supported matching class_def found for:\n" + ctx.getText());
    }

    @Override
    public Object visitClass_def_raw(PythonParser.Class_def_rawContext ctx) {
        TerminalNode name = ctx.NAME();
        cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(V1_8, ACC_PUBLIC, path + name.getText(), null, "java/lang/Object", implementing == null ? new String[0] : implementing.isEmpty() ? null : implementing.toArray(new String[0]));
        Object visit = visit(ctx.block());
        if (visit == null) {
            throw new RuntimeException("block not supported for:\n" + ctx.getText());
        }
        cw.visitEnd();
        byte[] bytes = cw.toByteArray();
        try {
            String s = "build/pythonc/" + path;
            if (!new File(s).exists()) {
                new File(s).mkdirs();
            }
            FileOutputStream fileOutputStream = new FileOutputStream("build/pythonc/" + path + name.getText() + ".class");
            fileOutputStream.write(bytes);
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        cw = rootCw;
        return Unit.Instance;
    }

    @Override
    public Object visitDecorators(PythonParser.DecoratorsContext ctx) {
        List<PythonParser.Named_expressionContext> namedExpressionContexts = ctx.named_expression();
        var oldState = state;
        state = State.Decorators;
        List<Object> visit = new ArrayList<>();
        for (int i = 0; i < namedExpressionContexts.size(); i++) {
            visit.add(visit(namedExpressionContexts.get(i)));
        }
        if (state != State.Decorators) {
            throw new RuntimeException("Not in decorator state!");
        }
        state = oldState;
        return visit;
    }

    @Override
    public Object visitNamed_expression(PythonParser.Named_expressionContext ctx) {
        PythonParser.ExpressionContext expression = ctx.expression();
        if (expression != null) {
            return visit(expression);
        }
        PythonParser.Assignment_expressionContext assignmentExpressionContext = ctx.assignment_expression();
        if (assignmentExpressionContext != null) {
            return visit(assignmentExpressionContext);
        }
        throw new RuntimeException("No supported matching named_expression found for:\n" + ctx.getText());
    }

    @Override
    public Object visitSimple_stmts(PythonParser.Simple_stmtsContext ctx) {
        List<Object> visit = new ArrayList<>();
        for (int i = 0; i < ctx.simple_stmt().size(); i++) {
            Object visit1 = visit(ctx.simple_stmt(i));
            if (visit1 == null) {
                throw new RuntimeException("simple_stmt not supported for:\n" + ctx.getText());
            }
            visit.add(visit1);
        }
        return visit;
    }

    @Override
    public Object visitSimple_stmt(PythonParser.Simple_stmtContext ctx) {
        PythonParser.Import_stmtContext importStmtContext = ctx.import_stmt();
        if (importStmtContext != null) {
            return visit(importStmtContext);
        }
        PythonParser.Del_stmtContext delStmtContext = ctx.del_stmt();
        if (delStmtContext != null) {
            throw new RuntimeException("del_stmt not supported for:\n" + delStmtContext.getText());
        }

        PythonParser.Global_stmtContext globalStmtContext = ctx.global_stmt();
        if (globalStmtContext != null) {
            throw new RuntimeException("global_stmt not supported for:\n" + globalStmtContext.getText());
        }
        PythonParser.Nonlocal_stmtContext nonlocalStmtContext = ctx.nonlocal_stmt();
        if (nonlocalStmtContext != null) {
            throw new RuntimeException("nonlocal_stmt not supported for:\n" + nonlocalStmtContext.getText());
        }
        PythonParser.Assert_stmtContext assertStmtContext = ctx.assert_stmt();
        if (assertStmtContext != null) {
            throw new RuntimeException("assert_stmt not supported for:\n" + assertStmtContext.getText());
        }

        PythonParser.Star_expressionsContext starExpressionsContext = ctx.star_expressions();
        if (starExpressionsContext != null) {
            return visit(starExpressionsContext);
        }
        throw new RuntimeException("No supported matching simple_stmt found of type " + ctx.getClass().getSimpleName() + " for:\n" + ctx.getText());
    }

    @Override
    public Object visitStar_expressions(PythonParser.Star_expressionsContext ctx) {
        if (ctx.star_expression().size() == 1) {
            return visit(ctx.star_expression(0));
        } else {
            throw new RuntimeException("star_expressions not supported for:\n" + ctx.getText());
        }
    }

    @Override
    public Object visitStar_expression(PythonParser.Star_expressionContext ctx) {
        PythonParser.Bitwise_orContext bitwiseOrContext = ctx.bitwise_or();
        if (ctx.STAR() != null) {
            throw new RuntimeException("star_expression not supported for:\n" + ctx.getText());
        }
        if (bitwiseOrContext != null) {
            return visit(bitwiseOrContext);
        }
        PythonParser.ExpressionContext expression = ctx.expression();
        if (expression != null) {
            return visit(expression);
        }
        throw new RuntimeException("No supported matching star_expression found for:\n" + ctx.getText());
    }

    @Override
    public Object visitBitwise_or(PythonParser.Bitwise_orContext ctx) {
        PythonParser.Bitwise_xorContext bitwiseXorContext = ctx.bitwise_xor();
        if (ctx.VBAR() != null) {
            throw new RuntimeException("bitwise_or not supported for:\n" + ctx.getText());
        }
        if (bitwiseXorContext != null) {
            return visit(bitwiseXorContext);
        }
        throw new RuntimeException("No supported matching bitwise_or found for:\n" + ctx.getText());
    }

    @Override
    public Object visitBitwise_xor(PythonParser.Bitwise_xorContext ctx) {
        PythonParser.Bitwise_andContext andExprContext = ctx.bitwise_and();
        if (ctx.CIRCUMFLEX() != null) {
            throw new RuntimeException("bitwise_xor not supported for:\n" + ctx.getText());
        }
        PythonParser.Bitwise_xorContext bitwiseXorContext = ctx.bitwise_xor();
        if (ctx.bitwise_xor() != null) {
            throw new RuntimeException("bitwise_xor not supported for:\n" + ctx.getText());
        }
        if (andExprContext != null) {
            return visit(andExprContext);
        }
        throw new RuntimeException("No supported matching bitwise_xor found for:\n" + ctx.getText());
    }

    @Override
    public Object visitConjunction(PythonParser.ConjunctionContext ctx) {
        List<PythonParser.InversionContext> inversionContext = ctx.inversion();
        if (!ctx.AND().isEmpty()) {
            throw new RuntimeException("conjunction not supported for:\n" + ctx.getText());
        }
        if (inversionContext.size() == 1) {
            return visit(inversionContext.getFirst());
        }
        throw new RuntimeException("No supported matching conjunction found for:\n" + ctx.getText());
    }

    @Override
    public Object visitDisjunction(PythonParser.DisjunctionContext ctx) {
        List<PythonParser.ConjunctionContext> conjunctionContext = ctx.conjunction();
        if (!ctx.OR().isEmpty()) {
            throw new RuntimeException("disjunction not supported for:\n" + ctx.getText());
        }
        if (conjunctionContext.size() == 1) {
            return visit(conjunctionContext.getFirst());
        }
        throw new RuntimeException("No supported matching disjunction found for:\n" + ctx.getText());
    }

    @Override
    public Object visitExpression(PythonParser.ExpressionContext ctx) {
        List<PythonParser.DisjunctionContext> disjunction = ctx.disjunction();
        if (disjunction.size() == 1) {
            return visit(disjunction.getFirst());
        }

        throw new RuntimeException("No supported matching expression found for:\n" + ctx.getText());
    }

    @Override
    public Object visitInversion(PythonParser.InversionContext ctx) {
        PythonParser.InversionContext inversion = ctx.inversion();
        if (ctx.NOT() != null) {
            throw new RuntimeException("inversion not supported for:\n" + ctx.getText());
        }
        PythonParser.ComparisonContext comparison = ctx.comparison();
        if (comparison != null) {
            return visit(comparison);
        }
        throw new RuntimeException("No supported matching inversion found for:\n" + ctx.getText());
    }

    @Override
    public Object visitComparison(PythonParser.ComparisonContext ctx) {
        PythonParser.Bitwise_orContext bitwiseOrContext = ctx.bitwise_or();
        if (bitwiseOrContext != null) {
            return visit(bitwiseOrContext);
        }

        throw new RuntimeException("No supported matching comparison found for:\n" + ctx.getText());
    }


    @Override
    public Object visitBitwise_and(PythonParser.Bitwise_andContext ctx) {
        PythonParser.Bitwise_andContext bitwiseAndContext = ctx.bitwise_and();
        if (ctx.AMPER() != null) {
            throw new RuntimeException("bitwise_and not supported for:\n" + ctx.getText());
        }
        if (bitwiseAndContext != null) {
            throw new RuntimeException("bitwise_and not supported for:\n" + ctx.getText());
        }
        PythonParser.Shift_exprContext shiftExprContext = ctx.shift_expr();
        if (shiftExprContext != null) {
            return visit(shiftExprContext);
        }
        throw new RuntimeException("No supported matching bitwise_and found for:\n" + ctx.getText());
    }

    @Override
    public Object visitShift_expr(PythonParser.Shift_exprContext ctx) {
        PythonParser.Shift_exprContext shiftExprContext = ctx.shift_expr();
        if (ctx.LEFTSHIFT() != null || ctx.RIGHTSHIFT() != null) {
            throw new RuntimeException("shift_expr not supported for:\n" + ctx.getText());
        }
        if (ctx.RIGHTSHIFT() != null) {
            throw new RuntimeException("shift_expr not supported for:\n" + ctx.getText());
        }
        if (shiftExprContext != null) {
            throw new RuntimeException("shift_expr not supported for:\n" + ctx.getText());
        }
        PythonParser.SumContext sum = ctx.sum();
        if (sum != null) {
            return visit(sum);
        }
        throw new RuntimeException("No supported matching shift_expr found for:\n" + ctx.getText());
    }

    @Override
    public Object visitSum(PythonParser.SumContext ctx) {
        PythonParser.SumContext sumContext = ctx.sum();
        if (ctx.PLUS() != null) {
            throw new RuntimeException("sum not supported for:\n" + ctx.getText());
        }
        if (ctx.MINUS() != null) {
            throw new RuntimeException("sum not supported for:\n" + ctx.getText());
        }
        if (sumContext != null) {
            throw new RuntimeException("sum not supported for:\n" + ctx.getText());
        }
        PythonParser.TermContext termContext = ctx.term();
        if (termContext != null) {
            return visit(termContext);
        }
        throw new RuntimeException("No supported matching sum found for:\n" + ctx.getText());
    }

    @Override
    public Object visitTerm(PythonParser.TermContext ctx) {
        PythonParser.TermContext termContext = ctx.term();
        if (ctx.STAR() != null) {
            throw new RuntimeException("term not supported for:\n" + ctx.getText());
        }
        if (ctx.SLASH() != null) {
            throw new RuntimeException("term not supported for:\n" + ctx.getText());
        }
        if (termContext != null) {
            throw new RuntimeException("term not supported for:\n" + ctx.getText());
        }
        PythonParser.FactorContext factorContext = ctx.factor();
        if (factorContext != null) {
            return visit(factorContext);
        }
        throw new RuntimeException("No supported matching term found for:\n" + ctx.getText());
    }

    @Override
    public Object visitFactor(PythonParser.FactorContext ctx) {
        PythonParser.FactorContext factor = ctx.factor();
        if (ctx.MINUS() != null) {
            throw new RuntimeException("factor not supported for:\n" + ctx.getText());
        }

        if (ctx.PLUS() != null) {
            throw new RuntimeException("factor not supported for:\n" + ctx.getText());
        }

        if (ctx.TILDE() != null) {
            throw new RuntimeException("factor not supported for:\n" + ctx.getText());
        }
        PythonParser.PowerContext powerContext = ctx.power();
        if (powerContext != null) {
            return visit(powerContext);
        }
        throw new RuntimeException("No supported matching factor found for:\n" + ctx.getText());
    }

    @Override
    public Object visitPower(PythonParser.PowerContext ctx) {
        if (ctx.DOUBLESTAR() != null) {
            throw new RuntimeException("power not supported for:\n" + ctx.getText());
        }
        PythonParser.FactorContext factorContext = ctx.factor();
        if (factorContext != null) {
            return visit(factorContext);
        }

        PythonParser.Await_primaryContext awaitPrimaryContext = ctx.await_primary();
        if (awaitPrimaryContext != null) {
            return visit(awaitPrimaryContext);
        }
        throw new RuntimeException("No supported matching power found for:\n" + ctx.getText());
    }

    @Override
    public Object visitAwait_primary(PythonParser.Await_primaryContext ctx) {
        PythonParser.PrimaryContext primaryContext = ctx.primary();
        if (primaryContext != null) {
            return visit(primaryContext);
        }
        throw new RuntimeException("No supported matching await_primary found for:\n" + ctx.getText());
    }

    @Override
    public Object visitT_primary(PythonParser.T_primaryContext ctx) {
        PythonParser.AtomContext atom = ctx.atom();
        PythonParser.T_primaryContext tPrimaryContext = ctx.t_primary();
        if (tPrimaryContext != null) {
            throw new RuntimeException("t_primary not supported for:\n" + ctx.getText());
        }
        PythonParser.ArgumentsContext arguments = ctx.arguments();
        if (arguments != null) {
            throw new RuntimeException("t_primary not supported for:\n" + ctx.getText());
        }
        if (atom != null) {
            return visit(atom);
        }
        throw new RuntimeException("No supported matching t_primary found for:\n" + ctx.getText());
    }

    @Override
    public Object visitPrimary(PythonParser.PrimaryContext ctx) {
        PythonParser.PrimaryContext primaryContext = ctx.primary();
        if (ctx.LPAR() != null) {
            if (primaryContext == null) {
                throw new RuntimeException("primary not supported for:\n" + ctx.getText());
            }
            PythonParser.ArgumentsContext arguments = ctx.arguments();
            if (arguments != null) {
                PythonParser.PrimaryContext primary = ctx.primary();
                if (primary == null) {
                    throw new RuntimeException("primary not supported for:\n" + ctx.getText());
                }
                return new FuncCallWithArgs(primary, primaryContext, arguments);
            } else {
                PythonParser.PrimaryContext primary = ctx.primary();
                if (primary == null) {
                    throw new RuntimeException("primary not supported for:\n" + ctx.getText());
                }
                return new FuncCallWithArgs(primary, primaryContext, null);
            }
        } else {
            PythonParser.ArgumentsContext arguments = ctx.arguments();
            if (arguments != null) {
                throw new RuntimeException("primary not supported for:\n" + ctx.getText());
            }
        }
        if (ctx.RPAR() != null) {
            throw new RuntimeException("primary not supported for:\n" + ctx.getText());
        }
        if (primaryContext != null) {
            return visit(primaryContext);
        }

        PythonParser.AtomContext atom = ctx.atom();
        if (atom != null) {
            return visit(atom);
        }
        throw new RuntimeException("No supported matching primary found for:\n" + ctx.getText());
    }

    @Override
    public Object visitAtom(PythonParser.AtomContext ctx) {
        PythonParser.DictContext dict = ctx.dict();
        if (dict != null) {
            return visit(dict);
        }
        PythonParser.ListContext list = ctx.list();
        if (list != null) {
            return visit(list);
        }
        PythonParser.SetContext set = ctx.set();
        if (set != null) {
            return visit(set);
        }
        TerminalNode ellipsis = ctx.ELLIPSIS();
        if (ellipsis != null) {
            throw new RuntimeException("atom not supported for:\n" + ctx.getText());
        }
        TerminalNode name = ctx.NAME();
        if (name != null) {
            return new PyObjectRef(name.getText(), name.getSymbol().getLine());
        }
        PythonParser.StringsContext strings = ctx.strings();
        if (strings != null) {
            return visit(strings);
        }

        TerminalNode number = ctx.NUMBER();
        if (number != null) {
            if (number.getText().contains(".")) {
                return Double.parseDouble(number.getText());
            } else {
                return Long.parseLong(number.getText());
            }
        }
        throw new RuntimeException("No supported matching atom found for:\n" + ctx.getText());
    }

    @Override
    public Object visitStrings(PythonParser.StringsContext ctx) {
        List<PythonParser.FstringContext> fstring = ctx.fstring();
        if (fstring != null) {
            if (!fstring.isEmpty()) {
                throw new RuntimeException("strings not supported for:\n" + ctx.getText());
            }
        }
        List<PythonParser.StringContext> string = ctx.string();
        if (string != null) {
            if (string.size() == 1) {
                return visit(string.getFirst());
            }

            throw new RuntimeException("strings not supported for:\n" + ctx.getText());
        }
        throw new RuntimeException("No supported matching strings found for:\n" + ctx.getText());
    }

    @Override
    public Object visitString(PythonParser.StringContext ctx) {
        TerminalNode STRING = ctx.STRING();
        if (STRING != null) {
            return STRING.getText().substring(1, STRING.getText().length() - 1);
        }
        throw new RuntimeException("No supported matching string found for:\n" + ctx.getText());
    }

    @Override
    public Object visitImport_stmt(PythonParser.Import_stmtContext ctx) {
        PythonParser.Import_nameContext importNameContext = ctx.import_name();
        if (importNameContext != null) {
            return visit(importNameContext);
        }
        if (ctx.import_from() != null) {
            return visit(ctx.import_from());
        }
        throw new RuntimeException("No supported matching import_stmt found for:\n" + ctx.getText());
    }

    @Override
    public Object visitImport_from(PythonParser.Import_fromContext ctx) {
        PythonParser.Import_from_targetsContext importFromTargetsContext = ctx.import_from_targets();
        TerminalNode from = ctx.FROM();
        TerminalNode import_ = ctx.IMPORT();
        if (from == null || import_ == null) {
            throw new RuntimeException("No supported matching import_from found for:\n" + ctx.getText());
        }

        if (importFromTargetsContext != null) {
            PythonParser.Dotted_nameContext dottedNameContext = ctx.dotted_name();
            System.out.println("dottedNameContext = " + dottedNameContext.getText());
            System.out.println("importFromTargetsContext.getText() = " + importFromTargetsContext.getText());

            List<Map.Entry<String, String>> visit = (List<Map.Entry<String, String>>) visit(importFromTargetsContext);
            for (Map.Entry<String, String> s : visit) {
                this.imports.put(s.getKey(), ((String) visit(dottedNameContext)).replace(".", "/") + "/" + s.getValue());
            }

            return visit(importFromTargetsContext);
        }
        throw new RuntimeException("No supported matching import_from_targets found for:\n" + ctx.getText());
    }

    @Override
    public Object visitImport_from_targets(PythonParser.Import_from_targetsContext ctx) {
        PythonParser.Import_from_as_namesContext importFromAsNamesContext = ctx.import_from_as_names();
        if (importFromAsNamesContext != null) {
            return visit(importFromAsNamesContext);
        }
        throw new RuntimeException("No supported matching import_from_as_names found for:\n" + ctx.getText());
    }

    @Override
    public Object visitImport_from_as_names(PythonParser.Import_from_as_namesContext ctx) {
        List<Map.Entry<String, String>> importFromAsNames = new ArrayList<>();
        for (int i = 0; i < ctx.import_from_as_name().size(); i++) {
            importFromAsNames.add((Map.Entry<String, String>) visit(ctx.import_from_as_name(i)));
        }
        return importFromAsNames;
    }

    @Override
    public Object visitImport_from_as_name(PythonParser.Import_from_as_nameContext ctx) {
        TerminalNode as = ctx.AS();
        if (as == null) {
            String text = ctx.NAME(0).getText();
            return new AbstractMap.SimpleEntry(text, text);
        }

        if (ctx.NAME().size() == 2) {
            return new AbstractMap.SimpleEntry(ctx.NAME(1).getText(), ctx.NAME(0).getText());
        }

        throw new RuntimeException("No supported matching import_from_as_name found for:\n" + ctx.getText());
    }

    @Override
    public Object visitDotted_name(PythonParser.Dotted_nameContext ctx) {
        return ctx.getText();
    }

    @Override
    public Object visitFile_input(PythonParser.File_inputContext ctx) {
        PythonParser.StatementsContext statements = ctx.statements();
        rootCw.visit(V1_8, ACC_PUBLIC, path + "_" + CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, fileName), null, "java/lang/Object", null);

        // Set the file name of the class to fileName
        rootCw.visitSource(fileName + ".py", null);

        visit(statements);
        rootCw.visitEnd();

        try {
            File file = new File("build/pythonc/" + path + "_" + CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, fileName) + ".class");
            File parentFile = file.getParentFile();
            if (!parentFile.exists()) {
                parentFile.mkdirs();
            }
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(rootCw.toByteArray());
            fos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Unit.Instance;
    }

    public void compile(File file) throws IOException {
        PythonLexer lexer = new PythonLexer(CharStreams.fromPath(file.toPath()));
        PythonParser parser = new PythonParser(new CommonTokenStream(lexer));
        PythonParser.File_inputContext fileInputContext = parser.file_input();
        var p = file.getName().substring(0, file.getName().length() - ".py".length());
        this.fileName = p.substring(p.lastIndexOf("/") + 1);
        this.path = p.substring(0, p.lastIndexOf("/") + 1);
        visit(fileInputContext);
    }

    public void compile(String python, String fileName) throws IOException {
        PythonLexer lexer = new PythonLexer(CharStreams.fromString(python));
        PythonParser parser = new PythonParser(new CommonTokenStream(lexer));
        PythonParser.File_inputContext fileInputContext = parser.file_input();
        var p = fileName.substring(0, fileName.length() - ".py".length());
        this.fileName = p.substring(p.lastIndexOf("/") + 1);
        this.path = p.substring(0, p.lastIndexOf("/") + 1);
        visit(fileInputContext);
    }

    public void pack(String module) {
        // Pack "build/rustc/**/*.class"
        try {
            Process process = Runtime.getRuntime().exec("jar cf build/" + module + ".jar -C build/pythonc .");
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static class PythonListener extends PythonParserBaseListener {
        private final ClassWriter rootClassWriter;
        private final String fileName;

        public PythonListener(ClassWriter rootClassWriter, String fileName) {
            this.rootClassWriter = rootClassWriter;
            this.fileName = fileName;
        }
    }

    private record FuncCall(PythonParser.AtomContext atom, PythonParser.PrimaryContext primaryContext) {
    }

    private record FuncCallWithArgs(PythonParser.PrimaryContext atom,
                                    PythonParser.PrimaryContext primaryContext,
                                    PythonParser.ArgumentsContext arguments) {
        public void write(MethodVisitor mv, PythonCompiler pythonCompiler) {
            mv.visitLineNumber(atom.getStart().getLine(), new Label());
            Object visit = pythonCompiler.visit(atom);
            if (visit == null) {
                throw new RuntimeException("atom not supported for:\n" + atom.getText());
            }

            Object visit1 = pythonCompiler.visit(arguments);
            if (!(visit1 instanceof List)) {
                throw new RuntimeException("arguments not supported for:\n" + arguments.getText());
            }

            if (visit instanceof TypedName) {
//                mv.visitLdcInsn("Hello World!");

                List<Object> constants = new ArrayList<>();
                for (Object o : (List<?>) visit1) {
                    switch (o) {
                        case String s -> mv.visitLdcInsn(o);
                        case Integer i -> mv.visitLdcInsn(o);
                        case PyVariable pyVar -> mv.visitVarInsn(ALOAD, pyVar.index);
                        case null, default ->
                                throw new RuntimeException("argument not supported for:\n" + arguments.getText());
                    }
                }

                // Create an array of objects
                mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
                for (int i = 0; i < constants.size(); i++) {
                    mv.visitInsn(DUP);
                    mv.visitLdcInsn(constants.get(i));
                    // Cast the object to the correct type
                    mv.visitTypeInsn(CHECKCAST, "java/lang/Object");
                    mv.visitInsn(AASTORE);
                }

                // Print the array
                mv.visitMethodInsn(INVOKESTATIC, "python/builtins/_Builtins", "print", "([Ljava/lang/Object;)V", false);
            } else if (visit instanceof PyObjectRef) {
                List<PyExpr> exprs = new ArrayList<>();
                List<?> objects = (List<?>) visit1;
                for (int j = 0, objectsSize = objects.size(); j < objectsSize; j++) {
                    Object o = objects.get(j);
                    switch (o) {
                        case String s -> exprs.add(new PyConstant(s, arguments.args().expression(j).getStart().getLine()));
                        case Boolean b -> exprs.add(new PyConstant(b, arguments.args().expression(j).getStart().getLine()));
                        case Float f -> exprs.add(new PyConstant(f, arguments.args().expression(j).getStart().getLine()));
                        case Double d -> exprs.add(new PyConstant(d, arguments.args().expression(j).getStart().getLine()));
                        case Character c -> exprs.add(new PyConstant(c, arguments.args().expression(j).getStart().getLine()));
                        case Byte b -> exprs.add(new PyConstant(b, arguments.args().expression(j).getStart().getLine()));
                        case Short s -> exprs.add(new PyConstant(s, arguments.args().expression(j).getStart().getLine()));
                        case Integer i -> exprs.add(new PyConstant(i, arguments.args().expression(j).getStart().getLine()));
                        case Long l -> exprs.add(new PyConstant(l, arguments.args().expression(j).getStart().getLine()));
                        case PyVariable pyVar -> mv.visitVarInsn(ALOAD, pyVar.index);
                        case null, default ->
                                throw new RuntimeException("argument not supported for:\n" + arguments.getText());
                    }
                }

                Object[] preloaded = new Object[exprs.size()];
                for (int i = 0, exprsSize = exprs.size(); i < exprsSize; i++) {
                    PyExpr expr = exprs.get(i);
                    preloaded[i] = expr.preload(mv, pythonCompiler);
                }

                mv.visitIntInsn(BIPUSH, exprs.size());
                mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");

                // Create an array of objects
                for (int i = 0; i < exprs.size(); i++) {
                    mv.visitLineNumber(arguments.args().expression(i).getStart().getLine(), new Label());
                    mv.visitInsn(DUP);
                    mv.visitIntInsn(BIPUSH, i); // Push the index onto the stack
                    if (exprs.get(i) instanceof PyConstant o) {
                        switch (o.type) {
                            case INTEGER -> {
                                mv.visitTypeInsn(NEW, "java/lang/Integer");
                                mv.visitInsn(DUP);
                            }
                            case FLOAT -> {
                                mv.visitTypeInsn(NEW, "java/lang/Float");
                                mv.visitInsn(DUP);
                            }
                            case DOUBLE -> {
                                mv.visitTypeInsn(NEW, "java/lang/Double");
                                mv.visitInsn(DUP);
                            }
                            case BOOLEAN -> {
                                mv.visitTypeInsn(NEW, "java/lang/Boolean");
                                mv.visitInsn(DUP);
                            }
                            case BYTE -> {
                                mv.visitTypeInsn(NEW, "java/lang/Byte");
                                mv.visitInsn(DUP);
                            }
                            case SHORT -> {
                                mv.visitTypeInsn(NEW, "java/lang/Short");
                                mv.visitInsn(DUP);
                            }
                            case LONG -> {
                                mv.visitTypeInsn(NEW, "java/lang/Long");
                                mv.visitInsn(DUP);
                            }
                            case CHARACTER -> {
                                mv.visitTypeInsn(NEW, "java/lang/Character");
                                mv.visitInsn(DUP);
                            }
                        }
                    }
                    exprs.get(i).load(mv, pythonCompiler, preloaded[i]);
                    // Unbox
                    if (exprs.get(i) instanceof PyConstant o) {
                        switch (o.type) {
                            case INTEGER -> mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(I)V", false);
                            case FLOAT -> mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Float", "<init>", "(F)V", false);
                            case DOUBLE -> mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Double", "<init>", "(D)V", false);
                            case BOOLEAN -> mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Boolean", "<init>", "(Z)V", false);
                            case BYTE -> mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Byte", "<init>", "(B)V", false);
                            case SHORT -> mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Short", "<init>", "(S)V", false);
                            case LONG -> mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Long", "<init>", "(J)V", false);
                            case CHARACTER -> mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Character", "<init>", "(C)V", false);
                        }
                    }
                    mv.visitInsn(AASTORE);
                }

                // Print the array
                mv.visitMethodInsn(INVOKESTATIC, "python/builtins/_Builtins", "print", "([Ljava/lang/Object;)V", false);
            } else {
                throw new RuntimeException("No supported matching atom found for: " + visit.getClass().getName());
            }

            mv.visitLineNumber(atom.getStop().getLine(), new Label());
        }
    }

    private int createVariable(PyExpr expr) {
        Object preloaded = expr.preload(mv, this);
        expr.load(mv, this, preloaded);

        currentVariableIndex++;

        mv.visitVarInsn(ASTORE, currentVariableIndex);

        return currentVariableIndex;
    }

    @Override
    public Object visitArguments(PythonParser.ArgumentsContext ctx) {
        return visit(ctx.args());
    }

    @Override
    public Object visitArgs(PythonParser.ArgsContext ctx) {
        PythonParser.KwargsContext kwargs = ctx.kwargs();
        if (kwargs != null) {
            throw new RuntimeException("kwargs not supported for:\n" + ctx.getText());
        }

        List<Object> args = new ArrayList<>();
        for (PythonParser.ExpressionContext expressionContext : ctx.expression()) {
            args.add(visit(expressionContext));
        }

        return args;
    }

    private record PyObjectRef(String name, int lineNo) implements PyExpr {

        @Override
        public Object preload(MethodVisitor mv, PythonCompiler pythonCompiler) {
            // Set variable to "<name>.class"
            mv.visitLdcInsn(name);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);

            // Set variable
            mv.visitVarInsn(ASTORE, pythonCompiler.currentVariableIndex++);
            return null;
        }

        @Override
        public void load(MethodVisitor mv, PythonCompiler pythonCompiler, Object preloaded) {
            throw new RuntimeException("No supported matching PyObjectRef found for:\n" + this.name);
        }
    }

    private record TypedName(String name, String type) {

    }

    private record Self(int lineNo) implements PyExpr {
        @Override
        public Object preload(MethodVisitor mv, PythonCompiler pythonCompiler) {
            return null;
        }

        @Override
        public void load(MethodVisitor mv, PythonCompiler pythonCompiler, Object preloaded) {
            // Use "this"
//            mv.visitVarInsn(ALOAD, 0);
            throw new RuntimeException("No supported matching Self found for:\n" + this.lineNo);
        }
    }

    private record PyVariable(String name, int index, int lineNo) implements PyExpr {

        @Override
        public Object preload(MethodVisitor mv, PythonCompiler pythonCompiler) {
            return null;
        }

        @Override
        public void load(MethodVisitor mv, PythonCompiler pythonCompiler, Object preloaded) {
            throw new RuntimeException("No supported matching PyVariable found for:\n" + this.name);
//            mv.visitVarInsn(ALOAD, index);
        }
    }

    private interface PyExpr {
        Object preload(MethodVisitor mv, PythonCompiler pythonCompiler);
        void load(MethodVisitor mv, PythonCompiler pythonCompiler, Object preloaded);
        int lineNo();
    }

    private static final class PyConstant implements PyExpr {
        private final Object value;
        private final int lineNo;
        public final Type type;

        private PyConstant(Object value, int lineNo) {
            this.value = value;
            this.lineNo = lineNo;
            type = switch (this.value) {
                    case String s -> Type.STRING;
                    case Integer i -> Type.INTEGER;
                    case Float f -> Type.FLOAT;
                    case Double d -> Type.DOUBLE;
                    case Boolean b -> Type.BOOLEAN;
                    case Byte b -> Type.BYTE;
                    case Short s -> Type.SHORT;
                    case Long l -> Type.LONG;
                    case Character c -> Type.CHARACTER;
                    default -> throw new RuntimeException("No supported matching type found for:\n" + this.value);
                };
        }

            enum Type {
                STRING,
                INTEGER,
                FLOAT,
                DOUBLE,
                BOOLEAN,
                BYTE,
                SHORT,
                LONG,
                CHARACTER
            }

            @Override
            public Object preload(MethodVisitor mv, PythonCompiler pythonCompiler) {
                return null;
            }

            @Override
            public void load(MethodVisitor mv, PythonCompiler pythonCompiler, Object preloaded) {
                mv.visitLdcInsn(value);
            }

        public Object value() {
            return value;
        }

        @Override
        public int lineNo() {
            return lineNo;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (PyConstant) obj;
            return Objects.equals(this.value, that.value) &&
                   this.lineNo == that.lineNo;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value, lineNo);
        }

        @Override
        public String toString() {
            return "PyConstant[" +
                   "value=" + value + ", " +
                   "lineNo=" + lineNo + ']';
        }

        }

    private class Decorators {
        public final Map<String, FuncCallWithArgs> byJvmName = new HashMap<>();
    }
}
