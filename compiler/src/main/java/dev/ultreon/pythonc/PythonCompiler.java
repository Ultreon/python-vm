package dev.ultreon.pythonc;

import com.google.common.base.CaseFormat;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    public static final int F_CPL_ASSIGN = 7;
    public static final int F_CPL_TYPE_ANNO = 8;
    public static final int F_CPL_CLASS_INHERITANCE = 9;
    public static final int F_CPL_FILE = 10;
    public static final int F_DYN_CALL = 50;

    public static final String E_NOT_ALLOWED = "Not allowed";
    public static final String FMT_CLASS = "L%s;";

    private ClassWriter rootCw;
    Set<String> undefinedClasses = new LinkedHashSet<>();
    private ClassWriter cw = rootCw;
    private String rootDir;
    private String path = "";
    private String fileName = "Main";
    final Map<String, Symbol> imports = new HashMap<>();
    private State state = State.File;
    final BitSet flags = new BitSet();
    final Decorators decorators = new Decorators();
    MethodVisitor mv;
    MethodVisitor rootInitMv;
    int currentVariableIndex = 1;
    final Set<String> implementing = new HashSet<>();
    final Map<String, Symbol> symbols = new HashMap<>();
    Label endLabel;
    Label startLabel;
    Label curLabel;
    Map<String, PyClass> classes = new HashMap<>();
    private PyClass curPyClass = null;
    private final List<CompilerException> compileErrors = new ArrayList<>();
    private Label elifLabel;
    final Stack<Context> contextStack = new Stack<>();

    public final JvmWriter writer = new JvmWriter(this);

    public void compileSources(String sourceDir) {
        // Walk the directory
        Path path = Path.of(System.getProperty("user.dir")).relativize(Path.of(sourceDir));
        try {
            try (Stream<Path> walk = Files.walk(path)) {
                walk
                        .map(Path::toString)
                        .filter(string -> string.endsWith(".py"))
                        .forEach(v -> {
                            try {
                                compile(new File(v), new File(sourceDir));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
            }
            try (Stream<Path> walk = Files.walk(path)) {
                walk
                        .filter(p -> !p.toString().endsWith(".py"))
                        .map(v -> Path.of(sourceDir).relativize(v.toAbsolutePath()).toString())
                        .forEach(v -> {
                            try {
                                // Copy over resources
                                Path path1 = Path.of("build/tmp/compilePython", v);
                                System.out.println("path1 = " + path1);
                                if (Files.notExists(path1)) Files.createDirectories(path1.getParent());
                                Path path2 = Path.of(sourceDir, v);
                                if (Files.isDirectory(path2)) return;
                                Files.copy(Path.of(sourceDir, v), path1.toAbsolutePath(), StandardCopyOption.REPLACE_EXISTING);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (!compileErrors.isEmpty()) {
            for (CompilerException ex : compileErrors) {
                System.err.println("ERROR: " + ex.getMessage());
            }

            System.exit(1);
        }
    }

    JvmClass getClassSymbol(String className) {
        var name = className.substring(className.lastIndexOf(".") + 1);

        if (!symbols.containsKey(name)) {
            undefinedClasses.add(className);
            return null;
        }

        Symbol symbol = symbols.get(name);
        if (symbol instanceof JvmClass) {
            return (JvmClass) symbol;
        }

        undefinedClasses.add(className);
        return null;
    }

    enum State {
        File, Class, Function, Decorators
    }

    public PythonCompiler() {
        symbols.put("int", new PyBuiltinClass("Ljava/lang/Long;", "J", "Lpythonvm/builtins/Long;", "int"));
        symbols.put("float", new PyBuiltinClass("Ljava/lang/Double;", "D", "Lpythonvm/builtins/Double;", "float"));
        symbols.put("bool", new PyBuiltinClass("Ljava/lang/Boolean;", "Z", "Lpythonvm/builtins/Boolean;", "bool"));
        symbols.put("str", new PyBuiltinClass("Ljava/lang/String;", "Lpythonvm/builtins/String;", "str"));
        symbols.put("bytes", new PyBuiltinClass("[B", "[B", "Lpythonvm/builtins/Bytes;", "bytes"));
        symbols.put("bytearray", new PyBuiltinClass("[B", "[B", "Lpythonvm/builtins/ByteArray;", "bytearray"));
        symbols.put("list", new PyBuiltinClass("Ljava/util/List;", "Lpythonvm/builtins/List;", "list"));
        symbols.put("dict", new PyBuiltinClass("Ljava/util/Map;", "Lpythonvm/builtins/Dict;", "dict"));
        symbols.put("set", new PyBuiltinClass("Ljava/util/Set;", "Lpythonvm/builtins/Set;", "set"));
        symbols.put("tuple", new PyBuiltinClass("[Ljava/util/Object;", "Lpythonvm/builtins/Tuple;", "tuple"));
        symbols.put("range", new PyBuiltinClass("Ljava/util/List;", "Lpythonvm/builtins/Range;", "range"));
        symbols.put("None", new PyBuiltinClass("Ljava/lang/Object;", "Lpythonvm/builtins/None;", "None"));
        symbols.put("object", new PyBuiltinClass("Ljava/lang/Object;", "Lpythonvm/builtins/Object;", "object"));
        symbols.put("type", new PyBuiltinClass("Ljava/lang/Class;", "Lpythonvm/builtins/Type;", "type"));
        symbols.put("Exception", new PyBuiltinClass("Ljava/lang/Exception;", "Lpythonvm/builtins/Exception;", "Exception"));
        symbols.put("BaseException", new PyBuiltinClass("Ljava/lang/RuntimeException;", "Lpythonvm/builtins/BaseException;", "BaseException"));
        symbols.put("StopIteration", new PyBuiltinClass("Ljava/lang/RuntimeException;", "Lpythonvm/builtins/StopIteration;", "StopIteration"));
        symbols.put("StopAsyncIteration", new PyBuiltinClass("Ljava/lang/RuntimeException;", "Lpythonvm/builtins/StopAsyncIteration;", "StopAsyncIteration"));
        symbols.put("GeneratorExit", new PyBuiltinClass("Ljava/lang/RuntimeException;", "Lpythonvm/builtins/GeneratorExit;", "GeneratorExit"));
        symbols.put("SystemExit", new PyBuiltinClass("Ljava/lang/RuntimeException;", "Lpythonvm/builtins/SystemExit;", "SystemExit"));
        symbols.put("KeyboardInterrupt", new PyBuiltinClass("Ljava/lang/RuntimeException;", "Lpythonvm/builtins/KeyboardInterrupt;", "KeyboardInterrupt"));
        symbols.put("ImportError", new PyBuiltinClass("Ljava/lang/RuntimeException;", "Lpythonvm/builtins/ImportError;", "ImportError"));
        symbols.put("ModuleNotFoundError", new PyBuiltinClass("Ljava/lang/RuntimeException;", "Lpythonvm/builtins/ModuleNotFoundError;", "ModuleNotFoundError"));
        symbols.put("IndexError", new PyBuiltinClass("Ljava/lang/RuntimeException;", "Lpythonvm/builtins/IndexError;", "IndexError"));
        symbols.put("KeyError", new PyBuiltinClass("Ljava/lang/RuntimeException;", "Lpythonvm/builtins/KeyError;", "KeyError"));
        symbols.put("ValueError", new PyBuiltinClass("Ljava/lang/RuntimeException;", "Lpythonvm/builtins/ValueError;", "ValueError"));
        symbols.put("TypeError", new PyBuiltinClass("Ljava/lang/RuntimeException;", "Lpythonvm/builtins/TypeError;", "TypeError"));
        symbols.put("NotImplementedError", new PyBuiltinClass("Ljava/lang/UnsupportedOperationException;", "Lpythonvm/builtins/NotImplementedError;", "NotImplementedError"));
        symbols.put("OverflowError", new PyBuiltinClass("Ljava/lang/RuntimeException;", "Lpythonvm/builtins/OverflowError;", "OverflowError"));

        symbols.put("asc", new PyBuiltinFunction("java/lang/String", "pythonvm/builtins/BuiltinsPy", new String[]{"asc(Ljava/lang/String;)V"}, 1, "asc"));
        symbols.put("print", new PyBuiltinFunction("java/lang/System", "pythonvm/builtins/BuiltinsPy", new String[]{"print([Ljava/lang/Object;Ljava/util/Map;)"}, 2, "print", PyBuiltinFunction.Mode.DYN_CTOR));
    }

    @Override
    public Object visit(@org.jetbrains.annotations.Nullable ParseTree tree) {
        if (tree == null) {
            throw new RuntimeException("Tree is null");
        }
        System.out.print("Visiting: " + tree.getClass().getSimpleName() + " " + tree.getText());
        if (!contextStack.isEmpty()) {
            System.out.println(" (Stack size before: " + getContext(Context.class).stackSize() + ")");
        } else {
            System.out.println();
        }

        Object visit = super.visit(tree);
        if (visit == null) {
            String simpleName = tree.getClass().getSimpleName();
            throw new RuntimeException("Visit unavailable for visit" + simpleName.substring(0, simpleName.length() - "Context".length()) + ":\n" + tree.getText());
        }
        System.out.print("Visited: " + tree.getClass().getSimpleName() + " " + tree.getText());
        if (!contextStack.isEmpty()) {
            System.out.println(" (Stack size after: " + getContext(Context.class).stackSize() + ")");
        } else {
            System.out.println();
        }

        return visit;
    }

    @Override
    public Object visitStatements(PythonParser.StatementsContext ctx) {
        for (int i = 0; i < ctx.statement().size(); i++) {
            Context context = getContext(Context.class);
            if (context.needsPop() && i > 0) {
                PythonParser.StatementContext statement = ctx.statement(i - 1);
                if (statement == null) {
                    throw new RuntimeException("Didn't fully pop before statement " + (i - 1) + " for:\n" + ctx.getText());
                }
                throw new RuntimeException("Didn't fully pop in statement " + (i - 1) + " for:\n" + statement.getText());
            }
            Object visit = visit(ctx.statement(i));
            switch (visit) {
                case Unit unit -> {

                }
                case List<?> list -> {
                    for (Object o : list) {
                        switch (o) {
                            case FuncCall funcCall -> {
                                if (mv == null) {
                                    throw new RuntimeException("Not writing a method:\n" + ctx.getText());
                                }

                                funcCall.write(mv, this);

                                Type type = funcCall.type(this);
                                if (type.equals(Type.VOID_TYPE)) {
                                    continue;
                                }

                                writer.pop();
                            }
                            case List<?> list1 -> {
                                for (Object o1 : list1) {
                                    if (o1 instanceof FuncCall funcCall) {
                                        if (mv == null) {
                                            throw new RuntimeException("Not writing a method:\n" + ctx.getText());
                                        }

                                        funcCall.write(mv, this);
                                        Type type = funcCall.type(this);
                                        if (type.equals(Type.VOID_TYPE)) {
                                            continue;
                                        }

                                        writer.pop();

                                    }
                                }
                            }
                            case PyObjectRef pyObjectRef -> {
                                if (flags.get(F_CPL_TYPE_ANNO)) {
                                    return imports.get(pyObjectRef.name()).name();
                                } else {
                                    throw new RuntimeException("statement not supported for:\n" + ctx.getText());
                                }
                            }
                            case String s ->
                                    throw new RuntimeException("statement not supported for string:\n" + ctx.getText());
                            case Boolean b ->
                                    throw new RuntimeException("statement not supported for boolean:\n" + ctx.getText());
                            case Integer j ->
                                    throw new RuntimeException("statement not supported for integer:\n" + ctx.getText());
                            case Float f ->
                                    throw new RuntimeException("statement not supported for float:\n" + ctx.getText());
                            case Long l ->
                                    throw new RuntimeException("statement not supported for long:\n" + ctx.getText());
                            case Double d ->
                                    throw new RuntimeException("statement not supported for double:\n" + ctx.getText());
                            case Unit unit -> {
                            }
                            case PyExpr pyExpr -> {
                                pyExpr.load(mv, this, pyExpr.preload(mv, this, false), false);
                                if (pyExpr.type(this) != Type.VOID_TYPE) {
                                    writer.pop();
                                }
                            }
                            case null, default ->
                                    throw new RuntimeException("statement not supported for: " + ctx.getText());
                        }
                    }
                }
                case null, default -> throw new RuntimeException("statement not supported for:\n" + ctx.getText());
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

        PythonParser.If_stmtContext ifStmtContext = ctx.if_stmt();
        if (ifStmtContext != null) {
            return visit(ifStmtContext);
        }

        PythonParser.While_stmtContext whileStmtContext = ctx.while_stmt();
        if (whileStmtContext != null) {
            return visit(whileStmtContext);
        }

        PythonParser.For_stmtContext forStmtContext = ctx.for_stmt();
        if (forStmtContext != null) {
            return visit(forStmtContext);
        }

        throw new RuntimeException("No supported matching compound_stmt found for:\n" + ctx.getText());
    }

    @Override
    public Object visitFor_stmt(PythonParser.For_stmtContext ctx) {
//        PythonParser.Named_expressionContext namedExpressionContext = ctx.named_expression();
//        if (namedExpressionContext != null) {
//            Object visit = visit(namedExpressionContext);
//            loadExpr(ctx, visit);
//        } else {
//            throw new RuntimeException("No supported matching named_expression found for:\n" + ctx.getText());
//        }

        throw new RuntimeException("No supported matching for_stmt found for:\n" + ctx.getText());
    }

    @Override
    public Object visitWhile_stmt(PythonParser.While_stmtContext ctx) {
        Label loopStart = new Label();
        Label loopEnd = new Label();
        Label elseBlock = null;
        if (ctx.else_block() != null) {
            elseBlock = new Label();
        }

        // Loop start:
        mv.visitLabel(loopStart);

        // region Comparison(named_expression)
        pushContext(new WhileConditionContext(loopEnd, elseBlock));
        PythonParser.Named_expressionContext namedExpressionContext = ctx.named_expression();
        if (namedExpressionContext != null) {
            Object visit = visit(namedExpressionContext);
            loadExpr(ctx, visit);
        } else {
            throw new RuntimeException("No supported matching named_expression found for:\n" + ctx.getText());
        }
        Context context = writer.getContext();
        popContext();
        // endregion Comparison

        // region Loop(block)
        Context loopContext = new WhileLoopContext(loopStart, loopEnd);
        pushContext(loopContext);
        PythonParser.BlockContext block = ctx.block();
        if (block != null) {
            visit(block);
        }
        if (context.needsPop()) {
            throw new RuntimeException("Still values on stack for:\n" + ctx.getText());
        }
        popContext();
        // endregion Loop

        // Jump to loop start
        mv.visitJumpInsn(GOTO, loopStart);

        if (elseBlock != null) {
            mv.visitLabel(elseBlock);

            // region Else(block)
            PythonParser.BlockContext elseBlockContext = ctx.else_block().block();
            if (elseBlockContext != null) {
                visit(elseBlockContext);
            }
            if (context.needsPop()) {
                throw new RuntimeException("Still values on stack for:\n" + ctx.getText());
            }
            // endregion Else
        }

        // Loop end:
        mv.visitLabel(loopEnd);

        return Unit.Instance;
    }

    @Override
    public Object visitIf_stmt(PythonParser.If_stmtContext ctx) {
        Label endLabel = new Label(); // Marks the end of the if-else block
        Label elseLabel = new Label(); // Marks the start of the else block (if present)

        // Check if there is an elif block
        PythonParser.Elif_stmtContext elifStmtContext = ctx.elif_stmt();
        Label elifLabel = null;
        if (elifStmtContext != null) {
            elifLabel = new Label();
        }

        IfStatementContext context = new IfStatementContext(elifLabel, endLabel);
        this.pushContext(context);

        // Visit and evaluate the "if" condition
        PythonParser.Named_expressionContext namedExpressionContext = ctx.named_expression();
        if (namedExpressionContext != null) {
            Object visit = visit(namedExpressionContext);// Generates bytecode for the condition
            switch (visit) {
                case PyConstant pyConstant -> {
                    if (mv == null) {
                        throw new RuntimeException("Not writing a method:\n" + ctx.getText());
                    }
                    writer.loadConstant(pyConstant.value());
                    writer.smartCast(pyConstant.type(this), Type.BOOLEAN_TYPE);
                    writer.pushTrue();
                }
                case PyExpr pyExpr -> {
                    if (mv == null) {
                        throw new RuntimeException("Not writing a method:\n" + ctx.getText());
                    }

                    Type type = pyExpr.type(this);
                    if (type != Type.BOOLEAN_TYPE) {
                        throw new RuntimeException("Expected boolean, got " + type.getClassName());
                    }
                    pyExpr.load(mv, this, pyExpr.preload(mv, this, false), false);
                }
                case Boolean b -> {
                    if (mv == null) {
                        throw new RuntimeException("Not writing a method:\n" + ctx.getText());
                    }

                    writer.loadConstant(b);
                    writer.pushBoolean(b);
                    writer.pushTrue();
                }
                case Integer i -> {
                    if (mv == null) {
                        throw new RuntimeException("Not writing a method:\n" + ctx.getText());
                    }
                    writer.loadConstant(i);
                    writer.pushBoolean(i != 0);
                    writer.pushTrue();
                }
                case String s -> {
                    throw new RuntimeException("String is not a condition:\n" + ctx.getText());
                }
                case Float f -> {
                    throw new RuntimeException("Float is not a condition:\n" + ctx.getText());
                }
                case Long l -> {
                    throw new RuntimeException("Long is not a condition:\n" + ctx.getText());
                }
                case Double d -> {
                    throw new RuntimeException("Double is not a condition:\n" + ctx.getText());
                }
                case Unit unit -> {
                    throw new RuntimeException("Unit is not a condition:\n" + ctx.getText());
                }
                case null, default ->
                        throw new RuntimeException("statement not supported for: " + visit.getClass().getSimpleName());
            }

            // Jump to elif or else block if the condition is false
            writer.jumpIfEqual(elifLabel != null ? elifLabel : elseLabel);
        }

        // If block
        visit(ctx.block()); // Generate bytecode for "if" block
        writer.jump(endLabel); // Skip the else/elif blocks

        // Handle elif blocks if present
        if (elifStmtContext != null) {
            writer.label(elifLabel);

            visit(elifStmtContext);
            context.elifLabel = null;
        }

        // Handle else block if present
        writer.label(elseLabel); // Mark the else label (jumped here if the condition is false)
        PythonParser.Else_blockContext elseBlockContext = ctx.else_block();
        if (elseBlockContext != null) {
            visit(elseBlockContext);
        }

        // End label to exit if-else
        writer.label(endLabel);
        this.popContext();

        return Unit.Instance;
    }

    @Override
    public Object visitElif_stmt(PythonParser.Elif_stmtContext ctx) {
        IfStatementContext context = getContext(IfStatementContext.class);
        Label nextLabel = null;
        Label endLabel = context.endLabel; // Do NOT reassign this!

        if (ctx.elif_stmt() != null) {
            nextLabel = new Label();
        }

        Label elseLabel = ctx.else_block() != null ? new Label() : null;
        Label jumpTarget = nextLabel != null ? nextLabel : elseLabel != null ? elseLabel : endLabel;

        // Evaluate condition
        Object visit = visit(ctx.named_expression());
        loadConstant(ctx, visit, mv);
        context.pop();
        writer.jumpIfEqual(jumpTarget);

        // If true, execute block
        visit(ctx.block());
        writer.jump(endLabel);

        // Handle elif chain
        if (nextLabel != null) {
            writer.label(nextLabel);
            visit(ctx.elif_stmt());
        }

        // Else block
        if (elseLabel != null) {
            writer.label(elseLabel);
            visit(ctx.else_block());
        }

        writer.label(endLabel);

        return Unit.Instance;
    }

    private void popContext() {
        contextStack.pop();
    }

    private void pushContext(Context ifStatementContext) {
        contextStack.push(ifStatementContext);
    }

    public <T extends Context> T getContext(Class<T> ifStatementContextClass) {
        return ifStatementContextClass.cast(contextStack.peek());
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

        boolean static_ = decorators.byJvmName.containsKey("pythonvm/builtins/Staticmethod") || cw == rootCw;
        boolean class_ = decorators.byJvmName.containsKey("pythonvm/builtins/Classmethod");

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
            String s = imports.get(typedName.type()).name();
            if (s == null) {
                throw new RuntimeException("No import found for:\n" + ctx.getText());
            }
            signature.append(s).append("L").append(typedName.name()).append(";");
        }
        String sig = "";

        // TODO
        Type returnType = Type.VOID_TYPE;

        if (name.getText().startsWith("__"))
            mv = cw.visitMethod(ACC_PRIVATE + (static_ ? ACC_STATIC : 0) + (class_ ? ACC_STATIC : 0), name.getText().substring(2), "(" + sig + ")" + returnType.getDescriptor(), null, null);
        else if (name.getText().startsWith("_"))
            mv = cw.visitMethod(ACC_PROTECTED + (static_ ? ACC_STATIC : 0) + (class_ ? ACC_STATIC : 0), name.getText().substring(1), "(" + sig + ")" + returnType.getDescriptor(), null, null);
        else
            mv = cw.visitMethod(ACC_PUBLIC + (static_ ? ACC_STATIC : 0) + (class_ ? ACC_STATIC : 0), name.getText(), "(" + sig + ")" + returnType.getDescriptor(), null, null);

        endLabel = new Label();
        endLabel.info = 100000;
        startLabel = new Label();
        startLabel.info = 0;

        try {
            mv.visitCode();

            if (decorators.byJvmName.containsKey("pythonvm/utils/Override")) {
                // Ignore for now
            }

            Label blockStart = new Label();
            mv.visitLineNumber(ctx.getStart().getLine(), blockStart);
            FunctionContext functionContext = new FunctionContext();
            pushContext(functionContext);

            visit(block);

            popContext();
            while (functionContext.needsPop()) {
                writer.pop();
                functionContext.pop();
            }

//            writer.label(endLabel);

            writer.returnVoid();
            writer.end();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            flags.clear(F_CPL_STATIC_FUNC);
            flags.clear(F_CPL_CLASS_FUNC);
            flags.clear(F_CPL_INSTANCE_FUNC);

            mv = null;
        }

        if (curPyClass != null) {
            curPyClass.methods.put(name.getText(), new PyFunction(curPyClass, name.getText(), returnType, ctx.start.getLine()));
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
                return new Self(param.getStart().getLine(), curPyClass.type(this));
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
                for (int i = 0, listSize = list.size(); i < listSize; i++) {
                    Object o = list.get(i);
                    if (o instanceof FuncCall func) {
                        var atom = func.atom;
                        var arguments = func.arguments;
                        var primaryContext = func.primaryContext;
                        if (arguments != null) {
                            String text = primaryContext.getText();
                            if (text.equals("implements")) {
                                Object visit1 = visit(arguments);
                                if (visit1 == null) {
                                    throw new RuntimeException("Decorators not supported for:\n" + ctx.getText());
                                }
                                if (visit1 instanceof List<?> list1) {
                                    for (Object o1 : list1) {
                                        if (o1 instanceof PyObjectRef(String name, int lineNo)) {
                                            implementing.add(imports.get(name).name());
                                        } else {
                                            throw new CompilerException("Invalid @implements(...) decorator: @" + decorators.named_expression(i).getText());
                                        }
                                    }
                                } else if (visit1 instanceof Type type) {
                                    String descriptor = type.getDescriptor();
                                    implementing.add(descriptor.substring(1, descriptor.length() - 1));
                                } else {
                                    throw new CompilerException("Invalid @implements(...) decorator: @" + decorators.named_expression(i).getText());
                                }
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

        String superClass = "java/lang/Object";
        List<String> implementing = new ArrayList<>();
        PythonParser.ArgumentsContext arguments = ctx.arguments();
        if (arguments != null) {
            flags.set(F_CPL_CLASS_INHERITANCE);
            try {
                Object visit = visit(arguments);
                if (visit == null) {
                    throw new RuntimeException("arguments not supported for:\n" + ctx.getText());
                }

                if (visit instanceof List<?> list) {
                    for (Object o : list) {
                        if (o instanceof String classname) {
                            if (!classname.startsWith("L") || !classname.endsWith(";")) {
                                throw new RuntimeException("Class not supported for:\n" + ctx.getText());
                            }
                            var normalName = classname.substring(1, classname.length() - 1).replace('/', '.');
                            try {
                                Class<?> type = Class.forName(normalName, false, getClass().getClassLoader());
                                if (type.isInterface()) {
                                    implementing.add(normalName.replace('.', '/'));
                                } else if (superClass.equals("java/lang/Object") && type != Object.class) {
                                    superClass = normalName.replace('.', '/');
                                } else {
                                    throw new CompilerException("Cannot inherit multiple super classes for: " + path.replace("/", ".") + name.getText() + " at " + getLocation(ctx));
                                }
                            } catch (ClassNotFoundException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            } finally {
                flags.clear(F_CPL_CLASS_INHERITANCE);
            }
        }

        cw.visit(V1_8, ACC_PUBLIC, path + fileName + "/" + name.getText(), null, superClass, implementing == null ? new String[0] : implementing.isEmpty() ? null : implementing.toArray(new String[0]));
        String classname = (path + fileName + "/" + name.getText()).replace("/", ".");
        PyClass value = new PyClass(Type.getObjectType(path + fileName + "/" + name.getText()), ctx.start.getLine());
        imports.put(name.getText(), value);
        symbols.put(name.getText(), value);
        undefinedClasses.remove(classname);

        curPyClass = value;

        FieldVisitor self = cw.visitField(ACC_PUBLIC | ACC_FINAL, "__dict__", "Ljava/util/Map;", /*<String, Object>*/ "Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;", null);
//        AnnotationVisitor annotationVisitor = self.visitAnnotation("pythonvm/vm/annotations/Generated", true);
//        annotationVisitor.visit("reason", "Generated for Python interop");
//        annotationVisitor.visitEnd();
        self.visitEnd();

        // Create default constructor
        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitTypeInsn(NEW, "java/util/HashMap");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false);
        mv.visitFieldInsn(PUTFIELD, path + fileName + "/" + name.getText(), "__dict__", "Ljava/util/Map;");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, path + fileName + "/" + name.getText(), "__init__", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        // Create __init__ method
        mv = cw.visitMethod(ACC_PUBLIC, "__init__", "()V", null, null);
        mv.visitCode();
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        cw.visitEnd();

        Object visit = visit(ctx.block());
        if (visit == null) {
            throw new RuntimeException("block not supported for:\n" + ctx.getText());
        }
        cw.visitEnd();

        classes.put(classname, value);
        curPyClass = null;

        byte[] bytes = cw.toByteArray();

        try {
            String s = "build/tmp/compilePython/" + path + fileName;
            if (!new File(s).exists()) {
                new File(s).mkdirs();
            }
            FileOutputStream fileOutputStream = new FileOutputStream("build/tmp/compilePython/" + path + fileName + "/" + name.getText() + ".class");
            fileOutputStream.write(bytes);
            fileOutputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
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
        for (PythonParser.Named_expressionContext namedExpressionContext : namedExpressionContexts) {
            visit.add(visit(namedExpressionContext));
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
        PythonParser.AssignmentContext assignment = ctx.assignment();
        if (assignment != null) {
            Object visit = visit(assignment);
            if (visit == null || visit == Unit.Instance) {
                return visit;
            }

            writer.pop();
            return visit;
        }

        TerminalNode aBreak = ctx.BREAK();
        if (aBreak != null) {
            Context context = writer.getLoopContext();
            if (context instanceof WhileLoopContext whileConditionContext) {
                mv.visitJumpInsn(GOTO, whileConditionContext.endLabel);
                return Unit.Instance;
//            } else if (writer.getContext() instanceof ForConditionContext) {
//                ForConditionContext forConditionContext = (ForConditionContext) writer.getContext();
//                mv.visitJumpInsn(GOTO, forConditionContext.loopEnd);
//                return Unit.Instance;
            } else {
                throw new RuntimeException("Not in a loop:\n" + ctx.getText());
            }
        }
        throw new RuntimeException("No supported matching simple_stmt found of type " + ctx.getClass().getSimpleName() + " for:\n" + ctx.getText());
    }

    @Override
    public Object visitAssignment(PythonParser.AssignmentContext ctx) {
        flags.set(F_CPL_ASSIGN);
        try {
            curLabel = new Label();
            TerminalNode name1 = ctx.NAME();
            @Nullable String name = name1 == null ? null : name1.getText();

            if (name == null) {
                List<PythonParser.Star_targetsContext> starTargetsContexts = ctx.star_targets();
                if (starTargetsContexts.size() != 1) {
                    // TODO Add support for multiple star targets
                    throw new RuntimeException("Expected 1 variable target, got " + starTargetsContexts.size() + " which is unsupported for now.");
                }

                PythonParser.Star_targetsContext starTargetsContext = starTargetsContexts.get(0);
                if (starTargetsContext == null) {
                    throw new RuntimeException("Variable target is not found :\n" + ctx.getText());
                }

                List<PythonParser.Star_targetContext> starTargetContext = starTargetsContext.star_target();
                if (starTargetContext == null) {
                    throw new RuntimeException("Variable target is not found :\n" + ctx.getText());
                }


                if (starTargetContext.size() != 1) {
                    // TODO Add support for multiple star targets
                    throw new RuntimeException("Expected 1 variable target, got " + starTargetContext.size() + " which is unsupported for now.");
                }
                PythonParser.Star_targetContext starTargetContext1 = starTargetContext.getFirst();
                if (starTargetContext1 == null) {
                    throw new RuntimeException("Variable target is not found :\n" + ctx.getText());
                }

                PythonParser.Target_with_star_atomContext targetWithStarAtomContext = starTargetContext1.target_with_star_atom();
                if (targetWithStarAtomContext == null) {
                    throw new RuntimeException("Variable target is not found :\n" + ctx.getText());
                }

                PythonParser.Star_atomContext targetContext = targetWithStarAtomContext.star_atom();
                if (targetContext == null) {
                    throw new RuntimeException("Variable target is not found :\n" + ctx.getText());
                }

                name = targetContext.NAME().getText();

                PythonParser.Star_expressionsContext starExpressionsContext = ctx.star_expressions();
                if (starExpressionsContext == null) {
                    throw new RuntimeException("Value for variable assignment wasn't found.");
                }

                List<PythonParser.Star_expressionContext> starExpressionContexts = starExpressionsContext.star_expression();
                if (starExpressionContexts.size() != 1) {
                    // TODO Add support for multiple star expressions
                    throw new RuntimeException("Expected 1 expression, got " + starExpressionContexts.size() + " which is unsupported for now.");
                }

                PythonParser.Star_expressionContext starExpressionContext = starExpressionContexts.get(0);
                TerminalNode star = starExpressionContext.STAR();
                if (star != null) {
                    // TODO Add support for star expressions
                    throw new RuntimeException("Star expressions are not supported for now.");
                }

                PythonParser.ExpressionContext expressionContext = starExpressionContext.expression();
                if (expressionContext == null) {
                    throw new RuntimeException("Expression for variable assignment wasn't found.");
                }

                Object visit1 = visit(expressionContext);
                if (visit1 == null) {
                    throw new RuntimeException("Expression for variable assignment wasn't found.");
                }

                if (mv == null) {
                    Type type = switch (visit1) {
                        case PyExpr symbol -> symbol.type(this);
                        case String string -> Type.getType(String.class);
                        case Byte byteValue -> Type.getType(byte.class);
                        case Short shortValue -> Type.getType(short.class);
                        case Integer integer -> Type.getType(int.class);
                        case Boolean booleanValue -> Type.getType(boolean.class);
                        case Long longValue -> Type.getType(long.class);
                        case Float floatValue -> Type.getType(float.class);
                        case Double doubleValue -> Type.getType(double.class);
                        case Character charValue -> Type.getType(char.class);
                        default -> throw new RuntimeException("Expression for variable assignment wasn't found.");
                    };

                    if (type == null) {
                        throw new RuntimeException("Type for variable assignment wasn't found.");
                    }

                    FieldVisitor fieldVisitor = cw.visitField(ACC_PUBLIC + ACC_STATIC, name, type.getDescriptor(), null, null);
                    fieldVisitor.visitEnd();

                    symbols.put(name, new ImportedField(name, type, getName(), ctx.start.getLine()));

                    if (cw == rootCw) {
                        MethodVisitor rootInitMv1 = rootInitMv;
                        loadConstant(ctx, visit1, rootInitMv1);
                        Context context = getContext(Context.class);
                        context.pop();
                        rootInitMv1.visitFieldInsn(PUTSTATIC, getName(), name, type.getDescriptor());
                    } else {
                        throw new RuntimeException("Not in root class");
                    }
                } else if (this.symbols.containsKey(name)) {
                    Symbol symbol = this.symbols.get(name);
                    if (symbol instanceof ImportedField importedField) {
                        PyExpr expr = switch (visit1) {
                            case PyExpr pyExpr -> pyExpr;
                            case String string -> new PyConstant(string, ctx.start.getLine());
                            case Byte byteValue -> new PyConstant(byteValue, ctx.start.getLine());
                            case Short shortValue -> new PyConstant(shortValue, ctx.start.getLine());
                            case Integer integer -> new PyConstant(integer, ctx.start.getLine());
                            case Boolean booleanValue -> new PyConstant(booleanValue, ctx.start.getLine());
                            case Long longValue -> new PyConstant(longValue, ctx.start.getLine());
                            case Float floatValue -> new PyConstant(floatValue, ctx.start.getLine());
                            case Double doubleValue -> new PyConstant(doubleValue, ctx.start.getLine());
                            case Character charValue -> new PyConstant(charValue, ctx.start.getLine());
                            default -> throw new RuntimeException("Expression for variable assignment wasn't found.");
                        };
                        importedField.set(mv, this, expr);
                    } else if (symbol instanceof PyVariable pyVariable) {
                        Type type = switch (visit1) {
                            case PyExpr pyExpr -> pyExpr.type(this);
                            case String string -> Type.getType(String.class);
                            case Byte byteValue -> Type.BYTE_TYPE;
                            case Short shortValue -> Type.SHORT_TYPE;
                            case Integer integer -> Type.INT_TYPE;
                            case Boolean booleanValue -> Type.BOOLEAN_TYPE;
                            case Long longValue -> Type.LONG_TYPE;
                            case Float floatValue -> Type.FLOAT_TYPE;
                            case Double doubleValue -> Type.DOUBLE_TYPE;
                            case Character charValue -> Type.CHAR_TYPE;
                            default -> throw new RuntimeException("Expression for variable assignment wasn't found.");
                        };
                        PyExpr expr = switch (visit1) {
                            case PyExpr pyExpr -> pyExpr;
                            case String string -> new PyConstant(string, ctx.start.getLine());
                            case Byte byteValue -> new PyConstant(byteValue, ctx.start.getLine());
                            case Short shortValue -> new PyConstant(shortValue, ctx.start.getLine());
                            case Integer integer -> new PyConstant(integer, ctx.start.getLine());
                            case Boolean booleanValue -> new PyConstant(booleanValue, ctx.start.getLine());
                            case Long longValue -> new PyConstant(longValue, ctx.start.getLine());
                            case Float floatValue -> new PyConstant(floatValue, ctx.start.getLine());
                            case Double doubleValue -> new PyConstant(doubleValue, ctx.start.getLine());
                            case Character charValue -> new PyConstant(charValue, ctx.start.getLine());
                            default -> throw new RuntimeException("Expression for variable assignment wasn't found.");
                        };

                        pyVariable.set(mv, this, expr);
                    }
                } else {
                    Type type = switch (visit1) {
                        case FuncCall funcCall -> funcCall.type(this);
                        case Symbol symbol -> symbol.type(this);
                        case Boolean booleanValue -> Type.BOOLEAN_TYPE;
                        case String stringValue -> Type.getType(String.class);
                        case Byte byteValue -> Type.BYTE_TYPE;
                        case Short shortValue -> Type.SHORT_TYPE;
                        case Integer integerValue -> Type.INT_TYPE;
                        case Long longValue -> Type.LONG_TYPE;
                        case Float floatValue -> Type.FLOAT_TYPE;
                        case Double doubleValue -> Type.DOUBLE_TYPE;
                        case Character charValue -> Type.CHAR_TYPE;
                        case PyConstant pyConstant -> pyConstant.type(this);
                        case PyExpr pyExpr -> pyExpr.type(this);
                        default -> throw new RuntimeException("Expression for variable assignment wasn't found.");
                    };
                    String s = importType(type);
                    createVariable(name, type, switch (visit1) {
                        case PyConstant pyConstant -> pyConstant;
                        case Symbol symbol -> symbol;
                        case String string -> new PyConstant(string, ctx.start.getLine());
                        case Integer integer -> new PyConstant(integer, ctx.start.getLine());
                        case Boolean booleanValue -> new PyConstant(booleanValue, ctx.start.getLine());
                        case Long longValue -> new PyConstant(longValue, ctx.start.getLine());
                        case Float floatValue -> new PyConstant(floatValue, ctx.start.getLine());
                        case Double doubleValue -> new PyConstant(doubleValue, ctx.start.getLine());
                        case Character charValue -> new PyConstant(charValue, ctx.start.getLine());
                        case Byte byteValue -> new PyConstant(byteValue, ctx.start.getLine());
                        case Short shortValue -> new PyConstant(shortValue, ctx.start.getLine());
                        case PyExpr pyExpr -> pyExpr;
                        default -> throw new RuntimeException("Expression for variable assignment wasn't found.");
                    }, false);

                    imports.remove(s);
                }

                return Unit.Instance;
            }

            PythonParser.AugassignContext augassign = ctx.augassign();
            if (augassign != null) {
                throw new RuntimeException("augassign not supported for:\n" + ctx.getText());
            }
            PythonParser.Star_expressionsContext starExpressionsContext = ctx.star_expressions();
            if (starExpressionsContext != null) {
                return visit(starExpressionsContext);
            }
            PythonParser.ExpressionContext expressionContext = ctx.expression();
            PythonParser.Annotated_rhsContext value = ctx.annotated_rhs();
            if (expressionContext != null) {
                if (value == null) {
                    throw new CompilerException("Annotated RHS is required " + getLocation(ctx));
                }
                System.out.println("annotatedRhsContext = " + value.getText());
                Object visit = visit(value);
                flags.set(F_CPL_TYPE_ANNO);
                Object visit1;
                try {
                    visit1 = visit(ctx.expression());
                } finally {
                    flags.clear(F_CPL_TYPE_ANNO);
                }

                if (mv == null) {
                    Type type = switch (visit1) {
                        case PyExpr symbol -> symbol.type(this);
                        default ->
                                throw new RuntimeException("Annotated RHS not supported with type " + visit1.getClass().getSimpleName() + " for:\n" + value.getText());
                    };
                    FieldVisitor fieldVisitor = cw.visitField(ACC_PUBLIC + ACC_STATIC, name, type.getDescriptor(), null, null);
                    fieldVisitor.visitEnd();

                    symbols.put(name, new ImportedField(name, type, getName(), ctx.start.getLine()));

                    if (cw == rootCw) {
                        var a = constant(ctx, visit);
                        a.load(rootInitMv, this, a.preload(rootInitMv, this, false), false);
                        rootInitMv.visitFieldInsn(PUTSTATIC, getName(), name, type.getDescriptor());
                    }
                } else {
                    createVariable(name, switch (visit1) {
                        case FuncCall funcCall -> funcCall.name();
                        case Symbol symbol -> symbol.name();
                        default ->
                                throw new RuntimeException("Annotated RHS not supported with type " + visit1.getClass().getSimpleName() + " for:\n" + value.getText());
                    }, constant(ctx, visit), false);
                }

                return Unit.Instance;
            } else {
                if (value != null) {
                    Type type = symbols.get(name).type(this);
                    Object visit = visit(value);
                    symbols.get(name).set(mv, this, switch (visit) {
                        case PyExpr pyExpr -> pyExpr;
                        default ->
                                throw new RuntimeException("Annotated RHS not supported with type " + visit.getClass().getSimpleName() + " for:\n" + value.getText());
                    });
                }
                throw new CompilerException("Type annotation is required " + getLocation(ctx));
            }
        } finally {
            flags.clear(F_CPL_ASSIGN);
        }
    }

    private String importType(Type type) {
        if (type == null) throw new RuntimeException("Can't import null type");
        if (type.getSort() == Type.ARRAY) {
            importType(type.getElementType());
            return null;
        }
        if (type.getSort() != Type.OBJECT) return null;
        Class clazz = null;
        try {
            clazz = Class.forName(type.getClassName(), false, getClass().getClassLoader());
            JClass value = new JClass(type.getClassName());
            String internalName = type.getInternalName();
            String[] split = internalName.split("/");
            String importedName = split[split.length - 1];
            imports.put(importedName, value);
            symbols.put(importedName, value);
            return importedName;
        } catch (ClassNotFoundException e) {
            PyClass value = classes.get(type.getClassName());
            if (value == null) throw new CompilerException("JVM Class not found: " + type.getClassName());
            String internalName = type.getInternalName();
            String[] split = internalName.split("/");
            String importedName = split[split.length - 1];
            imports.put(importedName, value);
            symbols.put(importedName, value);
            return importedName;
        }
    }

    public void loadConstant(ParserRuleContext ctx, Object visit1, MethodVisitor mv) {
        var a = constant(ctx, visit1);

        a.load(mv, this, a.preload(mv, this, false), false);
    }

    private static @NotNull PyExpr constant(ParserRuleContext ctx, Object visit1) {
        var a = switch (visit1) {
            case PyConstant pyConstant -> pyConstant;
            case Symbol symbol -> symbol;
            case PyExpr pyExpr -> pyExpr;
            case String string -> new PyConstant(string, ctx.getStart().getLine());
            case Integer integer -> new PyConstant(integer, ctx.getStart().getLine());
            case Boolean booleanValue -> new PyConstant(booleanValue, ctx.getStart().getLine());
            case Long longValue -> new PyConstant(longValue, ctx.getStart().getLine());
            case Float floatValue -> new PyConstant(floatValue, ctx.getStart().getLine());
            case Double doubleValue -> new PyConstant(doubleValue, ctx.getStart().getLine());
            case Character charValue -> new PyConstant(charValue, ctx.getStart().getLine());
            case Byte byteValue -> new PyConstant(byteValue, ctx.getStart().getLine());
            case Short shortValue -> new PyConstant(shortValue, ctx.getStart().getLine());
            default -> throw new RuntimeException("Expression for variable assignment wasn't found.");
        };
        return a;
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
        PythonParser.Bitwise_orContext otherContext = ctx.bitwise_or();

        Object value;
        Object addition = null;
        if (otherContext != null) {
            value = visit(otherContext);
            addition = visit(ctx.bitwise_xor());
        } else {
            value = visit(ctx.bitwise_xor());
        }
        Object finalValue = value;
        Object finalAddition = addition;
        if (flags.get(F_CPL_TYPE_ANNO)) {
            if (addition != null) {
                throw new RuntimeException("Binary operator is not allowed in type annotations (at " + fileName + ":" + ctx.getStart().getLine() + ":" + ctx.getStart().getCharPositionInLine() + ")");
            }

            if (value != null) {
                return value;
            }
        }
        PyEval.Operator operator = null;
        if (ctx.VBAR() != null) {
            operator = PyEval.Operator.OR;
        }
        return new PyEval(this, ctx, operator, finalValue, finalAddition);
    }

    @Override
    public Object visitBitwise_xor(PythonParser.Bitwise_xorContext ctx) {
        PythonParser.Bitwise_xorContext otherContext = ctx.bitwise_xor();

        Object value;
        Object addition = null;
        if (otherContext != null) {
            value = visit(otherContext);
            addition = visit(ctx.bitwise_and());
        } else {
            value = visit(ctx.bitwise_and());
        }
        Object finalValue = value;
        Object finalAddition = addition;
        PyEval.Operator operator = null;
        if (flags.get(F_CPL_TYPE_ANNO)) {
            if (addition != null) {
                throw new RuntimeException("Binary operator is not allowed in type annotations (at " + fileName + ":" + ctx.getStart().getLine() + ":" + ctx.getStart().getCharPositionInLine() + ")");
            }

            if (value != null) {
                return value;
            }
        }
        if (ctx.CIRCUMFLEX() != null) {
            operator = PyEval.Operator.XOR;
        }
        return new PyEval(this, ctx, operator, finalValue, finalAddition);
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
        List<PythonParser.Compare_op_bitwise_or_pairContext> compareOpBitwiseOrPairContexts = ctx.compare_op_bitwise_or_pair();
        if (bitwiseOrContext != null) {
            Object visit = visit(bitwiseOrContext);
            if (flags.get(F_CPL_TYPE_ANNO)) {
                if (!compareOpBitwiseOrPairContexts.isEmpty()) {
                    throw new RuntimeException("Comparison is not allowed in type annotations (at " + fileName + ":" + ctx.getStart().getLine() + ":" + ctx.getStart().getCharPositionInLine() + ")");
                }

                if (visit != null) {
                    return visit;
                }
            }
            if (!compareOpBitwiseOrPairContexts.isEmpty()) {
                for (PythonParser.Compare_op_bitwise_or_pairContext compareOpBitwiseOrPairContext : compareOpBitwiseOrPairContexts) {
                    Object visit1 = visit(compareOpBitwiseOrPairContext);
                    if (visit1 instanceof PyComparison comparison) {
                        return new PyExpr() {
                            @Override
                            public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
                                return null;
                            }

                            @Override
                            public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
                                loadExpr(ctx, visit);
                                comparison.load(mv, compiler, comparison.preload(mv, compiler, boxed), boxed);
                            }

                            @Override
                            public int lineNo() {
                                return 0;
                            }

                            @Override
                            public Type type(PythonCompiler compiler) {
                                return Type.BOOLEAN_TYPE;
                            }
                        };
                    } else {
                        throw new RuntimeException("compare_op_bitwise_or_pair not supported for:\n" + ctx.getText());
                    }
                }
            }
            return visit;
        }

        throw new RuntimeException("No supported matching comparison found for:\n" + ctx.getText());
    }

    @Override
    public Object visitCompare_op_bitwise_or_pair(PythonParser.Compare_op_bitwise_or_pairContext ctx) {
        PythonParser.Eq_bitwise_orContext eqBitwiseOrContext = ctx.eq_bitwise_or();
        if (flags.get(F_CPL_TYPE_ANNO)) {
            if (ctx.eq_bitwise_or() != null) {
                throw new RuntimeException("Equality is not allowed in type annotations (at " + fileName + ":" + ctx.getStart().getLine() + ":" + ctx.getStart().getCharPositionInLine() + ")");
            }
            if (ctx.noteq_bitwise_or() != null) {
                throw new RuntimeException("Inequality is not allowed in type annotations (at " + fileName + ":" + ctx.getStart().getLine() + ":" + ctx.getStart().getCharPositionInLine() + ")");
            }
            if (ctx.gt_bitwise_or() != null) {
                throw new RuntimeException("Relational operator is not allowed in type annotations (at " + fileName + ":" + ctx.getStart().getLine() + ":" + ctx.getStart().getCharPositionInLine() + ")");
            }
            if (ctx.gte_bitwise_or() != null) {
                throw new RuntimeException("Relational operator is not allowed in type annotations (at " + fileName + ":" + ctx.getStart().getLine() + ":" + ctx.getStart().getCharPositionInLine() + ")");
            }
            if (ctx.lt_bitwise_or() != null) {
                throw new RuntimeException("Relational operator is not allowed in type annotations (at " + fileName + ":" + ctx.getStart().getLine() + ":" + ctx.getStart().getCharPositionInLine() + ")");
            }
            if (ctx.lte_bitwise_or() != null) {
                throw new RuntimeException("Relational operator is not allowed in type annotations (at " + fileName + ":" + ctx.getStart().getLine() + ":" + ctx.getStart().getCharPositionInLine() + ")");
            }

            throw new RuntimeException("No supported matching compare_op_bitwise_or_pair found for:\n" + ctx.getText());
        }
        if (eqBitwiseOrContext != null) {
            return new PyComparison(eqBitwiseOrContext, PyComparison.Comparison.EQ, ctx);
        }
        PythonParser.Noteq_bitwise_orContext noteqBitwiseOrContext = ctx.noteq_bitwise_or();
        if (noteqBitwiseOrContext != null) {
            return new PyComparison(noteqBitwiseOrContext, PyComparison.Comparison.NE, ctx);
        }
        PythonParser.Gt_bitwise_orContext gtBitwiseOrContext = ctx.gt_bitwise_or();
        if (gtBitwiseOrContext != null) {
            return new PyComparison(gtBitwiseOrContext, PyComparison.Comparison.GT, ctx);
        }
        PythonParser.Gte_bitwise_orContext gteBitwiseOrContext = ctx.gte_bitwise_or();
        if (gteBitwiseOrContext != null) {
            return new PyComparison(gteBitwiseOrContext, PyComparison.Comparison.GTE, ctx);
        }
        PythonParser.Lt_bitwise_orContext ltBitwiseOrContext = ctx.lt_bitwise_or();
        if (ltBitwiseOrContext != null) {
            return new PyComparison(ltBitwiseOrContext, PyComparison.Comparison.LT, ctx);
        }
        PythonParser.Lte_bitwise_orContext lteBitwiseOrContext = ctx.lte_bitwise_or();
        if (lteBitwiseOrContext != null) {
            return new PyComparison(lteBitwiseOrContext, PyComparison.Comparison.LTE, ctx);
        }
        PythonParser.Notin_bitwise_orContext notinBitwiseOrContext = ctx.notin_bitwise_or();
        if (notinBitwiseOrContext != null) {
            throw new RuntimeException("compare_op_bitwise_or_pair not supported for:\n" + ctx.getText());
        }
        PythonParser.In_bitwise_orContext inBitwiseOrContext = ctx.in_bitwise_or();
        if (inBitwiseOrContext != null) {
            throw new RuntimeException("compare_op_bitwise_or_pair not supported for:\n" + ctx.getText());
        }
        throw new RuntimeException("No supported matching compare_op_bitwise_or_pair found for:\n" + ctx.getText());
    }

    @Override
    public Object visitBitwise_and(PythonParser.Bitwise_andContext ctx) {
        PythonParser.Bitwise_andContext otherContext = ctx.bitwise_and();

        Object value;
        Object addition = null;
        if (otherContext != null) {
            value = visit(otherContext);
            addition = visit(ctx.shift_expr());
        } else {
            value = visit(ctx.shift_expr());
        }
        Object finalValue = value;
        Object finalAddition = addition;
        PyEval.Operator operator = null;
        if (flags.get(F_CPL_TYPE_ANNO)) {
            if (addition != null) {
                throw new RuntimeException("Type annotation is not allowed in type annotations (at " + fileName + ":" + ctx.getStart().getLine() + ":" + ctx.getStart().getCharPositionInLine() + ")");
            }

            if (value != null) {
                return value;
            }
        }
        if (ctx.AMPER() != null) {
            operator = PyEval.Operator.AND;
        }
        return new PyEval(this, ctx, operator, finalValue, finalAddition);
    }

    @Override
    public Object visitShift_expr(PythonParser.Shift_exprContext ctx) {
        PythonParser.Shift_exprContext otherContext = ctx.shift_expr();

        Object value;
        Object addition = null;
        if (otherContext != null) {
            value = visit(otherContext);
            addition = visit(ctx.sum());
        } else {
            value = visit(ctx.sum());
        }
        Object finalValue = value;
        Object finalAddition = addition;
        PyEval.Operator operator = null;
        if (flags.get(F_CPL_TYPE_ANNO)) {
            if (addition != null) {
                throw new RuntimeException("Type annotation is not allowed in type annotations (at " + fileName + ":" + ctx.getStart().getLine() + ":" + ctx.getStart().getCharPositionInLine() + ")");
            }

            if (value != null) {
                return value;
            }
        }
        if (ctx.LEFTSHIFT() != null) {
            operator = PyEval.Operator.LSHIFT;
        } else if (ctx.RIGHTSHIFT() != null) {
            operator = PyEval.Operator.RSHIFT;
        }
        return new PyEval(this, ctx, operator, finalValue, finalAddition);
    }

    @Override
    public Object visitSum(PythonParser.SumContext ctx) {
        PythonParser.SumContext otherContext = ctx.sum();

        Object value;
        Object addition = null;
        if (otherContext != null) {
            value = visit(otherContext);
            addition = visit(ctx.term());
        } else {
            value = visit(ctx.term());
        }
        Object finalValue = value;
        Object finalAddition = addition;
        PyEval.Operator operator = null;
        if (flags.get(F_CPL_TYPE_ANNO)) {
            if (addition != null) {
                throw new RuntimeException("Type annotation is not allowed in type annotations (at " + fileName + ":" + ctx.getStart().getLine() + ":" + ctx.getStart().getCharPositionInLine() + ")");
            }

            if (value != null) {
                return value;
            }
        }
        if (ctx.PLUS() != null) {
            operator = PyEval.Operator.ADD;
        } else if (ctx.MINUS() != null) {
            operator = PyEval.Operator.SUB;
        }
        if (operator != null) {
            return new PyEval(this, ctx, operator, finalValue, finalAddition);
        }
        return value;
    }

    public void loadExpr(ParserRuleContext ctx, Object visit) {
        MethodVisitor mv = this.mv == null ? this.rootInitMv : this.mv;
        switch (visit) {
            case PyExpr pyExpr -> pyExpr.load(mv, this, pyExpr.preload(mv, this, false), false);
            case Integer integer -> loadConstant(ctx, integer, mv);
            case Long aLong -> loadConstant(ctx, aLong, mv);
            case Float aFloat -> loadConstant(ctx, aFloat, mv);
            case Double aDouble -> loadConstant(ctx, aDouble, mv);
            case String s -> loadConstant(ctx, s, mv);
            case Boolean aBoolean -> loadConstant(ctx, aBoolean, mv);
            case Character aChar -> loadConstant(ctx, aChar, mv);
            case Unit unit -> throw new RuntimeException("unit not supported for:\n" + ctx.getText());
            default -> throw new RuntimeException("No supported matching loadExpr found for:\n" + ctx.getText());
        }
    }

    @Override
    public Object visitTerm(PythonParser.TermContext ctx) {
        PythonParser.TermContext otherContext = ctx.term();

        Object value;
        Object addition = null;
        if (otherContext != null) {
            value = visit(otherContext);
            addition = visit(ctx.factor());
        } else {
            value = visit(ctx.factor());
        }
        Object finalValue = value;
        Object finalAddition = addition;
        PyEval.Operator operator = null;
        if (flags.get(F_CPL_TYPE_ANNO)) {
            if (addition != null) {
                throw new RuntimeException("Type annotation is not allowed in type annotations (at " + fileName + ":" + ctx.getStart().getLine() + ":" + ctx.getStart().getCharPositionInLine() + ")");
            }

            if (value != null) {
                return value;
            }
        }
        if (ctx.STAR() != null) {
            operator = PyEval.Operator.MUL;
        } else if (ctx.SLASH() != null) {
            operator = PyEval.Operator.DIV;
        } else if (ctx.PERCENT() != null) {
            operator = PyEval.Operator.MOD;
        } else if (ctx.DOUBLESLASH() != null) {
            operator = PyEval.Operator.FLOORDIV;
        }
        return new PyEval(this, ctx, operator, finalValue, finalAddition);
    }

    @Override
    public Object visitFactor(PythonParser.FactorContext ctx) {
        PythonParser.FactorContext factor = ctx.factor();
        if (ctx.MINUS() != null) {
            PythonParser.FactorContext otherContext = ctx.factor();

            Object value;
            if (otherContext != null) {
                value = visit(otherContext);
            } else {
                value = visit(ctx.power());
            }
            Object finalValue = value;
            PyEval.Operator operator = PyEval.Operator.UNARY_MINUS;
            if (flags.get(F_CPL_TYPE_ANNO)) {
                throw new RuntimeException("Unary operator is not allowed in type annotations (at " + fileName + ":" + ctx.getStart().getLine() + ":" + ctx.getStart().getCharPositionInLine() + ")");
            }
            return new PyEval(this, ctx, operator, finalValue, null);
        }

        if (ctx.PLUS() != null) {
            PythonParser.FactorContext otherContext = ctx.factor();

            Object value;
            if (otherContext != null) {
                value = visit(otherContext);
            } else {
                value = visit(ctx.power());
            }
            Object finalValue = value;
            PyEval.Operator operator = PyEval.Operator.UNARY_PLUS;
            if (flags.get(F_CPL_TYPE_ANNO)) {
                throw new RuntimeException("Unary operator is not allowed in type annotations (at " + fileName + ":" + ctx.getStart().getLine() + ":" + ctx.getStart().getCharPositionInLine() + ")");
            }
            return new PyEval(this, ctx, operator, finalValue, null);
        }

        if (ctx.TILDE() != null) {
            PythonParser.FactorContext otherContext = ctx.factor();

            Object value;
            if (otherContext != null) {
                value = visit(otherContext);
            } else {
                value = visit(ctx.power());
            }
            Object finalValue = value;
            PyEval.Operator operator = PyEval.Operator.UNARY_NOT;
            if (flags.get(F_CPL_TYPE_ANNO)) {
                throw new RuntimeException("Unary operator is not allowed in type annotations (at " + fileName + ":" + ctx.getStart().getLine() + ":" + ctx.getStart().getCharPositionInLine() + ")");
            }
            return new PyEval(this, ctx, operator, finalValue, null);
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
            PythonParser.FactorContext otherContext = ctx.factor();

            Object value;
            Object addition = null;
            if (otherContext != null) {
                value = visit(otherContext);
                addition = visit(ctx.await_primary());
            } else {
                value = visit(ctx.await_primary());
            }
            Object finalValue = value;
            Object finalAddition = addition;
            PyEval.Operator operator = PyEval.Operator.POW;
            if (flags.get(F_CPL_TYPE_ANNO)) {
                if (addition != null) {
                    throw new RuntimeException("Type annotation is not allowed in type annotations (at " + fileName + ":" + ctx.getStart().getLine() + ":" + ctx.getStart().getCharPositionInLine() + ")");
                }

                if (value != null) {
                    return value;
                }
            }
            return new PyEval(this, ctx, operator, finalValue, finalAddition);

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
                return new FuncCall(primary, primaryContext, arguments);
            } else {
                PythonParser.PrimaryContext primary = ctx.primary();
                if (primary == null) {
                    throw new RuntimeException("primary not supported for:\n" + ctx.getText());
                }
                return new FuncCall(primary, primaryContext, null);
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
        TerminalNode aTrue = ctx.TRUE();
        if (aTrue != null) {
            return true;
        }
        TerminalNode aFalse = ctx.FALSE();
        if (aFalse != null) {
            return false;
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

    @SuppressWarnings("unchecked")
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
                JClass jClass = new JClass(((String) visit(dottedNameContext)).replace(".", "/") + "/" + s.getValue());
                this.imports.put(s.getKey(), jClass);
                this.symbols.put(s.getKey(), jClass);
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
        rootCw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        rootCw.visit(V1_8, ACC_PUBLIC, getName(), null, "java/lang/Object", null);
        cw = rootCw;

        rootInitMv = rootCw.visitMethod(ACC_PUBLIC | ACC_STATIC, "<clinit>", "()V", null, null);
        rootInitMv.visitCode();

        // Set the file name of the class to fileName
        rootCw.visitSource(path + fileName + ".py", null);

        FileContext context = new FileContext();
        pushContext(context);

        if (statements == null) {
            return Unit.Instance;
        }

        visit(statements);

        popContext();

        while (context.needsPop()) {
            context.pop();
            rootInitMv.visitInsn(POP);
        }

        if (mv != null) {
            throw new AssertionError("mv != null");
        }

        rootInitMv.visitInsn(RETURN);
        rootInitMv.visitMaxs(0, 0);
        rootInitMv.visitEnd();
        rootInitMv = null;
        rootCw.visitEnd();
        try {
            File file = new File("build/tmp/compilePython/" + getName() + ".class");
            File parentFile = file.getParentFile();
            if (!parentFile.exists()) {
                parentFile.mkdirs();
            }
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(rootCw.toByteArray());
            fos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            rootCw = null;
            cw = null;
        }
        return Unit.Instance;
    }

    private String getName() {
        return path + CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, fileName) + "Py";
    }

    public void compile(File file, File rootDir) throws IOException {
        String absolutePath = file.getAbsolutePath();
        String absolutePath1 = rootDir.getAbsolutePath();
        if (!absolutePath1.endsWith("/")) {
            absolutePath1 += "/";
        }
        if (!absolutePath.startsWith(absolutePath1)) {
            throw new RuntimeException("File is not in the root directory");
        }
        String path1 = absolutePath.replaceFirst(absolutePath1, "");
        PythonLexer lexer = new PythonLexer(CharStreams.fromPath(file.toPath().toAbsolutePath()));
        PythonParser parser = new PythonParser(new CommonTokenStream(lexer));
        PythonParser.File_inputContext fileInputContext = parser.file_input();
        var p = path1.substring(0, path1.length() - ".py".length());
        this.rootDir = rootDir.getAbsolutePath();
        this.fileName = p.substring(p.lastIndexOf("/") + 1);
        this.path = p.substring(0, p.lastIndexOf("/") + 1);

        try {
            visit(fileInputContext);
        } catch (CompilerException e) {
            e.printStackTrace();
            compileErrors.add(e);
        }
    }

    public void compile(String python, String fileName) throws IOException {
        PythonLexer lexer = new PythonLexer(CharStreams.fromString(python));
        PythonParser parser = new PythonParser(new CommonTokenStream(lexer));
        PythonParser.File_inputContext fileInputContext = parser.file_input();
        var p = fileName.substring(0, fileName.length() - ".py".length());
        this.fileName = p.substring(p.lastIndexOf("/") + 1);
        this.path = p.substring(0, p.lastIndexOf("/") + 1);

        try {
            visit(fileInputContext);
        } catch (CompilerException e) {
            e.printStackTrace();
            compileErrors.add(e);
        }
    }

    public void pack(String outputDir, String outputJar) {
        // Pack "build/rustc/**/*.class"
        try {
            Process process = Runtime.getRuntime().exec("jar cf " + outputJar + " -C " + outputDir + " .");
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    static class PythonListener extends PythonParserBaseListener {
        private final ClassWriter rootClassWriter;
        private final String fileName;

        public PythonListener(ClassWriter rootClassWriter, String fileName) {
            this.rootClassWriter = rootClassWriter;
            this.fileName = fileName;
        }
    }

    static void throwError(MethodVisitor mv, String value) {
        mv.visitTypeInsn(NEW, "java/lang/Error");
        mv.visitInsn(DUP);
        mv.visitLdcInsn(value);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Error", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitInsn(ATHROW);
    }

    int createVariable(String name, String type, PyExpr expr, boolean boxed) {
        mv.visitLineNumber(expr.lineNo(), new Label());

        Symbol symbol = symbols.get(type);
        if (symbol == null) {
            throw new CompilerException("Symbol '" + type + "' not found (" + getLocation(expr) + ")");
        }
        Type type1 = symbol.type(this);
        int computationalType = 1;
        if (type1 == null) {
            type1 = switch (type) {
                case "str" -> Type.getType(String.class);
                case "int", "jlong" -> Type.getType(boxed ? Long.class : long.class);
                case "float", "jfloat" -> Type.getType(boxed ? Float.class : float.class);
                case "bool", "jboolean" -> Type.getType(boxed ? Boolean.class : boolean.class);
                case "bytes", "bytearray" -> Type.getType(byte[].class);
                case "list" -> Type.getType(List.class);
                case "dict" -> Type.getType(Map.class);
                case "set" -> Type.getType(Set.class);
                case "tuple" -> Type.getType(Object[].class);
                case "object" -> Type.getType(Object.class);
                case "type" -> Type.getType(Class.class);
                case "jint" -> Type.getType(boxed ? Integer.class : int.class);
                case "jbyte" -> Type.getType(boxed ? Byte.class : byte.class);
                case "jchar" -> Type.getType(boxed ? Character.class : char.class);
                case "jdouble" -> Type.getType(boxed ? Double.class : double.class);
                case "jshort" -> Type.getType(boxed ? Short.class : short.class);
                case "None" -> throw new RuntimeException("None not supported");
                case "NoneType" -> throw new RuntimeException("NoneType not supported");
                default -> {
                    if (imports.get(type) == null) {
                        throw typeNotFound(type, expr);
                    }

                    yield imports.get(type).type(this);
                }
            };
        }

        Object preloaded = expr.preload(mv, this, false);
        expr.load(mv, this, preloaded, boxed);

        Label label = new Label();
        symbols.put(name, new PyVariable(name, type1, currentVariableIndex, expr.lineNo(), true, label));
//        writer.label(label);
        mv.visitLocalVariable(name, type1.getDescriptor(), null, endLabel, endLabel, currentVariableIndex);
        mv.visitLineNumber(expr.lineNo(), label);

        writer.box(writer.unboxType(type1));
        writer.getContext().pop();
        mv.visitVarInsn(ASTORE, currentVariableIndex);

        return currentVariableIndex++;
    }

    int createVariable(String name, Type type, PyExpr expr, boolean boxed) {
        mv.visitLineNumber(expr.lineNo(), new Label());

        int index = 1;
        Object preloaded = expr.preload(mv, this, false);
        expr.load(mv, this, preloaded, boxed);
        writer.getContext().push(type);

        Label label = new Label();
        symbols.put(name, new PyVariable(name, type, currentVariableIndex, expr.lineNo(), false, label));
//        writer.label(label);
        writer.localVariable(name, Type.getType(Object.class).getDescriptor(), null, endLabel, endLabel, currentVariableIndex);
        mv.visitLineNumber(expr.lineNo(), label);
        int opcode;
        typeCheck(type, expr);
        Context context = writer.getContext();
        context.pop();

        writer.box(writer.unboxType(type));
        writer.getContext().pop();
        mv.visitVarInsn(ASTORE, currentVariableIndex);

        return currentVariableIndex++;
    }

    public Type typeCheck(Type type, PyExpr expr) {
        if (!type.equals(Type.getType(String.class)) && !type.equals(Type.LONG_TYPE) && !type.equals(Type.DOUBLE_TYPE)
                && !type.equals(Type.FLOAT_TYPE) && !type.equals(Type.INT_TYPE) && !type.equals(Type.BOOLEAN_TYPE)
                && !type.equals(Type.BYTE_TYPE) && !type.equals(Type.SHORT_TYPE)
                && symbols.get(type.getClassName().substring(type.getClassName().lastIndexOf('.') + 1)) == null) {
            if (type.getSort() == Type.ARRAY) {
                Type actualType = type.getElementType();
                while (actualType.getSort() == Type.ARRAY) {
                    actualType = actualType.getElementType();
                }
                if (!actualType.equals(Type.getType(String.class)) && !actualType.equals(Type.LONG_TYPE) && !actualType.equals(Type.DOUBLE_TYPE)
                        && !actualType.equals(Type.FLOAT_TYPE) && !actualType.equals(Type.INT_TYPE) && !actualType.equals(Type.BOOLEAN_TYPE)
                        && !actualType.equals(Type.BYTE_TYPE) && !actualType.equals(Type.SHORT_TYPE)
                        && symbols.get(actualType.getClassName().substring(actualType.getClassName().lastIndexOf('.') + 1)) == null) {
                    throw typeNotFound(actualType.getClassName(), expr);
                }

                return actualType;
            } else {
                throw typeNotFound(type.getClassName(), expr);
            }
        }
        return type;
    }

    CompilerException typeNotFound(String type, PyExpr expr) {
        return new CompilerException("Type '" + type + "' not found " + getLocation(expr));
    }

    CompilerException jvmClassNotFound(String type, PyExpr expr) {
        return new CompilerException("JVM Class '" + type + "' not found " + getLocation(expr));
    }

    CompilerException functionNotFound(Type owner, String name, PyExpr expr) {
        return new CompilerException("Function '" + name + "' not found in '" + owner.getClassName() + "' " + getLocation(expr));
    }

    CompilerException functionNotFound(Type owner, String name, Type[] args, PyExpr expr) {
        return new CompilerException("Function '" + name + "' not found that matches (" + Arrays.stream(args).map(Type::getClassName).collect(Collectors.joining(", ")) + ") in '" + owner.getClassName() + "' " + getLocation(expr));
    }

    String getLocation(PyExpr expr) {
        return Path.of(rootDir, path + fileName) + ".py:" + expr.lineNo();
    }

    String getLocation(ParserRuleContext ctx) {
        return "(" + Path.of(rootDir, path + fileName) + ".py:" + ctx.start.getLine() + ":" + (ctx.start.getCharPositionInLine() + 1) + ")";
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
            Object visit = visit(expressionContext);

            if (visit == null) {
                throw new RuntimeException("Unknown visitArgs for expression context: " + expressionContext.getText());
            }


            if (!flags.get(F_DYN_CALL) && !flags.get(F_CPL_CLASS_INHERITANCE) && rootCw != cw) {
                switch (visit) {
                    case String s -> writer.loadConstant(s);
                    case Integer s -> writer.loadConstant(s);
                    case Float s -> writer.loadConstant(s);
                    case Long s -> writer.loadConstant(s);
                    case Double s -> writer.loadConstant(s);
                    case Character s -> writer.loadConstant(s);
                    case Byte s -> writer.loadConstant(s);
                    case Short s -> writer.loadConstant(s);
                    case Boolean s -> writer.loadConstant(s);
                    case Symbol symbol -> {
                        if (mv == null) {
                            symbol.preload(mv, this, false);
                        } else {
                            symbol.load(mv, this, symbol.preload(mv, this, false), false);
                        }
                    }
                    case PyExpr expr -> expr.load(mv, this, expr.preload(mv, this, false), false);
                    case null, default ->
                            throw new UnsupportedOperationException("Not implemented: " + visit.getClass());
                }
            } else if (flags.get(F_CPL_CLASS_INHERITANCE) || rootCw == cw) {
                switch (visit) {
                    case String s -> args.add(new PyConstant(s, ctx.getStart().getLine()));
                    case Integer s -> args.add(new PyConstant(s, ctx.getStart().getLine()));
                    case Float s -> args.add(new PyConstant(s, ctx.getStart().getLine()));
                    case Long s -> args.add(new PyConstant(s, ctx.getStart().getLine()));
                    case Double s -> args.add(new PyConstant(s, ctx.getStart().getLine()));
                    case Character s -> args.add(new PyConstant(s, ctx.getStart().getLine()));
                    case Byte s -> args.add(new PyConstant(s, ctx.getStart().getLine()));
                    case Short s -> args.add(new PyConstant(s, ctx.getStart().getLine()));
                    case Boolean s -> args.add(new PyConstant(s, ctx.getStart().getLine()));
                    case PyExpr expr -> {
                        args.add(expr);
                    }
                    default ->
                            throw new UnsupportedOperationException("Not implemented: " + visit.getClass().getName());
                }
            }
        }

        return args;
    }
}
