package dev.ultreon.pythonc;

import com.google.common.base.CaseFormat;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

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
    public static final int F_READ_CALL = 41;
    public static final int F_DYN_CALL = 50;

    public static final String E_NOT_ALLOWED = "Not allowed";
    public static final String FMT_CLASS = "L%s;";
    public static CompileExpectations expectations = new CompileExpectations();
    public static Path rootDir;
    public Builtins builtins = new Builtins(this);

    private ClassWriter rootCw;
    Set<String> undefinedClasses = new LinkedHashSet<>();
    private ClassVisitor cv = rootCw;
    private Path pathOfFile;
    private String path = "";
    private String fileName = "Main";
    final Set<String> implementing = new HashSet<>();
    final Map<String, Symbol> symbols = new HashMap<>();
    final PyImports imports = new PyImports(this);
    private State state = State.File;
    final BitSet flags = new BitSet();
    final Decorators decorators = new Decorators();
    MethodVisitor mv;
    MethodVisitor rootInitMv;
    int currentVariableIndex = 1;
    Label endLabel;
    Label startLabel;
    Label curLabel;
    PyClasses classes = new PyClasses(this);
    static JvmClassCache classCache = new JvmClassCache();
    private PyClass curPyClass = null;
    private final List<CompilerException> compileErrors = new ArrayList<>();
    private Label elifLabel;
    final Stack<Context> contextStack = new Stack<>();

    public final JvmWriter writer = new JvmWriter(this);
    @Nullable
    public PyCompileClass compilingClass;
    @Nullable
    public PyClass definingClass;
    @Nullable
    public PyModule definingModule;
    @Nullable PyClass definingInstance;
    private PyFunction definingFunction;
    private PythonParser.ArgumentsContext callArgs;
    private Stack<MemberContext> memberContextStack = new Stack<>();
    private MemberContext memberContext;
    private ClassNode cNode;

    public static boolean isInstanceOf(PythonCompiler pc, Type pop1, String owner) {
        Type type = Type.getObjectType(owner);
        JvmClass require = classCache.require(pc, type);
        JvmClass pop = classCache.require(pc, pop1);
        return pop.doesInherit(pc, require);
    }

    public static JvmClass getType(Class<Object> objectClass) {
        return classCache.require(null, Type.getObjectType(objectClass.getName()));
    }

    public void compileSources(String sourceDir) {
        rootDir = Path.of(sourceDir).toAbsolutePath();

        // Walk the directory
        Path path = Path.of(System.getProperty("user.dir")).relativize(Path.of(sourceDir));
        try {
            try (Stream<Path> walk = Files.walk(path)) {
                walk
                        .map(Path::toString)
                        .filter(string -> string.endsWith(".py"))
                        .collect(Collectors.toSet())
                        .forEach(v -> {
                            try {
                                compile(new File(v), new File(sourceDir));
                            } catch (CompilerException e) {
                                compileErrors.add(e);
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
                try {
                    System.err.println(ex.toAdvancedString());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
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
        } else if (symbol instanceof PyImport pyImport && pyImport.symbol instanceof JvmClass jvmClass) {
            return jvmClass;
        }

        undefinedClasses.add(className);
        return null;
    }

    public String pyName(Type owner) {
        if (owner.getSort() == Type.BYTE) return "jbyte";
        if (owner.getSort() == Type.SHORT) return "jshort";
        if (owner.getSort() == Type.INT) return "jint";
        if (owner.getSort() == Type.LONG) return "int";
        if (owner.getSort() == Type.FLOAT) return "jfloat";
        if (owner.getSort() == Type.DOUBLE) return "float";
        if (owner.getSort() == Type.BOOLEAN) return "bool";
        if (owner.getSort() == Type.CHAR) return "jchar";
        if (owner.getSort() == Type.ARRAY) return "typing.Tuple";
        if (owner.getSort() == Type.OBJECT && owner.getClassName().equals("java/lang/String")) return "str";
        if (owner.getSort() == Type.OBJECT && owner.getClassName().equals("java/util/List")) return "list";
        if (owner.getSort() == Type.OBJECT && owner.getClassName().equals("java/util/Map")) return "dict";
        if (owner.getSort() == Type.OBJECT && owner.getClassName().equals("java/util/Set")) return "set";
        if (owner.getSort() == Type.OBJECT && owner.getClassName().equals("java/lang/Iterable"))
            return "typing.Iterable";
        if (owner.getSort() == Type.OBJECT && owner.getClassName().equals("java/lang/Enum")) return "typing.Enum";
        if (owner.getSort() == Type.OBJECT && owner.getClassName().equals("java/lang/Enum$Value")) return "typing.Enum";

        PythonCompiler.classCache.load(this, owner);
        return PythonCompiler.classCache.get(owner).name();
    }

    enum State {
        File, Class, Function, Decorators
    }

    public PythonCompiler() {
        for (PyBuiltinClass builtinClass : builtins.getClasses()) {
            this.symbols.put(builtinClass.pyName, builtinClass);
            this.imports.add(builtinClass.pyName, builtinClass);
        }

        for (PyBuiltinFunction builtinFunction : builtins.getFunctions()) {
            this.symbols.put(builtinFunction.name, builtinFunction);
        }

        classCache.init(this);
    }

    @Override
    public Object visit(@Nullable ParseTree tree) {
        if (tree == null) {
            throw new RuntimeException("Tree is null");
        }
        System.out.print("Visiting: " + tree.getClass().getSimpleName() + " " + tree.getText());
        if (!contextStack.isEmpty()) {
            System.out.println(" (Stack size before: " + getContext(Context.class).stackSize() + ")");
        } else {
            System.out.println();
        }

        if (tree instanceof ParserRuleContext context) writer.lineNumber(context.getStart().getLine());
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
                if (!compileErrors.isEmpty()) {
                    return ErrorValue.Instance;
                }
                if (statement == null)
                    throw new RuntimeException("Didn't fully pop before statement " + (i - 1) + " for:\n" + ctx.getText());
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
                                pyExpr.expectReturnType(this, classCache.require(this, Object.class), new Location(pathOfFile.toString(), ctx.start.getLine(), ctx.start.getCharPositionInLine(), ctx.stop.getLine(), ctx.stop.getCharPositionInLine()));
                                pyExpr.load(mv, this, pyExpr.preload(mv, this, false), false);
                                if (pyExpr.type(this) != Type.VOID_TYPE) {
                                    writer.pop();
                                }

                                while (context.needsPop()) {
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
        writer.label(loopStart);

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
        writer.jump(loopStart);

        if (elseBlock != null) {
            writer.label(elseBlock);

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
        writer.label(loopEnd);

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
                    writer.box(Type.BOOLEAN_TYPE);
                    writer.cast(Type.getType(Object.class));
                    writer.pushTrue();
                    writer.box(Type.BOOLEAN_TYPE);
                    writer.cast(Type.getType(Object.class));
                }
                case PyExpr pyExpr -> {
                    if (mv == null) {
                        throw new RuntimeException("Not writing a method:\n" + ctx.getText());
                    }

                    Type type = pyExpr.type(this);
                    if (type.equals(Type.getType(Object.class))) {
                        pyExpr.load(mv, this, pyExpr.preload(mv, this, false), false);
                        writer.cast(Type.getType(Boolean.class));
                        writer.unbox(Type.BOOLEAN_TYPE);
                        writer.pushTrue();
                        writer.unbox(Type.BOOLEAN_TYPE);
                    } else if (type != Type.BOOLEAN_TYPE) {
                        throw new RuntimeException("Expected boolean, got " + type.getClassName());
                    } else {
                        pyExpr.load(mv, this, pyExpr.preload(mv, this, false), false);
                        writer.box(Type.BOOLEAN_TYPE);
                        writer.cast(Type.getType(Object.class));
                        writer.pushTrue();
                        writer.unbox(Type.BOOLEAN_TYPE);
                    }
                }
                case Boolean b -> {
                    if (mv == null) {
                        throw new RuntimeException("Not writing a method:\n" + ctx.getText());
                    }

                    writer.loadConstant(b);
                    writer.unbox(Type.BOOLEAN_TYPE);
                    writer.pushTrue();
                    writer.unbox(Type.BOOLEAN_TYPE);
                }
                case Integer i -> {
                    if (mv == null) {
                        throw new RuntimeException("Not writing a method:\n" + ctx.getText());
                    }
                    writer.loadConstant(i);
                    writer.unbox(Type.BOOLEAN_TYPE);
                    writer.pushTrue();
                }
                case String s -> throw new RuntimeException("String is not a condition:\n" + ctx.getText());
                case Float f -> throw new RuntimeException("Float is not a condition:\n" + ctx.getText());
                case Long l -> throw new RuntimeException("Long is not a condition:\n" + ctx.getText());
                case Double d -> throw new RuntimeException("Double is not a condition:\n" + ctx.getText());
                case Unit unit -> throw new RuntimeException("Unit is not a condition:\n" + ctx.getText());
                case null, default -> {
                    if (visit != null) {
                        throw new RuntimeException("statement not supported for: " + visit.getClass().getSimpleName());
                    }
                }
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

        if (decorators.byJvmName.containsKey("org/pythonutils/Override")) {
            // Ignore for now
        }

        boolean static_ = decorators.byJvmName.containsKey("org/python/builtins/PyStaticmethod") || cv == rootCw;
        boolean class_ = decorators.byJvmName.containsKey("org/python/builtins/PyClassmethod");

        if (static_) {
            flags.set(F_CPL_STATIC_FUNC);
        } else if (class_) {
            flags.set(F_CPL_CLASS_FUNC);
        } else {
            flags.set(F_CPL_INSTANCE_FUNC);
        }

        List<PyParameter> parmeters = new ArrayList<>();
        if (params != null) {
            Object visit = visit(params);
            if (visit == null) {
                throw new RuntimeException("params not supported for:\n" + ctx.getText());
            }
            if (!(visit instanceof List<?> list)) {
                throw new RuntimeException("Not a list:\n" + visit.getClass().getSimpleName());
            }
            for (Object o : list) {
                if (o instanceof TypedName typedName) {
                    parmeters.add(new PyParameter(typedName, 0, typedName.type(), null));
                } else if (!(o instanceof PyParameter pyParameter)) {
                    throw new RuntimeException("Not a PyParameter:\n" + ctx.getText());
                } else {
                    parmeters.add(pyParameter);
                }
            }
        }

        StringBuilder signature = new StringBuilder();
        for (PyParameter typedName : parmeters) {
            if (typedName.type() == null) {
                signature.append("Ljava/lang/Object;");
                continue;
            }
            if (builtins.getClass(typedName.type()) != null) {
                signature.append("L").append(typedName.typedName().type().getInternalName()).append(";");
                continue;
            }
            Symbol symbol = imports.get(typedName.type());
            if (symbol == null) {
                throw new RuntimeException("No import found for " + typedName.typedName().name() + " " + getLocation(ctx));
            }
            String s = symbol.name();
            if (s == null) {
                throw new RuntimeException("No import found for " + symbol.type(this).getClassName() + " " + getLocation(ctx));
            }
            signature.append("L").append(typedName.typedName()).append(";");
        }

        StringJoiner joiner = new StringJoiner("");
        for (PyParameter typedName : parmeters) {
            Type type = typedName.type();
            if (type == null) type = Type.getType(Object.class);
            joiner.add(type.getDescriptor());
        }
        String sig = joiner.toString();

        Type returnType = Type.getType(Object.class);
        if (ctx.expression() != null) {
            Object visit = visit(ctx.expression());
            if (visit == null) {
                throw new RuntimeException("expression not supported for:\n" + ctx.getText());
            }
            if (visit instanceof PyExpr pyExpr) {
                returnType = pyExpr.type(this);
                if (returnType == null) {
                    throw new RuntimeException("No type found for: " + pyExpr.getClass().getName());
                }
            } else {
                throw new RuntimeException("Not a PyExpr:\n" + ctx.getText());
            }
        }

        if (name.getText().equals("__init__")) {
            List<JvmFunction> jvmFunctions = definingClass.methods.get("<init>");
            if (jvmFunctions == null || jvmFunctions.isEmpty()) {
                mv = rootInitMv;
                MethodNode initMv = (MethodNode) rootInitMv;
                initMv.desc = "(" + sig + ")V";

                MethodNode ctorInitMv = new MethodNode(ACC_PUBLIC, "<init>", "(" + sig + ")V", null, null);
                ctorInitMv.visitCode();
                ctorInitMv.visitVarInsn(ALOAD, 0);
                ctorInitMv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

                for (JvmClass jvmClass : definingClass.dynamicSuperClasses(this)) {
                    if (jvmClass instanceof PyInheritorClass pyInheritorClass) {
                        callPyInit(ctx, jvmClass, ctorInitMv, parmeters, sig);
                    }
                }

                callPyInit(ctx, definingClass, ctorInitMv, parmeters, sig);

                ctorInitMv.visitInsn(RETURN);
                ctorInitMv.visitMaxs(1, 1);
                ctorInitMv.visitEnd();

                ctorInitMv.accept(cv);
            } else {
                mv = cv.visitMethod(ACC_PUBLIC, "<init>", "(" + sig + ")" + returnType.getDescriptor(), null, null);
            }
        } else if (name.getText().startsWith("__")) {
            mv = cv.visitMethod(ACC_PRIVATE + (static_ ? ACC_STATIC : 0) + (class_ ? ACC_STATIC : 0), name.getText().substring(2), "(" + sig + ")" + returnType.getDescriptor(), null, null);
        } else {
            int access = ACC_PUBLIC + (static_ ? ACC_STATIC : 0) + (class_ ? ACC_STATIC : 0);
            mv = cv.visitMethod(access, name.getText(), "(" + sig + ")" + returnType.getDescriptor(), null, null);
        }

        if (definingInstance != null) {
            currentVariableIndex = 2;
        }
        PyClass oldDefiningInstance = definingInstance;
        Map<String, Symbol> oldSymbols = null;
        if (definingClass != null) {
            definingInstance = definingClass;
        }
        PyFunction oldDefiningFunction = definingFunction;
        boolean isStatic = false; // TODO
        PyFunction func = name.getText().equals("__init__")
                ? (definingFunction = new PyConstructor(this, definingClass, parmeters.stream().map(PyParameter::type).toArray(Type[]::new), returnType, getLocation(ctx)))
                : (definingFunction = new PyFunction(this, definingClass == null ? definingModule : definingClass, name.getText(), parmeters.stream().map(PyParameter::type).toArray(Type[]::new), returnType, this.getLocation(ctx), definingClass == null || isStatic));

        pushMemberContext(MemberContext.VAR);
        for (PyParameter typedName : parmeters) {
            mv.visitParameter(typedName.typedName().name(), 0);
            definingFunction.createParam(typedName.typedName().name(), new PyVariable(typedName.typedName().name(), typedName.type(), currentVariableIndex, null, getLocation(ctx)));
        }

        endLabel = new Label();
        endLabel.info = 100000;
        startLabel = new Label();
        startLabel.info = 0;

        try {
            if (mv != rootInitMv) mv.visitCode();

            if (decorators.byJvmName.containsKey("org/pythonutils/Override")) {
                // Ignore for now
            }

            Label blockStart = new Label();
            FunctionContext functionContext = new FunctionContext();
            pushContext(functionContext);

            visit(block);
            definingFunction = oldDefiningFunction;
            definingInstance = oldDefiningInstance;
            symbols.put(func.name(), func);

            popContext();
            while (functionContext.needsPop()) {
                writer.pop();
            }

            if (mv != rootInitMv) {
                if (mv instanceof MethodNode) {
                    AbstractInsnNode last = ((MethodNode) mv).instructions.getLast();
                    if ((last == null || last.getOpcode() != IRETURN) && last.getOpcode() != ARETURN && last.getOpcode() != DRETURN && last.getOpcode() != FRETURN && last.getOpcode() != LRETURN && last.getOpcode() != RETURN) {
                        doReturn(returnType);
                    }
                } else {
                    doReturn(returnType);
                }
            }
            if (mv != rootInitMv) writer.end();
            popMemberContext();
        } catch (CompilerException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            flags.clear(F_CPL_STATIC_FUNC);
            flags.clear(F_CPL_CLASS_FUNC);
            flags.clear(F_CPL_INSTANCE_FUNC);

            mv = null;
        }

        if (definingModule != null) {
            if (name.getText().equals("__init__")) {
                if (!(func instanceof PyConstructor)) {
                    throw new AssertionError("Why is this not a constructor?");
                }
                definingModule.methods.computeIfAbsent("<init>", v -> new ArrayList<>()).add(func);
            } else {
                definingModule.methods.computeIfAbsent(name.getText(), v -> new ArrayList<>()).add(func);
            }
        }

        if (curPyClass != null) {
            if (name.getText().equals("__init__")) {
                if (!(func instanceof PyConstructor)) {
                    throw new AssertionError("Why is this not a constructor?");
                }
                curPyClass.methods.computeIfAbsent("<init>", v -> new ArrayList<>()).add(func);
            } else {
                curPyClass.methods.computeIfAbsent(name.getText(), v -> new ArrayList<>()).add(func);
            }
        }
        return Unit.Instance;
    }

    private void callPyInit(PythonParser.Function_def_rawContext ctx, JvmClass jvmClass, MethodNode ctorInitMv, List<PyParameter> parmeters, String sig) {
        // Call <inheritor>.super.__init__(<arguments>);
        ctorInitMv.visitVarInsn(ALOAD, 0);
        int index = 1;
        for (PyParameter typedName : parmeters) {
            Type type = typedName.type();
            if (type == null) {
                ctorInitMv.visitVarInsn(ALOAD, index);
                index++;
                continue;
            }
            switch (type.getSort()) {
                case Type.ARRAY -> throw new RuntimeException("Array not supported for:\n" + ctx.getText());
                case Type.OBJECT -> {
                    Symbol symbol = imports.get(type);
                    if (symbol == null) {
                        throw new RuntimeException("No import found for " + type + " " + getLocation(ctx));
                    }

                    String s = symbol.name();
                    if (s == null) {
                        throw new RuntimeException("No import found for " + symbol.type(this) + " " + getLocation(ctx));
                    }
                    ctorInitMv.visitVarInsn(ALOAD, index);
                    index++;
                }
                case Type.BYTE, Type.SHORT, Type.INT, Type.BOOLEAN, Type.CHAR -> {
                    ctorInitMv.visitVarInsn(ILOAD, index);
                    index++;
                }
                case Type.LONG -> {
                    ctorInitMv.visitVarInsn(LLOAD, index);
                    index += 2;
                }
                case Type.FLOAT -> {
                    ctorInitMv.visitVarInsn(FLOAD, index);
                    index++;
                }
                case Type.DOUBLE -> {
                    ctorInitMv.visitVarInsn(DLOAD, index);
                    index += 2;
                }
                default -> throw new RuntimeException("Type not supported for:\n" + ctx.getText());
            }
        }

        ctorInitMv.visitMethodInsn(INVOKEVIRTUAL, jvmClass.type(this).getInternalName(), "__init__", "(" + sig + ")V", false);
    }

    private void doReturn(Type returnType) {
        if (returnType.getSort() == Type.VOID) {
            writer.returnVoid();
        } else if (returnType.getSort() == Type.ARRAY) {
            writer.pushNull();
            writer.returnObject();
        } else if (returnType.getSort() == Type.OBJECT) {
            writer.pushNull();
            writer.returnObject();
        } else if (returnType.getSort() == Type.CHAR) {
            writer.pushInt(0);
            writer.returnChar();
        } else if (returnType.getSort() == Type.BOOLEAN) {
            writer.pushBoolean(false);
            writer.returnBoolean();
        } else if (returnType.getSort() == Type.BYTE) {
            writer.pushInt(0);
            writer.returnByte();
        } else if (returnType.getSort() == Type.SHORT) {
            writer.pushInt(0);
            writer.returnShort();
        } else if (returnType.getSort() == Type.INT) {
            writer.pushInt(0);
            writer.returnInt();
        } else if (returnType.getSort() == Type.FLOAT) {
            writer.pushFloat(0f);
            writer.returnFloat();
        } else if (returnType.getSort() == Type.DOUBLE) {
            writer.pushDouble(0.0);
            writer.returnDouble();
        } else if (returnType.getSort() == Type.LONG) {
            writer.pushLong(0L);
            writer.returnLong();
        }
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
        List<PyParameter> typedNames = new ArrayList<>();
        if (ctx.star_etc() != null) {
            if (ctx.param_no_default() != null && !ctx.param_no_default().isEmpty()) {
                throw new RuntimeException("param_no_default not supported for:\n" + ctx.getText());
            }
            if (ctx.param_with_default() != null && !ctx.param_with_default().isEmpty()) {
                throw new RuntimeException("param_with_default not supported for:\n" + ctx.getText());
            }
            if (ctx.slash_no_default() != null && !ctx.slash_no_default().isEmpty()) {
                throw new RuntimeException("slash_no_default not supported for:\n" + ctx.getText());
            }
            if (ctx.slash_with_default() != null && !ctx.slash_with_default().isEmpty()) {
                throw new RuntimeException("slash_with_default not supported for:\n" + ctx.getText());
            }

            return ctx.star_etc().accept(this);
        }

        for (int i = 0; i < ctx.param_no_default().size(); i++) {
            Object visit = visit(ctx.param_no_default(i));
            if (visit == null) {
                throw new RuntimeException("param_no_default not supported for:\n" + ctx.getText());
            }

            if (!(visit instanceof TypedName typedName)) {
                if (visit instanceof Self self) continue;
                if (visit instanceof String name) {
                    typedNames.add(new PyParameter(new TypedName(name, Type.getType(Object.class)), ctx.param_no_default(i).start.getLine(), null, null));
                    continue;
                }
                throw new RuntimeException("param_no_default not supported for:\n" + ctx.param_no_default(i).getText());
            }

            typedNames.add(new PyParameter(typedName, ctx.param_no_default(i).start.getLine(), typedName.type(), null));

            if (flags.get(F_CPL_FUNCTION)) {
                if (visit.equals("self")) {
                    if (!flags.get(F_CPL_INSTANCE_FUNC)) {
                        throw new CompilerException("self on a non-instance method:\n" + ctx.getText());
                    }
                } else {
                    // TODO
                }
            } else {
                throw new RuntimeException("param_no_default not supported for:\n" + ctx.getText());
            }
        }

        for (int i = 0; i < ctx.param_with_default().size(); i++) {
            PythonParser.Param_with_defaultContext paramWithDefaultContext = ctx.param_with_default(i);
            Object visitDef = visit(paramWithDefaultContext.default_assignment());
            Object visit = visit(paramWithDefaultContext.param());
            if (visit == null) {
                throw new RuntimeException("param_no_default not supported for:\n" + ctx.getText());
            }

            if (!(visit instanceof TypedName typedName)) {
                if (visit instanceof Self self) continue;
                if (visit instanceof String name) {
                    typedNames.add(new PyParameter(new TypedName(name, Type.getType(Object.class)), paramWithDefaultContext.start.getLine(), Type.getType(Object.class), (PyExpr) visitDef));
                    continue;
                }
                throw new RuntimeException("param_no_default not supported for:\n" + ctx.param_no_default(i).getText());
            }

            typedNames.add(new PyParameter(typedName, paramWithDefaultContext.start.getLine(), typedName.type(), (PyExpr) visitDef));

            if (flags.get(F_CPL_FUNCTION)) {
                if (visit.equals("self")) {
                    if (!flags.get(F_CPL_INSTANCE_FUNC)) {
                        throw new CompilerException("self on a non-instance method:\n" + ctx.getText());
                    }
                } else {
                    // TODO
                }
            } else {
                throw new RuntimeException("param_no_default not supported for:\n" + ctx.getText());
            }
        }
        return typedNames;
    }

    @Override
    public Object visitStar_etc(PythonParser.Star_etcContext ctx) {
        TerminalNode star = ctx.STAR();
        PythonParser.Param_no_defaultContext paramNoDefaultContext = ctx.param_no_default();
        List<PythonParser.Param_maybe_defaultContext> paramMaybeDefaultContexts = ctx.param_maybe_default();
        PythonParser.Param_no_default_star_annotationContext paramNoDefaultStarAnnotationContext = ctx.param_no_default_star_annotation();

        List<PyParameter> parameters = new ArrayList<>();
        if (star == null) {
            throw new AssertionError("Star (*) not found in parameters!");
        }

        if (paramNoDefaultContext != null) {
            if (paramNoDefaultContext.TYPE_COMMENT() != null) {
                throw new AssertionError("TYPE_COMMENT not supported for:\n" + ctx.getText());
            }
            Object visit = visit(paramNoDefaultContext.param());
            if (visit == null) {
                throw new AssertionError("param_no_default not supported for:\n" + ctx.getText());
            }
            switch (visit) {
                case TypedName typedName ->
                        parameters.add(new PyParameter(typedName, paramNoDefaultContext.start.getLine(), typedName.type(), null));
                case String name ->
                        parameters.add(new PyParameter(new TypedName(name, Type.getType(Object.class)), paramNoDefaultContext.start.getLine(), Type.getType(Object.class), null));
                default -> throw new AssertionError("param_no_default not supported for:\n" + ctx.getText());
            }
        }

        if (paramMaybeDefaultContexts != null) {
            for (PythonParser.Param_maybe_defaultContext paramMaybeDefaultContext : paramMaybeDefaultContexts) {
                if (paramMaybeDefaultContext.TYPE_COMMENT() != null) {
                    throw new AssertionError("TYPE_COMMENT not supported for:\n" + ctx.getText());
                }
                Object visit = visit(paramMaybeDefaultContext.param());
                PythonParser.Default_assignmentContext tree = paramMaybeDefaultContext.default_assignment();
                if (visit == null) {
                    throw new AssertionError("param_maybe_default not supported for:\n" + ctx.getText());
                }
                switch (visit) {
                    case TypedName typedName ->
                            parameters.add(new PyParameter(typedName, paramMaybeDefaultContext.start.getLine(), typedName.type(), tree == null ? null : (PyExpr) visit(tree), true));
                    case String name ->
                            parameters.add(new PyParameter(new TypedName(name, Type.getType(Object.class)), paramMaybeDefaultContext.start.getLine(), null, tree == null ? null : (PyExpr) visit(tree), true));
                    default -> throw new AssertionError("param_maybe_default not supported for:\n" + ctx.getText());
                }
            }

            return parameters;
        }

        if (paramNoDefaultStarAnnotationContext != null) {
            throw new AssertionError("param_no_default_star_annotation not supported for:\n" + ctx.getText());
        }

        throw new AssertionError("No supported matching star_etc found for:\n" + ctx.getText());
    }

    @Override
    public Object visitParam_no_default(PythonParser.Param_no_defaultContext ctx) {
        PythonParser.ParamContext param = ctx.param();
        TerminalNode terminalNode = ctx.TYPE_COMMENT();
        if (visit(param).equals("self")) {
            if (!flags.get(F_CPL_INSTANCE_FUNC)) {
                throw new CompilerException("self on a non-instance method:\n" + ctx.getText());
            } else {
                return new Self(curPyClass.type(this), getLocation(param));
            }
        }
        if (terminalNode != null) {
            Object visit = visit(param);
            if (visit == null) {
                throw new RuntimeException("param not supported for:\n" + ctx.getText());
            }
            if (!(visit instanceof TypedName typedName)) {
                throw new RuntimeException("Not a typed typedName:\n" + ctx.getText());
            }
            return visit;
        }

        return visit(param);
    }

    @Override
    public Object visitParam(PythonParser.ParamContext ctx) {
        PythonParser.AnnotationContext annotation = ctx.annotation();
        if (annotation != null) {
            Object visit = visit(annotation);
            if (visit == null) {
                throw new RuntimeException("annotation not supported for:\n" + ctx.getText());
            }
            return new TypedName(ctx.NAME().getText(), switch (visit) {
                case Type type -> type;
                case String name -> Type.getType(name);
                case PyExpr expr -> expr.type(this);
                default -> throw new RuntimeException("annotation not supported for:\n" + ctx.getText());
            });
        }

        TerminalNode name = ctx.NAME();
        if (name == null) {
            throw new RuntimeException("No NAME found for:\n" + ctx.getText());
        }
        return name.getText();
    }

    @Override
    public Object visitClass_def(PythonParser.Class_defContext ctx) {
        MethodVisitor rootInitMv1 = rootInitMv;
        PythonParser.DecoratorsContext decorators = ctx.decorators();

        ClassVisitor cv1 = cv;
        if (decorators != null) {
            Object visit = visit(decorators);

            if (visit instanceof List<?> list) {
                for (int i = 0, listSize = list.size(); i < listSize; i++) {
                    Object o = list.get(i);
                    if (o instanceof FuncCall func) {
                        var arguments = func.arguments;
                        if (arguments != null) {
                            Symbol symbol = imports.get(func.name());
                            Type owner = func.owner(this);
                            if (owner == null) {
                                imports.get("implements");
                                for (PyExpr o1 : arguments) {
                                    if (o1 instanceof PyObjectRef(String name, Location location)) {
                                        implementing.add(imports.get(name).name());
                                    } else {
                                        throw new CompilerException("Invalid @implements(...) decorator: @" + decorators.named_expression(i).getText());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        PythonParser.Class_def_rawContext classDefRawContext = ctx.class_def_raw();
        if (classDefRawContext != null) {
            Object visit = visit(classDefRawContext);
            this.rootInitMv = rootInitMv1;
            cv = cv1;
            return visit;
        }
        this.rootInitMv = rootInitMv1;
        cv = cv1;
        throw new RuntimeException("No supported matching class_def found for:\n" + ctx.getText());
    }

    @Override
    public Object visitClass_def_raw(PythonParser.Class_def_rawContext ctx) {
        TerminalNode name = ctx.NAME();
        var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        ClassNode node = new ClassNode();
        this.cv = node;

        String superClass = "java/lang/Object";
        List<String> implementing = new ArrayList<>();
        List<String> dynamicSuperClass = new ArrayList<>();
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
                        if (o instanceof PyObjectRef(String name1, Location location)) {
                            o = name1;
                        }
                        if (o instanceof String classname) {
                            Symbol symbol = symbols.get(classname);
                            JvmClass type = switch (symbol) {
                                case JvmClass jvmClass -> jvmClass;
                                case PyImport pyImport -> switch (pyImport.symbol) {
                                    case JvmClass jvmClass -> jvmClass;
                                    default ->
                                            throw new AssertionError("No supported imported compile class owner: " + pyImport.symbol.getClass().getName());
                                };
                                default ->
                                        throw new AssertionError("No supported compile class owner: " + symbol.getClass().getName());
                            };
                            Type classType = type.type(this);
                            String internalName = classType.getInternalName();
                            if (type.isInterface()) {
                                implementing.add(internalName);
                            } else if (superClass.equals("java/lang/Object") && !classType.equals(Type.getType(Object.class))) {
                                superClass = internalName;
                            } else {
                                superClass = "java/lang/Object";
                                JvmClass require = classCache.require(this, classType);
                                if (require instanceof JClass) {
                                    if (!superClass.equals("java/lang/Object")) {
                                        throw new RuntimeException("Can't inherit multiple Java classes for:\n" + ctx.getText());
                                    }

                                    superClass = require.type(this).getInternalName();
                                } else {
                                    dynamicSuperClass.add(internalName);
                                }
                            }
                        }
                    }
                }
            } finally {
                flags.clear(F_CPL_CLASS_INHERITANCE);
            }
        }

        implementing.add("org/python/_internal/PyObject");

        cv.visit(V1_8, ACC_PUBLIC, path + name.getText(), null, superClass, implementing.isEmpty() ? null : implementing.toArray(new String[0]));
        String classname = (path + name.getText()).replace("/", ".");
        PyClass value = new PyClass(this.pathOfFile, name.getText(), getLocation(ctx));
        this.definingClass = value;
        PyCompileClass oldCompilingClass = compilingClass;
        this.compilingClass = value;
        imports.add(name.getText(), value);
        undefinedClasses.remove(classname);

        classes.add(value);
        classCache.add(this, value);

        curPyClass = value;

        FieldVisitor self = cv.visitField(ACC_PUBLIC | ACC_FINAL, "__dict__", "Ljava/util/Map;", /*<String, Object>*/ "Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;", null);
//        AnnotationVisitor annotationVisitor = self.visitAnnotation("org/pythonvm/annotations/Generated", true);
//        annotationVisitor.visit("reason", "Generated for Python interop");
//        annotationVisitor.visitEnd();
        self.visitEnd();

        // Generate method: public Map<String, Object> --dict--() { return storage; }
        mv = cv.visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, "\uffffdict\uffff", "()Ljava/util/Map;", "()Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;", null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, path + name.getText(), "__dict__", "Ljava/util/Map;");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        // Generate method: public void --dict--(Map<String, Object> dict) { storage = dict; }
        mv = cv.visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, "\uffffdict\uffff", "(Ljava/util/Map;)V", "(Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;)V", null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitFieldInsn(PUTFIELD, path + name.getText(), "__dict__", "Ljava/util/Map;");
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        // Create default constructor
        MethodNode mv = new MethodNode(ACC_PUBLIC, "__init__", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitTypeInsn(NEW, "java/util/HashMap");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false);
        mv.visitFieldInsn(PUTFIELD, path + name.getText(), "__dict__", "Ljava/util/Map;");

        this.rootInitMv = mv;

        Object visit = visit(ctx.block());
        if (visit == null) {
            throw new RuntimeException("block not supported for:\n" + ctx.getText());
        }

        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        mv.accept(cv);

        compilingClass = oldCompilingClass;

        cv.visitEnd();

        node.accept(cw);

        definingClass = null;
        curPyClass = null;

        byte[] bytes = cw.toByteArray();

        try {
            String s = "build/tmp/compilePython/" + path.substring(0, path.length() - 1);
            if (!new File(s).exists()) {
                boolean mkdirs = new File(s).mkdirs();
                if (!mkdirs) {
                    throw new RuntimeException("Failed to create directory: " + s);
                }
            }
            FileOutputStream fileOutputStream = new FileOutputStream("build/tmp/compilePython/" + path + name.getText() + ".class");
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
                writer.jump(whileConditionContext.endLabel);
                return Unit.Instance;
//            } else if (writer.getContext() instanceof ForConditionContext) {
//                ForConditionContext forConditionContext = (ForConditionContext) writer.getContext();
//                mv.visitJumpInsn(GOTO, forConditionContext.loopEnd);
//                return Unit.Instance;
            } else {
                throw new RuntimeException("Not in a loop:\n" + ctx.getText());
            }
        }

        PythonParser.Return_stmtContext returnStmtContext = ctx.return_stmt();
        if (returnStmtContext != null) {
            return visit(returnStmtContext);
        }

        throw new RuntimeException("No supported matching simple_stmt found of owner " + ctx.getClass().getSimpleName() + " for:\n" + ctx.getText());
    }

    @Override
    public Object visitReturn_stmt(PythonParser.Return_stmtContext ctx) {
        PythonParser.Star_expressionsContext starExpressionsContext = ctx.star_expressions();
        if (starExpressionsContext != null) {
            Object visit = visit(starExpressionsContext);
            PyExpr pyExpr = loadExpr(ctx, visit);
            Type type = pyExpr.type(this);
            if (this.definingFunction.returnType == null) {
                this.definingFunction.returnType = type;
            } else {
                JvmClass jvmClass = this.definingFunction.returnClass(this);
                if (jvmClass.type(this).getSort() == Type.VOID && type != Type.VOID_TYPE) {
                    throw new RuntimeException("Return owner mismatch for:\n" + ctx.getText());
                } else if (!PythonCompiler.classCache.require(this, type).doesInherit(this, jvmClass)) {
                    throw new RuntimeException("Return owner mismatch '" + type + "' should inherit from '" + jvmClass.type(this) + "' for:\n" + ctx.getText());
                }
            }

            if (type != Type.VOID_TYPE) {
                writer.returnValue(definingFunction.returnType);
            }
            writer.returnVoid();
            return Unit.Instance;
        }
        throw new RuntimeException("No supported matching return_stmt found for:\n" + ctx.getText());
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
                    throw new RuntimeException("Expected 1 variable target, got " + starTargetsContexts.size() + " which is unsupported for now, code: " + ctx.getText());
                }

                PythonParser.Star_targetsContext starTargetsContext = starTargetsContexts.getFirst();
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
                    List<?> visit = (List<?>) visit(starTargetsContext);
                    Object first = visit.getFirst();

                    PythonParser.Star_expressionsContext starExpressionsContext = ctx.star_expressions();
                    if (starExpressionsContext == null) {
                        throw new RuntimeException("Value for variable assignment wasn't found.");
                    }

                    List<PythonParser.Star_expressionContext> starExpressionContexts = starExpressionsContext.star_expression();
                    if (starExpressionContexts.size() != 1) {
                        // TODO Add support for multiple star expressions
                        throw new RuntimeException("Expected 1 expression, got " + starExpressionContexts.size() + " which is unsupported for now.");
                    }

                    PythonParser.Star_expressionContext starExpressionContext = starExpressionContexts.getFirst();
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

                    if (first instanceof MemberField field) {
                        PyExpr expr = switch (visit1) {
                            case PyExpr pyExpr -> pyExpr;
                            case String string -> new PyConstant(string, getLocation(ctx));
                            case Byte byteValue -> new PyConstant(byteValue, getLocation(ctx));
                            case Short shortValue -> new PyConstant(shortValue, getLocation(ctx));
                            case Integer integer -> new PyConstant(integer, getLocation(ctx));
                            case Boolean booleanValue -> new PyConstant(booleanValue, getLocation(ctx));
                            case Long longValue -> new PyConstant(longValue, getLocation(ctx));
                            case Float floatValue -> new PyConstant(floatValue, getLocation(ctx));
                            case Double doubleValue -> new PyConstant(doubleValue, getLocation(ctx));
                            case Character charValue -> new PyConstant(charValue, getLocation(ctx));
                            default -> throw new RuntimeException("Expression for variable assignment wasn't found.");
                        };

                        field.set(mv, this, expr);
                        return Unit.Instance;
                    } else {
                        throw new RuntimeException("Variable target is not found :\n" + ctx.getText());
                    }
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

                PythonParser.Star_expressionContext starExpressionContext = starExpressionContexts.getFirst();
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

                    FieldVisitor fieldVisitor = cv.visitField(ACC_PUBLIC + ACC_STATIC, name, type.getDescriptor(), null, null);
                    fieldVisitor.visitEnd();

                    symbols.put(name, new ImportedField(name, type, getName(), getLocation(ctx)));

                    if (cv == rootCw) {
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
                            case String string -> new PyConstant(string, getLocation(ctx));
                            case Byte byteValue -> new PyConstant(byteValue, getLocation(ctx));
                            case Short shortValue -> new PyConstant(shortValue, getLocation(ctx));
                            case Integer integer -> new PyConstant(integer, getLocation(ctx));
                            case Boolean booleanValue -> new PyConstant(booleanValue, getLocation(ctx));
                            case Long longValue -> new PyConstant(longValue, getLocation(ctx));
                            case Float floatValue -> new PyConstant(floatValue, getLocation(ctx));
                            case Double doubleValue -> new PyConstant(doubleValue, getLocation(ctx));
                            case Character charValue -> new PyConstant(charValue, getLocation(ctx));
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
                            case String string -> new PyConstant(string, getLocation(ctx));
                            case Byte byteValue -> new PyConstant(byteValue, getLocation(ctx));
                            case Short shortValue -> new PyConstant(shortValue, getLocation(ctx));
                            case Integer integer -> new PyConstant(integer, getLocation(ctx));
                            case Boolean booleanValue -> new PyConstant(booleanValue, getLocation(ctx));
                            case Long longValue -> new PyConstant(longValue, getLocation(ctx));
                            case Float floatValue -> new PyConstant(floatValue, getLocation(ctx));
                            case Double doubleValue -> new PyConstant(doubleValue, getLocation(ctx));
                            case Character charValue -> new PyConstant(charValue, getLocation(ctx));
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
                        case String string -> new PyConstant(string, getLocation(ctx));
                        case Integer integer -> new PyConstant(integer, getLocation(ctx));
                        case Boolean booleanValue -> new PyConstant(booleanValue, getLocation(ctx));
                        case Long longValue -> new PyConstant(longValue, getLocation(ctx));
                        case Float floatValue -> new PyConstant(floatValue, getLocation(ctx));
                        case Double doubleValue -> new PyConstant(doubleValue, getLocation(ctx));
                        case Character charValue -> new PyConstant(charValue, getLocation(ctx));
                        case Byte byteValue -> new PyConstant(byteValue, getLocation(ctx));
                        case Short shortValue -> new PyConstant(shortValue, getLocation(ctx));
                        case PyExpr pyExpr -> pyExpr;
                        default -> throw new RuntimeException("Expression for variable assignment wasn't found.");
                    }, false, getLocation(ctx));

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
                                throw new RuntimeException("Annotated RHS not supported with owner " + visit1.getClass().getSimpleName() + " for:\n" + value.getText());
                    };
                    FieldVisitor fieldVisitor = cv.visitField(ACC_PUBLIC + ACC_STATIC, name, type.getDescriptor(), null, null);
                    fieldVisitor.visitEnd();

                    symbols.put(name, new ImportedField(name, type, getName(), getLocation(ctx)));

                    if (cv == rootCw) {
                        var a = constant(ctx, visit);
                        a.load(rootInitMv, this, a.preload(rootInitMv, this, false), false);
                        rootInitMv.visitFieldInsn(PUTSTATIC, getName(), name, type.getDescriptor());
                    }
                } else {
                    createVariable(name, switch (visit1) {
                        case PyClassConstruction classConstruction -> classConstruction.type(this);
                        case Symbol symbol -> symbol.type(this);
                        default ->
                                throw new RuntimeException("Annotated RHS not supported with owner " + visit1.getClass().getSimpleName() + " for:\n" + value.getText());
                    }, constant(ctx, visit), false, getLocation(ctx));
                }

                return Unit.Instance;
            } else {
                if (value != null) {
                    Type type = symbols.get(name).type(this);
                    Object visit = visit(value);
                    symbols.get(name).set(mv, this, switch (visit) {
                        case PyExpr pyExpr -> pyExpr;
                        default ->
                                throw new RuntimeException("Annotated RHS not supported with owner " + visit.getClass().getSimpleName() + " for:\n" + value.getText());
                    });
                }
                throw new CompilerException("Type annotation is required " + getLocation(ctx));
            }
        } catch (CompilerException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            flags.clear(F_CPL_ASSIGN);
        }
    }

    @Override
    public Object visitStar_targets(PythonParser.Star_targetsContext ctx) {
        if (ctx.star_target().size() != 1) {
            throw new AssertionError("DEBUG");
        }
        return List.of(visit(ctx.star_target(0)));
    }

    @Override
    public Object visitStar_target(PythonParser.Star_targetContext ctx) {
        PythonParser.Star_targetContext starTargetContext = ctx.star_target();
        if (starTargetContext != null) {
            throw new AssertionError("DEBUG");
        }

        TerminalNode star = ctx.STAR();
        if (star != null) {
            throw new AssertionError("DEBUG");
        }

        return visit(ctx.target_with_star_atom());
    }

    @Override
    public Object visitTarget_with_star_atom(PythonParser.Target_with_star_atomContext ctx) {
        if (ctx.LSQB() != null)
            throw new AssertionError("DEBUG");
        if (ctx.RSQB() != null)
            throw new AssertionError("DEBUG");
        if (ctx.star_atom() != null)
            throw new AssertionError("DEBUG");

        PythonParser.T_primaryContext tPrimaryContext = ctx.t_primary();
        TerminalNode name = ctx.NAME();
        TerminalNode dot = ctx.DOT();
        if (tPrimaryContext == null) throw new AssertionError("DEBUG");
        if (dot == null) throw new AssertionError("DEBUG");
        if (name == null) throw new AssertionError("DEBUG");

        Object visit = visit(tPrimaryContext);
        if (visit instanceof JvmClass jvmClass) {
            String name1 = name.getText();
            return jvmClass.field(this, name1);
        }
        if (visit instanceof Self self) {
            PyClass pyClass = self.typeClass(this);
            JvmField field = pyClass.field(this, name.getText());
            if (field == null) {
                int access;
                if (name.getText().startsWith("__")) {
                    access = ACC_PRIVATE;
                } else if (name.getText().startsWith("_")) {
                    access = ACC_PUBLIC;
                } else {
                    access = ACC_PUBLIC;
                }
                FieldVisitor fieldVisitor = cv.visitField(access, name.getText(), "Ljava/lang/Object;", null, null);
                PyField value = new PyField(pyClass.type(this), name.getText(), Type.getType(Object.class), getLocation(ctx));
                pyClass.fields.put(name.getText(), value);
                return new MemberField(self, name.getText(), getLocation(ctx));
            }
//            self.load(mv, this, self.preload(mv, this, false), false);
            return new MemberField(self, name.getText(), getLocation(ctx));
        }

        if (visit instanceof PyObjectRef(String varName, Location location)) {
            PyVariable variable = definingFunction.getVariable(varName);
            if (variable == null) throw new CompilerException("Variable '" + varName + "' not found", getLocation(ctx));
            return new MemberField(variable, name.getText(), getLocation(ctx));
        }

        throw new AssertionError("DEBUG");
    }

    private String importType(Type type) {
        if (type == null) throw new RuntimeException("Can't import null owner");
        if (type.getSort() == Type.ARRAY) {
            importType(type.getElementType());
            return null;
        }
        if (type.getSort() != Type.OBJECT) return null;
        try {
            Class javaType = Class.forName(type.getClassName(), false, getClass().getClassLoader());
            String simpleName = getSimpleName(type);
            JClass value = new JClass(type.getClassName(), javaType);
            imports.add(simpleName, value);
            return simpleName;
        } catch (ClassNotFoundException e) {
            PyClass value = classes.byClassName(type.getClassName());
            if (value == null) throw new CompilerException("JVM Class not found: " + type.getClassName());
            String simpleName = getSimpleName(value.owner);
            imports.add(simpleName, value);
            return simpleName;
        }
    }

    private String getSimpleName(Type type) {
        String[] split = type.getInternalName().split("/");
        return split[split.length - 1];
    }

    public PyExpr loadConstant(ParserRuleContext ctx, Object visit1, MethodVisitor mv) {
        var a = constant(ctx, visit1);

        a.load(mv, this, a.preload(mv, this, false), false);
        return a;
    }

    private @NotNull PyExpr constant(ParserRuleContext ctx, Object visit1) {
        return switch (visit1) {
            case PyConstant pyConstant -> pyConstant;
            case Symbol symbol -> symbol;
            case PyExpr pyExpr -> pyExpr;
            case String string -> new PyConstant(string, getLocation(ctx));
            case Integer integer -> new PyConstant(integer, getLocation(ctx));
            case Boolean booleanValue -> new PyConstant(booleanValue, getLocation(ctx));
            case Long longValue -> new PyConstant(longValue, getLocation(ctx));
            case Float floatValue -> new PyConstant(floatValue, getLocation(ctx));
            case Double doubleValue -> new PyConstant(doubleValue, getLocation(ctx));
            case Character charValue -> new PyConstant(charValue, getLocation(ctx));
            case Byte byteValue -> new PyConstant(byteValue, getLocation(ctx));
            case Short shortValue -> new PyConstant(shortValue, getLocation(ctx));
            default -> throw new RuntimeException("Expression for variable assignment wasn't found.");
        };
    }

    @Override
    public Object visitStar_atom(PythonParser.Star_atomContext ctx) {
        throw new AssertionError(ctx);
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
        if (shouldNotCreateEval()) {
            if (addition != null) {
                throw new RuntimeException("Binary operator is not allowed in owner annotations (at " + fileName + ":" + ctx.getStart().getLine() + ":" + ctx.getStart().getCharPositionInLine() + ")");
            }

            if (value != null) {
                return value;
            }
        }
        PyEval.Operator operator = null;
        if (ctx.VBAR() != null) {
            operator = PyEval.Operator.OR;
        }
        return new PyEval(this, ctx, operator, finalValue, finalAddition, getLocation(ctx));
    }

    private boolean shouldNotCreateEval() {
        return flags.get(F_CPL_TYPE_ANNO) || flags.get(F_CPL_CLASS_INHERITANCE);
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
        if (shouldNotCreateEval()) {
            if (addition != null) {
                throw new RuntimeException("Binary operator is not allowed in owner annotations (at " + fileName + ":" + ctx.getStart().getLine() + ":" + ctx.getStart().getCharPositionInLine() + ")");
            }

            if (value != null) {
                return value;
            }
        }
        if (ctx.CIRCUMFLEX() != null) {
            operator = PyEval.Operator.XOR;
        }
        return new PyEval(this, ctx, operator, finalValue, finalAddition, getLocation(ctx));
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
            if (shouldNotCreateEval()) {
                if (!compareOpBitwiseOrPairContexts.isEmpty()) {
                    throw new RuntimeException("Comparison is not allowed in owner annotations (at " + fileName + ":" + ctx.getStart().getLine() + ":" + ctx.getStart().getCharPositionInLine() + ")");
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
                            public void expectReturnType(PythonCompiler compiler, JvmClass returnType, Location location) {
                                comparison.expectReturnType(compiler, returnType, location);
                            }

                            @Override
                            public Type type(PythonCompiler compiler) {
                                return Type.BOOLEAN_TYPE;
                            }

                            @Override
                            public Location location() {
                                return getLocation(ctx);
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
        if (shouldNotCreateEval()) {
            if (ctx.eq_bitwise_or() != null) {
                throw new RuntimeException("Equality is not allowed in owner annotations (at " + fileName + ":" + ctx.getStart().getLine() + ":" + ctx.getStart().getCharPositionInLine() + ")");
            }
            if (ctx.noteq_bitwise_or() != null) {
                throw new RuntimeException("Inequality is not allowed in owner annotations (at " + fileName + ":" + ctx.getStart().getLine() + ":" + ctx.getStart().getCharPositionInLine() + ")");
            }
            if (ctx.gt_bitwise_or() != null) {
                throw new RuntimeException("Relational operator is not allowed in owner annotations (at " + fileName + ":" + ctx.getStart().getLine() + ":" + ctx.getStart().getCharPositionInLine() + ")");
            }
            if (ctx.gte_bitwise_or() != null) {
                throw new RuntimeException("Relational operator is not allowed in owner annotations (at " + fileName + ":" + ctx.getStart().getLine() + ":" + ctx.getStart().getCharPositionInLine() + ")");
            }
            if (ctx.lt_bitwise_or() != null) {
                throw new RuntimeException("Relational operator is not allowed in owner annotations (at " + fileName + ":" + ctx.getStart().getLine() + ":" + ctx.getStart().getCharPositionInLine() + ")");
            }
            if (ctx.lte_bitwise_or() != null) {
                throw new RuntimeException("Relational operator is not allowed in owner annotations (at " + fileName + ":" + ctx.getStart().getLine() + ":" + ctx.getStart().getCharPositionInLine() + ")");
            }

            throw new RuntimeException("No supported matching compare_op_bitwise_or_pair found for:\n" + ctx.getText());
        }
        if (eqBitwiseOrContext != null) {
            return new PyComparison(eqBitwiseOrContext, PyComparison.Comparison.EQ, ctx, getLocation(eqBitwiseOrContext));
        }
        PythonParser.Noteq_bitwise_orContext noteqBitwiseOrContext = ctx.noteq_bitwise_or();
        if (noteqBitwiseOrContext != null) {
            return new PyComparison(noteqBitwiseOrContext, PyComparison.Comparison.NE, ctx, getLocation(noteqBitwiseOrContext));
        }
        PythonParser.Gt_bitwise_orContext gtBitwiseOrContext = ctx.gt_bitwise_or();
        if (gtBitwiseOrContext != null) {
            return new PyComparison(gtBitwiseOrContext, PyComparison.Comparison.GT, ctx, getLocation(gtBitwiseOrContext));
        }
        PythonParser.Gte_bitwise_orContext gteBitwiseOrContext = ctx.gte_bitwise_or();
        if (gteBitwiseOrContext != null) {
            return new PyComparison(gteBitwiseOrContext, PyComparison.Comparison.GTE, ctx, getLocation(gteBitwiseOrContext));
        }
        PythonParser.Lt_bitwise_orContext ltBitwiseOrContext = ctx.lt_bitwise_or();
        if (ltBitwiseOrContext != null) {
            return new PyComparison(ltBitwiseOrContext, PyComparison.Comparison.LT, ctx, getLocation(ltBitwiseOrContext));
        }
        PythonParser.Lte_bitwise_orContext lteBitwiseOrContext = ctx.lte_bitwise_or();
        if (lteBitwiseOrContext != null) {
            return new PyComparison(lteBitwiseOrContext, PyComparison.Comparison.LTE, ctx, getLocation(lteBitwiseOrContext));
        }
        PythonParser.Notin_bitwise_orContext notinBitwiseOrContext = ctx.notin_bitwise_or();
        if (notinBitwiseOrContext != null) {
            return new PyComparison(notinBitwiseOrContext, PyComparison.Comparison.NOT_IN, ctx, getLocation(notinBitwiseOrContext));
        }
        PythonParser.In_bitwise_orContext inBitwiseOrContext = ctx.in_bitwise_or();
        if (inBitwiseOrContext != null) {
            return new PyComparison(inBitwiseOrContext, PyComparison.Comparison.IN, ctx, getLocation(inBitwiseOrContext));
        }
        PythonParser.Is_bitwise_orContext bitwiseOr = ctx.is_bitwise_or();
        if (bitwiseOr != null) {
            return new PyComparison(bitwiseOr, PyComparison.Comparison.IS, ctx, getLocation(bitwiseOr));
        }
        PythonParser.Isnot_bitwise_orContext isnotBitwiseOrContext = ctx.isnot_bitwise_or();
        if (isnotBitwiseOrContext != null) {
            return new PyComparison(isnotBitwiseOrContext, PyComparison.Comparison.IS_NOT, ctx, getLocation(isnotBitwiseOrContext));
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
        if (shouldNotCreateEval()) {
            if (addition != null) {
                throw new RuntimeException("Type annotation is not allowed in owner annotations (at " + fileName + ":" + ctx.getStart().getLine() + ":" + ctx.getStart().getCharPositionInLine() + ")");
            }

            if (value != null) {
                return value;
            }
        }
        if (ctx.AMPER() != null) {
            operator = PyEval.Operator.AND;
        }
        return new PyEval(this, ctx, operator, finalValue, finalAddition, getLocation(ctx));
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
        if (shouldNotCreateEval()) {
            if (addition != null) {
                throw new RuntimeException("Type annotation is not allowed in owner annotations (at " + fileName + ":" + ctx.getStart().getLine() + ":" + ctx.getStart().getCharPositionInLine() + ")");
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
        return new PyEval(this, ctx, operator, finalValue, finalAddition, getLocation(ctx));
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
        if (shouldNotCreateEval()) {
            if (addition != null) {
                throw new RuntimeException("Type annotation is not allowed in owner annotations (at " + fileName + ":" + ctx.getStart().getLine() + ":" + ctx.getStart().getCharPositionInLine() + ")");
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
            return new PyEval(this, ctx, operator, finalValue, finalAddition, getLocation(ctx));
        }
        return value;
    }

    public PyExpr loadExpr(ParserRuleContext ctx, Object visit) {
        MethodVisitor mv = this.mv == null ? this.rootInitMv : this.mv;
        return switch (visit) {
            case PyExpr pyExpr -> {
                pyExpr.load(mv, this, pyExpr.preload(mv, this, false), false);
                yield pyExpr;
            }
            case Integer integer -> loadConstant(ctx, integer, mv);
            case Long aLong -> loadConstant(ctx, aLong, mv);
            case Float aFloat -> loadConstant(ctx, aFloat, mv);
            case Double aDouble -> loadConstant(ctx, aDouble, mv);
            case String s -> loadConstant(ctx, s, mv);
            case Boolean aBoolean -> loadConstant(ctx, aBoolean, mv);
            case Character aChar -> loadConstant(ctx, aChar, mv);
            case Unit unit -> throw new RuntimeException("unit not supported for:\n" + ctx.getText());
            default -> throw new RuntimeException("No supported matching loadExpr found for:\n" + ctx.getText());
        };
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
        if (shouldNotCreateEval()) {
            if (addition != null) {
                throw new RuntimeException("Type annotation is not allowed in owner annotations (at " + fileName + ":" + ctx.getStart().getLine() + ":" + ctx.getStart().getCharPositionInLine() + ")");
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
        return new PyEval(this, ctx, operator, finalValue, finalAddition, getLocation(ctx));
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
            if (shouldNotCreateEval()) {
                throw new RuntimeException("Unary operator is not allowed in owner annotations (at " + fileName + ":" + ctx.getStart().getLine() + ":" + ctx.getStart().getCharPositionInLine() + ")");
            }
            return new PyEval(this, ctx, operator, finalValue, null, getLocation(ctx));
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
            if (shouldNotCreateEval()) {
                throw new RuntimeException("Unary operator is not allowed in owner annotations (at " + fileName + ":" + ctx.getStart().getLine() + ":" + ctx.getStart().getCharPositionInLine() + ")");
            }
            return new PyEval(this, ctx, operator, finalValue, null, getLocation(ctx));
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
            if (shouldNotCreateEval()) {
                throw new RuntimeException("Unary operator is not allowed in owner annotations (at " + fileName + ":" + ctx.getStart().getLine() + ":" + ctx.getStart().getCharPositionInLine() + ")");
            }
            return new PyEval(this, ctx, operator, finalValue, null, getLocation(ctx));
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
            if (shouldNotCreateEval()) {
                if (addition != null) {
                    throw new RuntimeException("Type annotation is not allowed in owner annotations (at " + fileName + ":" + ctx.getStart().getLine() + ":" + ctx.getStart().getCharPositionInLine() + ")");
                }

                if (value != null) {
                    return value;
                }
            }
            return new PyEval(this, ctx, operator, finalValue, finalAddition, getLocation(ctx));

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
            Object visit = visit(atom);
            if (this.definingInstance != null && visit instanceof PyObjectRef(String name, Location location)) {
                if (name.equals("self")) {
                    return new Self(definingInstance.owner, getLocation(ctx));
                }
            }
            return visit;
        }
        throw new RuntimeException("No supported matching t_primary found for:\n" + ctx.getText());
    }

    @Override
    public Object visitPrimary(PythonParser.PrimaryContext ctx) {
        PythonParser.PrimaryContext primaryContext = ctx.primary();
        PythonParser.ArgumentsContext argumentsContext = ctx.arguments();
        if (ctx.LPAR() != null) {
            pushMemberContext(MemberContext.FUNCTION);
            if (primaryContext == null) {
                throw new RuntimeException("primary not supported for:\n" + ctx.getText());
            }
            callArgs = argumentsContext;
            PyExpr visit = (PyExpr) visit(primaryContext);
            List<PyExpr> arguments = argumentsContext == null ? Collections.emptyList() : (List<PyExpr>) visit(argumentsContext);
            if (visit instanceof JvmClass) {
                JvmClass jvmClass = (JvmClass) visit;
                return new PyClassConstruction(arguments, jvmClass, getLocation(ctx));
            } else {
                return visit;
            }
        } else {
            PythonParser.ArgumentsContext arguments = argumentsContext;
            if (arguments != null) {
                throw new RuntimeException("primary not supported for:\n" + ctx.getText());
            }
        }
        if (ctx.RPAR() != null) {
            throw new RuntimeException("primary not supported for:\n" + ctx.getText());
        }
        if (primaryContext != null) {
            if (primaryContext.LPAR() == null) {
                this.pushMemberContext(MemberContext.FIELD);
            }
            Object parent = visit(primaryContext);
            if (primaryContext.LPAR() == null) {
                this.popMemberContext();
            }
            if (ctx.DOT() != null && ctx.NAME() != null) {
                String name = ctx.NAME().getText();
                switch (parent) {
                    case PyObjectRef(String name1, Location location) -> {
                        if (name1.equals("self") && definingInstance != null) {
                            return new Self(definingInstance.owner, getLocation(ctx));
                        }
                        Symbol symbol = symbols.get(name1);
                        if (symbol == null) {
                            throw new CompilerException("Unknown symbol: " + name1 + " at " + getLocation(ctx));
                        }
                        switch (symbol) {
                            case PyImport pyImport -> {
                                if (pyImport.symbol == null) {
                                    throw new CompilerException("Unknown symbol: " + name + " at " + getLocation(ctx));
                                }

                                return switch (pyImport.symbol) {
                                    case JvmClass cls -> {
                                        if (memberContext.equals(MemberContext.FUNCTION)) {
                                            List<PyExpr> visit = (List<PyExpr>) visit(callArgs);
                                            yield new MemberFuncCall((PyExpr) parent, name, visit.toArray(PyExpr[]::new), getLocation(ctx));
                                        } else if (flags.get(F_READ_CALL)) {
                                            PythonParser.ArgumentsContext arguments = this.callArgs;
                                            List<PyExpr> visit = (List<PyExpr>) visit(arguments);
                                            yield new MemberFuncCall((PyExpr) parent, name, visit.toArray(PyExpr[]::new), getLocation(ctx));
                                        }
                                        yield new MemberField((PyExpr) parent, name, getLocation(ctx));
                                    }
                                    case JvmFunction func -> {
                                        if (ctx.LPAR() != null) {
                                            if (argumentsContext != null) {
                                                PythonParser.PrimaryContext primary = ctx.primary();
                                                if (primary == null) {
                                                    throw new RuntimeException("primary not supported for:\n" + ctx.getText());
                                                }
                                                yield new FuncCall((PyExpr) parent, (List<PyExpr>) visit(argumentsContext), name, getLocation(ctx));
                                            } else {
                                                PythonParser.PrimaryContext primary = ctx.primary();
                                                if (primary == null) {
                                                    throw new RuntimeException("primary not supported for:\n" + ctx.getText());
                                                }
                                                yield new FuncCall((PyExpr) parent, List.of(), name, getLocation(ctx));
                                            }
                                        }

                                        throw new AssertionError("DEBUG");
                                    }
                                    default ->
                                            throw new RuntimeException("Primary Python Object Reference not supported for:\n" + ctx.getText());
                                };
                            }
                            case PyVariable pyVariable -> {
                                if (ctx.LPAR() != null) {
                                    if (argumentsContext != null) {
                                        PythonParser.PrimaryContext primary = ctx.primary();
                                        if (primary == null) {
                                            throw new RuntimeException("primary not supported for:\n" + ctx.getText());
                                        }
                                        return new FuncCall((PyExpr) parent, (List<PyExpr>) visit(argumentsContext), name, getLocation(ctx));
                                    } else {
                                        PythonParser.PrimaryContext primary = ctx.primary();
                                        if (primary == null) {
                                            throw new RuntimeException("primary not supported for:\n" + ctx.getText());
                                        }
                                        return new FuncCall((PyExpr) parent, List.of(), name, getLocation(ctx));
                                    }
                                }

                                if (flags.get(F_READ_CALL)) {
                                    PythonParser.ArgumentsContext arguments = this.callArgs;
                                    if (arguments == null) {
                                        throw new RuntimeException("arguments not supported for:\n" + ctx.getText());
                                    }
                                    return new FuncCall((PyExpr) parent, (List<PyExpr>) visit(arguments), name, getLocation(ctx));
                                }
                                if (memberContext == MemberContext.FUNCTION) {
                                    List<PyExpr> arguments = (List<PyExpr>) visit(this.callArgs);
                                    if (argumentsContext == null) {
                                        arguments = List.of();
                                    } else {
                                        arguments = (List<PyExpr>) visit(argumentsContext);
                                        if (arguments == null || arguments.isEmpty()) {
                                            throw new RuntimeException("arguments not supported for:\n" + ctx.getText());
                                        }
                                    }

                                    Type[] types = new Type[arguments.size()];
                                    for (int i = 0; i < arguments.size(); i++) {
                                        types[i] = switch (arguments.get(i)) {
                                            case PyObjectRef(String name2, Location location1) ->
                                                    symbols.get(name2).type(this);
                                            default -> arguments.get(i).type(this);
                                        };
                                    }

                                    return new MemberFuncCall(pyVariable, name, arguments.toArray(PyExpr[]::new), getLocation(ctx.NAME()));
                                }
                            }
                            default ->
                                    throw new RuntimeException("Unsupported primary Python Object Reference:\n" + ctx.getText());
                        }
                    }
                    case JvmClass jvmClass -> {
                        List<PyExpr> arguments = callArgs == null ? Collections.emptyList() : (List<PyExpr>) visit(this.callArgs);
                        return new MemberFuncCall(jvmClass, name, arguments.toArray(PyExpr[]::new), getLocation(ctx));
                    }
                    case JvmConstructor jvmConstructor ->
                            throw new RuntimeException("Primary JVM Constructor not supported for:\n" + ctx.getText());
                    case JvmFunction jvmFunction ->
                            throw new RuntimeException("Primary JVM Function not supported for:\n" + ctx.getText());
                    case JvmField jvmField -> {
                        if (memberContext == MemberContext.FIELD) {
                            return new MemberField(jvmField.cls(this).field(this, name), name, getLocation(ctx));
                        } else if (memberContext == MemberContext.FUNCTION) {
                            List<PyExpr> arguments = (List<PyExpr>) visit(this.callArgs);
                            return new MemberFuncCall(jvmField, name, arguments.toArray(PyExpr[]::new), getLocation(ctx.NAME()));
                        }
                    }
                    case FuncCall funcCall -> {
                        if (memberContext == MemberContext.FUNCTION) {
                            List<PyExpr> arguments = (List<PyExpr>) visit(this.callArgs);
                            return new MemberFuncCall(funcCall, name, arguments.toArray(PyExpr[]::new), getLocation(ctx.NAME()));
                        }
                    }
                    case Self self -> {
                        if (ctx.LPAR() != null) {
                            PythonParser.PrimaryContext primary = ctx.primary();
                            if (primary == null) {
                                throw new RuntimeException("primary not supported for:\n" + ctx.getText());
                            }

                            List<PyExpr> arguments = argumentsContext == null ? Collections.emptyList() : (List<PyExpr>) visit(argumentsContext);
                            return new MemberFuncCall(self, name, arguments.toArray(PyExpr[]::new), getLocation(ctx));
                        } else {
                            return new MemberField(self, name, getLocation(ctx));
                        }
                    }
                    case PyClassConstruction classConstruction -> {
                        if (name != null) {
                            return new MemberField(classConstruction, name, getLocation(ctx));
                        }
                        return classConstruction;
                    }
                    case PyVariable var -> {
                        if (name != null) {
                            return new MemberField(var, name, getLocation(ctx));
                        }
                        return var;
                    }
                    case PyExpr expr -> {
                        if (name != null) {
                            return new MemberField(expr, name, getLocation(ctx));
                        }
                        return expr;
                    }
                    default ->
                            throw new RuntimeException("Primary not supported with owner '" + parent.getClass().getSimpleName() + "' for:\n" + ctx.getText());
                }
            }
            return parent;
        }

        PythonParser.AtomContext atom = ctx.atom();
        if (atom != null) {
            Object visited = visit(atom);
            PyExpr expr = switch (visited) {
                case String s -> new PyConstant(s, getLocation(ctx));
                case Integer s -> new PyConstant(s, getLocation(ctx));
                case Float s -> new PyConstant(s, getLocation(ctx));
                case Long s -> new PyConstant(s, getLocation(ctx));
                case Double s -> new PyConstant(s, getLocation(ctx));
                case Character s -> new PyConstant(s, getLocation(ctx));
                case Byte s -> new PyConstant(s, getLocation(ctx));
                case Short s -> new PyConstant(s, getLocation(ctx));
                case Boolean s -> new PyConstant(s, getLocation(ctx));
                case PyExpr pyExpr -> pyExpr;
                case None none -> new PyConstant(none, getLocation(ctx));
                default -> throw new RuntimeException("Expression for variable assignment didn't find a match.");
            };
            switch (expr) {
                case PyObjectRef(String name, Location location) -> {
                    Symbol symbol = symbols.get(name);
                    if (definingFunction != null && name.equals("self") && !definingFunction.isStatic()) {
                        return new Self(definingClass.type(this), location);
                    } else if (symbol == null) {
                        if (definingFunction != null) {
                            PyVariable variable = definingFunction.getVariable(name);
                            if (variable != null) {
                                return variable;
                            }
                        }
                        throw new CompilerException("No symbol named '" + name + "' found", location);
                    } else if (symbol instanceof PyImport pyImport) {
                        if (pyImport.symbol instanceof JvmFunction jvmFunction) {
                            List<PyExpr> arguments = callArgs == null ? List.of() : (List<PyExpr>) visit(this.callArgs);
                            return new PyFunctionCall(arguments, jvmFunction, location);
                        } else if (pyImport.symbol instanceof JvmClass jvmClass) {
                            return jvmClass;
                        } else {
                            throw new RuntimeException("Primary not supported for expression type '" + pyImport.symbol.getClass().getSimpleName() + "' for:\n" + ctx.getText());
                        }
                    } else if (symbol instanceof JvmFunction jvmFunction) {
                        List<PyExpr> arguments = callArgs == null ? List.of() : (List<PyExpr>) visit(this.callArgs);
                        return new PyFunctionCall(arguments, jvmFunction, location);
                    } else if (symbol instanceof JvmClass jvmClass) {
                        return jvmClass;
                    } else if (symbol instanceof PyVariable variable) {
                        return variable;
                    } else if (symbol instanceof ImportedField field) {
                        return field;
                    } else {
                        throw new RuntimeException("Primary not supported for expression type '" + symbol.getClass().getSimpleName() + "' for:\n" + ctx.getText());
                    }
                }
                case PyConstant constant -> {
                    return constant;
                }
                default ->
                        throw new RuntimeException("Primary not supported for expression type '" + expr.getClass().getSimpleName() + "' for:\n" + ctx.getText());
            }
        }
        throw new RuntimeException("No supported matching primary found at:\n" + ctx.getText());
    }

    private Location getLocation(TerminalNode name) {
        return getLocation(name.getSymbol());
    }

    public void pushMemberContext(MemberContext memberContext) {
        this.memberContextStack.push(memberContext);
        this.memberContext = memberContext;
    }

    public void popMemberContext() {
        this.memberContextStack.pop();
        if (this.memberContextStack.isEmpty()) {
            this.memberContext = null;
            return;
        }
        this.memberContext = this.memberContextStack.peek();
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
            return new PyObjectRef(name.getText(), getLocation(name.getSymbol()));
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
        TerminalNode none = ctx.NONE();
        if (none != null) {
            return None.None;
        }
        throw new RuntimeException("No supported matching atom found for:\n" + ctx.getText());
    }

    private Location getLocation(Token symbol) {
        return new Location(rootDir.toAbsolutePath().resolve(pathOfFile).resolve(fileName + ".py").toString(), symbol.getLine(), symbol.getCharPositionInLine(), symbol.getLine(), symbol.getCharPositionInLine() + symbol.getStopIndex() - symbol.getStartIndex());
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
                String className = ((String) visit(dottedNameContext)).replace(".", "/") + "/" + s.getValue();
                Type objectType = Type.getObjectType(className);
                if (!classCache.load(this, objectType)) {
                    expectations.expectClass(this, dottedNameContext.getText(), s.getValue());
                }
                JvmClass jClass = classCache.get(objectType);
                this.imports.add(s.getKey(), jClass);
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
            return new AbstractMap.SimpleEntry<>(text, text);
        }

        if (ctx.NAME().size() == 2) {
            return new AbstractMap.SimpleEntry<>(ctx.NAME(1).getText(), ctx.NAME(0).getText());
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
        rootCw.visit(V1_8, ACC_PUBLIC, getName(), null, "java/lang/Object", new String[]{"org/python/_internal/PyModule"});
        cv = rootCw;

        definingModule = new PyModule(pathOfFile.resolve(fileName + ".py"), getLocation(ctx));
        PyCompileClass oldCompilingClass = compilingClass;
        compilingClass = definingModule;
        classCache.add(this, definingModule);

        rootInitMv = rootCw.visitMethod(ACC_PUBLIC | ACC_STATIC, "<clinit>", "()V", null, null);
        rootInitMv.visitCode();

        // Set the file typedName of the class to fileName
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

        compilingClass = oldCompilingClass;
        definingModule = null;

        try {
            File file = new File("build/tmp/compilePython/" + getName() + ".class");
            File parentFile = file.getParentFile();
            if (!parentFile.exists()) {
                boolean mkdirs = parentFile.mkdirs();
                if (!mkdirs) {
                    throw new IOException("Failed to create directory " + parentFile);
                }
            }
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(rootCw.toByteArray());
            fos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            rootCw = null;
            cv = null;
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
        this.fileName = p.substring(p.lastIndexOf("/") + 1);
        this.path = p.substring(0, p.lastIndexOf("/") + 1);
        this.pathOfFile = Path.of(path);

        try {
            visit(fileInputContext);
        } catch (CompilerException e) {
            compileErrors.add(e);
        }
    }

    public void compile(String python, String fileName) {
        PythonLexer lexer = new PythonLexer(CharStreams.fromString(python));
        PythonParser parser = new PythonParser(new CommonTokenStream(lexer));
        PythonParser.File_inputContext fileInputContext = parser.file_input();
        var p = fileName.substring(0, fileName.length() - ".py".length());
        this.fileName = p.substring(p.lastIndexOf("/") + 1);
        this.path = p.substring(0, p.lastIndexOf("/") + 1);
        this.pathOfFile = Path.of(path);

        try {
            visit(fileInputContext);
        } catch (CompilerException e) {
            e.printStackTrace();
            compileErrors.add(e);
        }
    }

    public void pack(String outputDir, String outputJar) {
        // Pack "build/rustc/**/*.class"
        try (JarOutputStream jarOut = new JarOutputStream(new FileOutputStream(outputJar))) {
            Path sourcePath = Paths.get(outputDir);
            Files.walk(sourcePath)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        String entryName = sourcePath.relativize(path).toString().replace("\\", "/");
                        try {
                            jarOut.putNextEntry(new ZipEntry(entryName));
                            Files.copy(path, jarOut);
                            jarOut.closeEntry();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        } catch (IOException e) {
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

    int createVariable(String name, String type, PyExpr expr, boolean boxed, Location location) {
        mv.visitLineNumber(expr.location().lineStart(), new Label());

        Symbol symbol = symbols.get(type);
        if (symbol == null) {
            throw new CompilerException("Symbol '" + type + "' not found ", getLocation(expr));
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
        symbols.put(name, new PyVariable(name, currentVariableIndex, label, location));
//        writer.label(label);
        mv.visitLocalVariable(name, type1.getDescriptor(), null, endLabel, endLabel, currentVariableIndex);
        mv.visitLineNumber(expr.location().lineStart(), label);

        writer.box(writer.unboxType(type1));
        writer.storeObject(currentVariableIndex, Type.getType(Object.class));

        return currentVariableIndex++;
    }

    PyVariable createVariable(String name, Type type, PyExpr expr, boolean boxed, Location location) {
        if (definingFunction == null) {
            throw new RuntimeException("Defining function is null");
        }

        return definingFunction.createVariable(this, name, expr, boxed, location);
    }

    private PyVariable createVariable(String name, Type type, boolean b, Location location) {
        if (definingFunction == null) {
            throw new RuntimeException("Defining function is null");
        }
        return definingFunction.createVariable(this, name, b, location);
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
        return new CompilerException("Type '" + type + "' not found ", getLocation(expr));
    }

    CompilerException jvmClassNotFound(String type, PyExpr expr) {
        return new CompilerException("JVM Class '" + type + "' not found ", getLocation(expr));
    }

    CompilerException functionNotFound(Type owner, String name, PyExpr expr) {
        return new CompilerException("Function '" + name + "' not found in '" + owner.getClassName() + "' ", getLocation(expr));
    }

    CompilerException functionNotFound(Type owner, String name, Type[] args, PyExpr expr) {
        return new CompilerException("Function '" + name + "' not found that matches (" + Arrays.stream(args).map(Type::getClassName).collect(Collectors.joining(", ")) + ") in '" + owner.getClassName() + "' ", getLocation(expr));
    }

    Location getLocation(PyExpr expr) {
        return expr.location();
    }

    Location getLocation(ParserRuleContext ctx) {
        return new Location(rootDir.toAbsolutePath().resolve(pathOfFile).resolve(fileName + ".py").toString(), ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine(), ctx.getStop().getLine(), ctx.getStop().getCharPositionInLine());
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


            switch (visit) {
                case String s -> args.add(new PyConstant(s, getLocation(ctx)));
                case Integer s -> args.add(new PyConstant(s, getLocation(ctx)));
                case Float s -> args.add(new PyConstant(s, getLocation(ctx)));
                case Long s -> args.add(new PyConstant(s, getLocation(ctx)));
                case Double s -> args.add(new PyConstant(s, getLocation(ctx)));
                case Character s -> args.add(new PyConstant(s, getLocation(ctx)));
                case Byte s -> args.add(new PyConstant(s, getLocation(ctx)));
                case Short s -> args.add(new PyConstant(s, getLocation(ctx)));
                case Boolean s -> args.add(new PyConstant(s, getLocation(ctx)));
                case PyExpr expr -> args.add(expr);
                default -> throw new UnsupportedOperationException("Not implemented: " + visit.getClass().getName());
            }
        }

        return args;
    }
}
