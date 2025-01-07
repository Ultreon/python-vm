package dev.ultreon.pythonc;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

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
    public static final int F_DYN_CALL = 50;

    private final ClassWriter rootCw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    private ClassWriter cw = rootCw;
    private String path = "";
    private String fileName = "Main";
    private final Map<String, Symbol> imports = new HashMap<>();
    private State state = State.File;
    private final BitSet flags = new BitSet();
    private final Decorators decorators = new Decorators();
    private MethodVisitor mv;
    private MethodVisitor rootInitMv;
    private int currentVariableIndex = 1;
    private final Set<String> implementing = new HashSet<>();
    private final Map<String, Symbol> symbols = new HashMap<>();
    private Label endLabel;
    private Label startLabel;
    private Label curLabel;

    public void compileSources(String sourceDir) {
        // Walk the directory
        Path path = Path.of(System.getProperty("user.dir")).relativize(Path.of(sourceDir));
        try {
            Files.walk(path)
                    .filter(p -> p.toString().endsWith(".py"))
                    .map(Path::toString)
                    .forEach(v -> {
                        try {
                            compile(new File(v), new File(sourceDir));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
            Files.walk(path)
                    .filter(p -> !p.toString().endsWith(".py"))
                    .map(v -> Path.of(sourceDir).relativize(v.toAbsolutePath()).toString())
                    .forEach(v -> {
                        try {
                            // Copy over resources
                            Path path1 = Path.of("build/pythonc", v);
                            System.out.println("path1 = " + path1);
                            if (Files.notExists(path1)) Files.createDirectories(path1.getParent());
                            Path path2 = Path.of(sourceDir, v);
                            if (Files.isDirectory(path2)) return;
                            Files.copy(Path.of(sourceDir, v), path1.toAbsolutePath(), StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

        symbols.put("asc", new PyBuiltinFunction("java/lang/String", "pythonvm/builtins/_Builtins", new String[]{"asc(Ljava/lang/String;)V"}, 1, "asc"));
        symbols.put("print", new PyBuiltinFunction("java/lang/System", "pythonvm/builtins/_Builtins", new String[]{"print([Ljava/lang/Object;Ljava/util/Map;)"}, 2, "print", PyBuiltinFunction.Mode.DYN_CTOR));
    }

    @Override
    public Object visit(@NonNull ParseTree tree) {
        if (tree == null) {
            throw new RuntimeException("Tree is null");
        }
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
                case Decorators decorators1 ->
                        throw new RuntimeException("statement not supported for:\n" + ctx.getText());
                case FuncCall funcCall ->
                        throw new RuntimeException("statement not supported for:\n" + ctx.getText());
                case List<?> list -> {
                    for (Object o : list) {
                        if (o instanceof FuncCall funcCall) {
                            if (mv == null) {
                                throw new RuntimeException("Not writing a method:\n" + ctx.getText());
                            }

                            funcCall.write(mv, this);
                        } else if (o instanceof List<?> list1) {
                            for (Object o1 : list1) {
                                if (o1 instanceof FuncCall funcCall) {
                                    if (mv == null) {
                                        throw new RuntimeException("Not writing a method:\n" + ctx.getText());
                                    }

                                    funcCall.write(mv, this);
                                }
                            }
                        } else if (o instanceof PyObjectRef pyObjectRef) {
                            if (flags.get(F_CPL_TYPE_ANNO)) {
                                return imports.get(pyObjectRef.name).name();
                            } else {
                                throw new RuntimeException("statement not supported for:\n" + ctx.getText());
                            }
                        } else if (o instanceof String s) {
                            throw new RuntimeException("statement not supported for string:\n" + ctx.getText());
                        } else if (o instanceof Boolean b) {
                            throw new RuntimeException("statement not supported for boolean:\n" + ctx.getText());
                        } else if (o instanceof Integer j) {
                            throw new RuntimeException("statement not supported for integer:\n" + ctx.getText());
                        } else if (o instanceof Float f) {
                            throw new RuntimeException("statement not supported for float:\n" + ctx.getText());
                        } else if (o instanceof Long l) {
                            throw new RuntimeException("statement not supported for long:\n" + ctx.getText());
                        } else if (o instanceof Double d) {
                            throw new RuntimeException("statement not supported for double:\n" + ctx.getText());
                        } else if (o instanceof Unit unit) {
                            continue;
                        } else {
                            throw new RuntimeException("statement not supported for: " + o.getClass().getSimpleName());
                        }
                    }
                }
                default -> throw new RuntimeException("statement not supported for:\n" + ctx.getText());
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
            String s = imports.get(typedName.type).name();
            if (s == null) {
                throw new RuntimeException("No import found for:\n" + ctx.getText());
            }
            signature.append(s).append("L").append(typedName.name).append(";");
        }
        String sig = "";

        if (name.getText().startsWith("__"))
            mv = cw.visitMethod(ACC_PRIVATE + (static_ ? ACC_STATIC : 0) + (class_ ? ACC_STATIC : 0), name.getText().substring(2), "(" + sig + ")V", null, null);
        else if (name.getText().startsWith("_"))
            mv = cw.visitMethod(ACC_PROTECTED + (static_ ? ACC_STATIC : 0) + (class_ ? ACC_STATIC : 0), name.getText().substring(1), "(" + sig + ")V", null, null);
        else
            mv = cw.visitMethod(ACC_PUBLIC + (static_ ? ACC_STATIC : 0) + (class_ ? ACC_STATIC : 0), name.getText(), "()V", null, null);

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
            visit(block);
//            mv.visitLabel(endLabel);

            mv.visitInsn(RETURN);

            mv.visitMaxs(0, 0);
            mv.visitEnd();
        } finally {
            flags.clear(F_CPL_STATIC_FUNC);
            flags.clear(F_CPL_CLASS_FUNC);
            flags.clear(F_CPL_INSTANCE_FUNC);

            mv = null;
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

        JClass value = new JClass(path + fileName + "/" + name.getText());
        imports.put(name.getText(), value);
        symbols.put(name.getText(), value);

        byte[] bytes = cw.toByteArray();
        try {
            String s = "build/pythonc/" + path + fileName;
            if (!new File(s).exists()) {
                new File(s).mkdirs();
            }
            FileOutputStream fileOutputStream = new FileOutputStream("build/pythonc/" + path + fileName + "/" + name.getText() + ".class");
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
        PythonParser.AssignmentContext assignment = ctx.assignment();
        if (assignment != null) {
            return visit(assignment);
        }
        throw new RuntimeException("No supported matching simple_stmt found of type " + ctx.getClass().getSimpleName() + " for:\n" + ctx.getText());
    }

    @Override
    public Object visitAssignment(PythonParser.AssignmentContext ctx) {
        flags.set(F_CPL_ASSIGN);
        try {
            curLabel = new Label();
            String name = ctx.NAME().getText();

            PythonParser.AugassignContext augassign = ctx.augassign();
            if (augassign != null) {
                throw new RuntimeException("augassign not supported for:\n" + ctx.getText());
            }
            PythonParser.Star_expressionsContext starExpressionsContext = ctx.star_expressions();
            if (starExpressionsContext != null) {
                return visit(starExpressionsContext);
            }
            PythonParser.ExpressionContext expressionContext = ctx.expression();
            if (expressionContext != null) {
                PythonParser.Annotated_rhsContext value = ctx.annotated_rhs();
                if (value != null) {
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
                        String descriptor = switch (visit1) {
                            case Symbol symbol -> symbol.type(this);
                            default ->
                                    throw new RuntimeException("Annotated RHS not supported with type " + visit1.getClass().getSimpleName() + " for:\n" + value.getText());
                        };
                        FieldVisitor fieldVisitor = cw.visitField(ACC_PUBLIC + ACC_STATIC, name, descriptor, null, null);
                        fieldVisitor.visitEnd();

                        symbols.put(name, new ImportedField(name, descriptor, getName(), ctx.start.getLine()));

                        if (cw == rootCw) {
                            var a = switch (visit) {
                                case PyConstant pyConstant -> pyConstant;
                                case Symbol symbol -> symbol;
                                case PyExpr pyExpr -> pyExpr;
                                case String string -> new PyConstant(string, ctx.start.getLine());
                                case Integer integer -> new PyConstant(integer, ctx.start.getLine());
                                case Boolean booleanValue -> new PyConstant(booleanValue, ctx.start.getLine());
                                case Long longValue -> new PyConstant(longValue, ctx.start.getLine());
                                case Float floatValue -> new PyConstant(floatValue, ctx.start.getLine());
                                case Double doubleValue -> new PyConstant(doubleValue, ctx.start.getLine());
                                case Character charValue -> new PyConstant(charValue, ctx.start.getLine());
                                case Byte byteValue -> new PyConstant(byteValue, ctx.start.getLine());
                                case Short shortValue -> new PyConstant(shortValue, ctx.start.getLine());
                                default ->
                                        throw new RuntimeException("Annotated RHS not supported with type " + visit.getClass().getSimpleName() + " for:\n" + value.getText());
                            };
                            a.load(rootInitMv, this, a.preload(rootInitMv, this, false), false);
                            rootInitMv.visitFieldInsn(PUTSTATIC, getName(), name, descriptor);
                        }
                    } else {
                        createVariable(name, switch (visit1) {
                            case FuncCall funcCall -> funcCall.name();
                            case Symbol symbol -> symbol.name();
                            default ->
                                    throw new RuntimeException("Annotated RHS not supported with type " + visit1.getClass().getSimpleName() + " for:\n" + value.getText());
                        }, switch (visit) {
                            case PyConstant pyConstant -> pyConstant;
                            case Symbol symbol -> symbol;
                            case PyExpr pyExpr -> pyExpr;
                            case String string -> new PyConstant(string, ctx.start.getLine());
                            case Integer integer -> new PyConstant(integer, ctx.start.getLine());
                            case Boolean booleanValue -> new PyConstant(booleanValue, ctx.start.getLine());
                            case Long longValue -> new PyConstant(longValue, ctx.start.getLine());
                            case Float floatValue -> new PyConstant(floatValue, ctx.start.getLine());
                            case Double doubleValue -> new PyConstant(doubleValue, ctx.start.getLine());
                            case Character charValue -> new PyConstant(charValue, ctx.start.getLine());
                            case Byte byteValue -> new PyConstant(byteValue, ctx.start.getLine());
                            case Short shortValue -> new PyConstant(shortValue, ctx.start.getLine());
                            default ->
                                    throw new RuntimeException("Annotated RHS not supported with type " + visit.getClass().getSimpleName() + " for:\n" + value.getText());
                        }, false);
                    }

                    return Unit.Instance;
                } else {
                    throw new CompilerException("Annotated RHS is required " + getLocation(ctx));
                }
            } else {
                PythonParser.Annotated_rhsContext value = ctx.annotated_rhs();
                if (value != null) {
                    String type = symbols.get(name).type(this);
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
                this.imports.put(s.getKey(), new JClass(((String) visit(dottedNameContext)).replace(".", "/") + "/" + s.getValue()));
                this.symbols.put(s.getKey(), new JClass(((String) visit(dottedNameContext)).replace(".", "/") + "/" + s.getValue()));
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
        rootCw.visit(V1_8, ACC_PUBLIC, getName(), null, "java/lang/Object", null);

        rootInitMv = rootCw.visitMethod(ACC_PUBLIC | ACC_STATIC, "<clinit>", "()V", null, null);
        rootInitMv.visitCode();

        // Set the file name of the class to fileName
        rootCw.visitSource(fileName + ".py", null);

        visit(statements);

        rootInitMv.visitInsn(RETURN);
        rootInitMv.visitMaxs(0, 0);
        rootInitMv.visitEnd();
        rootCw.visitEnd();

        try {
            File file = new File("build/pythonc/" + getName() + ".class");
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

    private String getName() {
        return path + "Py_" + fileName;
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

    public void pack(String outputDir, String outputJar) {
        // Pack "build/rustc/**/*.class"
        try {
            Process process = Runtime.getRuntime().exec("jar cf " + outputJar + " -C " + outputDir + " .");
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

    private static final class FuncCall implements Symbol {
        private final PythonParser.PrimaryContext atom;
        private final PythonParser.PrimaryContext primaryContext;
        private final PythonParser.ArgumentsContext arguments;
        private Method method;
        private Constructor<?> constructor;
        private boolean kwargs;
        private boolean varArgs;
        private boolean dynCtor;
        private Object callArgs;

        private FuncCall(PythonParser.PrimaryContext atom, PythonParser.PrimaryContext primaryContext,
                         PythonParser.ArgumentsContext arguments) {
            this.atom = atom;
            this.primaryContext = primaryContext;
            this.arguments = arguments;
        }

        public void write(MethodVisitor mv, PythonCompiler compiler) {
            mv.visitLineNumber(atom.getStart().getLine(), new Label());
            Object visit = compiler.visit(atom);
            if (visit == null) {
                throw new RuntimeException("atom not supported for:\n" + atom.getText());
            }


            if (visit instanceof TypedName) {
                throw new RuntimeException("atom not supported for:\n" + atom.getText());
            } else if (visit instanceof PyObjectRef) {
                // Print the array
                Symbol symbol = (Symbol) compiler.visit(atom);
                if (symbol == null) {
                    throw new CompilerException("Symbol '" + name() + "' not found (" + compiler.getLocation(this) + ")");
                }
                String named = symbol.name();
                String owner = symbol.type(compiler);
                if (symbol instanceof PyObjectRef(String name, int lineNo)) {
                    symbol = compiler.symbols.get(name);
                    switch (symbol) {
                        case PyBuiltinFunction builtinFunction -> {
                            this.varArgs = builtinFunction.varArgs;
                            this.kwargs = builtinFunction.kwargs;
                            this.dynCtor = builtinFunction.dynCtor;

                            if (dynCtor) {
                                compiler.flags.set(F_DYN_CALL);
                                try {
                                    setupCallArgs(mv, compiler);
                                } finally {
                                    compiler.flags.clear(F_DYN_CALL);
                                }

                                // Create Object Array
                                createArray(mv, compiler, ((List<?>) callArgs).stream().map(v -> (PyExpr) switch (v) {
                                    case String s -> new PyConstant(s, lineNo);
                                    case Integer i -> new PyConstant(i, lineNo);
                                    case PyExpr pyExpr -> pyExpr;
                                    case null, default ->
                                            throw new RuntimeException("argument not supported for:\n" + arguments.getText());
                                }).toList(), new Object[((List<?>) callArgs).size()]);

                                // Kwargs
                                // Create Map<String, Object>
                                // Call Mao.of()
                                mv.visitMethodInsn(INVOKESTATIC, "java/util/Map", "of", "()Ljava/util/Map;", true);
                                mv.visitMethodInsn(INVOKESTATIC, "pythonvm/builtins/_Builtins", "print", "([Ljava/lang/Object;Ljava/util/Map;)V", false);
                                return;
                            }

                            if (atom.NAME() == null && atom.atom() != null && atom.atom().NAME() != null) {
                                String mapOwner = builtinFunction.mapOwner;
                                setupCallArgs(mv, compiler);

                                mv.visitMethodInsn(INVOKESTATIC, mapOwner, name(), "(" + argDesc(compiler, callArgs) + ")" + type(compiler), false);
                                return;
                            } else {
                                throw new RuntimeException("Unexpected builtin function call!");
                            }
                        }
                        case JClass jClass -> {
                            if (atom.NAME() == null && atom.atom() != null && atom.atom().NAME() != null) {

                                // Constructor Call
                                mv.visitTypeInsn(NEW, className(owner));
                                mv.visitInsn(DUP);
                                setupCallArgs(mv, compiler);

                                mv.visitMethodInsn(INVOKESPECIAL, className(owner), "<init>", "(" + argDesc(compiler, callArgs) + ")V", false);
                                return;
                            }
                            boolean isInterface = jClass.isInterface(compiler);
                            setupCallArgs(mv, compiler);
                            mv.visitMethodInsn(INVOKESTATIC, className(owner), name(), "(" + argDesc(compiler, callArgs) + ")" + type(compiler), isInterface);
                            return;
                        }
                        case PyVariable pyVariable -> {
                            symbol.load(mv, compiler, symbol.preload(mv, compiler, false), false);
                            setupCallArgs(mv, compiler);

                            String s = argDesc(compiler, callArgs);
                            if (owner.startsWith("L") && owner.endsWith(";")) {
                                String internalName = owner.substring(1, owner.length() - 1);
                                Symbol symbol1 = compiler.symbols.get(internalName.substring(internalName.lastIndexOf("/") + 1));
                                if (symbol1 == null) {
                                    throw new CompilerException("Symbol '" + owner.substring(1, owner.length() - 1) + "' not found (" + compiler.getLocation(this) + ")");
                                }

                                if (symbol1 instanceof JClass jClass) {
                                    owner = jClass.type(compiler);
                                    if (owner.startsWith("L") && owner.endsWith(";")) {
                                        String internal = owner.substring(1, owner.length() - 1);
                                        String qualified = internal.replace("/", ".");
                                        Class<?> aClass = null;
                                        try {
                                            aClass = Class.forName(qualified, false, getClass().getClassLoader());
                                            if (aClass.isInterface()) {
                                                mv.visitMethodInsn(INVOKEINTERFACE, internal, name(), "(" + s + ")" + type(compiler), true);
                                                return;
                                            }
                                        } catch (ClassNotFoundException e) {
                                            throw new CompilerException("JVM Class '" + qualified + "' not found (" + compiler.getLocation(this) + ")");
                                        }
                                    }
                                } else if (symbol1 instanceof PyBuiltinFunction func) {
                                    owner = func.owner(compiler);
                                } else {
                                    throw new RuntimeException("Unknown symbol type: " + symbol1.getClass().getSimpleName());
                                }
                            } else {
                                throw new RuntimeException("Unknown JVM descriptor for class name: " + owner);
                            }
                            mv.visitMethodInsn(INVOKEVIRTUAL, className(owner), name(), "(" + s + ")" + type(compiler), false);
                        }
                        case null, default -> throw new UnsupportedOperationException("Not implemented!");
                    }
                }
                if (symbol instanceof PyBuiltinFunction) {
//                    symbol.load(mv, compiler, null, false);
                    throwError(mv, "Unimplemented function call: atom=" + atom.getText() + ", primaryContext=" + primaryContext.getText() + ", arguments=" + arguments.getText());
                    return;
                } else if (symbol instanceof JClass) {
                    setupCallArgs(mv, compiler);
                    mv.visitMethodInsn(INVOKESTATIC, className(owner), name(), "(" + (arguments == null ? "" : argDesc(compiler, callArgs)) + ")V", false);
                }
            } else if (visit instanceof FuncCall func) {
                FuncCall funcCall = (FuncCall) visit;
                funcCall.write(mv, compiler);

                setupArgs(mv, compiler, arguments == null ? List.of() : (List<?>) compiler.visit(arguments));

                if (atom.NAME() == null && atom.atom() != null && atom.atom().NAME() != null) {
                    String owner = owner(compiler);

                    // Constructor Call
                    mv.visitTypeInsn(NEW, className(owner));
                    mv.visitInsn(DUP);
                    setupCallArgs(mv, compiler);

                    mv.visitMethodInsn(INVOKESPECIAL, className(owner), "<init>", "(" + argDesc(compiler, callArgs) + ")V", false);
                    return;
                }
                mv.visitMethodInsn(INVOKEVIRTUAL, className(owner(compiler)), name(), "(" + argDesc(compiler, callArgs) + ")" + func.type(compiler), false);
                // Print the array
                //                throw new RuntimeException("atom not supported for:\n" + atom.getText());
                //                mv.visitMethodInsn(INVOKEDYNAMIC, type(compiler), name(), "(" + compiler.visit(arguments) + ")V", false);
                // Throw exception with error message

//                throwError(mv, "Unimplemented function call: atom=" + atom.getText() + ", primaryContext=" + primaryContext.getText() + ", arguments=" + arguments.getText());
            } else {
                throw new RuntimeException("No supported matching atom found for: " + visit.getClass().getName());
            }

            mv.visitLineNumber(atom.getStop().getLine(), new Label());
        }

        private void setupCallArgs(MethodVisitor mv, PythonCompiler compiler) {
            callArgs = List.of();
            if (arguments != null) {
                callArgs = compiler.visit(arguments);
            }
            if (!(callArgs instanceof List)) {
                throw new RuntimeException("arguments not supported for:\n" + arguments.getText());
            }
            setupArgs(mv, compiler, (List<?>) callArgs);
        }

        private String argDesc(PythonCompiler compiler, Object callArgs) {
            if (arguments == null) return "";
            return switch (callArgs) {
                case List<?> list -> {
                    StringJoiner joiner = new StringJoiner("");
                    int listSize = list.size();
                    if (varArgs) listSize--;
                    if (kwargs) listSize--;
                    for (int j = 0; j < listSize; j++) {
                        Object o = list.get(j);
                        String string = switch (o) {
                            case String s -> "Ljava/lang/String;";
                            case Integer i -> "I";
                            case Long i -> "J";
                            case Float i -> "F";
                            case Double i -> "D";
                            case Boolean i -> "Z";
                            case PyVariable pyVar -> owner(compiler);
                            case PyObjectRef objectRef -> owner(compiler);
                            case FuncCall funcCallWithArgs -> funcCallWithArgs.type(compiler);
                            case null, default ->
                                    throw new RuntimeException("argument not supported with type " + o.getClass().getSimpleName() + " for:\n" + arguments.getText());
                        };
                        joiner.add(string);
                    }
                    if (varArgs) joiner.add("[Ljava/lang/Object;");
                    if (kwargs) joiner.add("Ljava/util/Map;");
                    yield joiner.toString();
                }
                default -> throw new RuntimeException("argument not supported for:\n" + arguments.getText());
            };
        }

        @Override
        public String type(PythonCompiler compiler) {
            String owner = owner(compiler);
            if (owner.startsWith("[")) owner = componentTypeR(owner);
            String substring = owner.substring(1, owner.length() - 1).replace("/", ".");
            try {
                String name = name();
                Class<?> aClass = Class.forName(substring, false, getClass().getClassLoader());
                String desc = argDesc(compiler, callArgs);
                if (name.equals("<init>")) {
                    for (Constructor<?> constructor : aClass.getDeclaredConstructors()) {
                        StringBuilder sb = new StringBuilder();
                        for (Class<?> paramType : constructor.getParameterTypes()) {
                            sb.append(paramType.descriptorString());
                        }

                        if (sb.toString().equals(desc)) {
                            this.constructor = constructor;
                            return owner;
                        }
                    }
                    for (Constructor<?> constructor : aClass.getConstructors()) {
                        StringBuilder sb = new StringBuilder();
                        for (Class<?> paramType : constructor.getParameterTypes()) {
                            sb.append(paramType.descriptorString());
                        }

                        if (sb.toString().equals(desc)) {
                            this.constructor = constructor;
                            return owner;
                        }
                    }

                    throw new CompilerException("Java constructor not found in " + substring + " (" + compiler.getLocation(this) + ")");
                }
                Stack<Class<?>> stack = new Stack<>();
                stack.push(aClass);

                while (!stack.isEmpty()) {
                    if (aClass != Object.class && aClass.getSuperclass() != null) stack.push(aClass.getSuperclass());
                    for (Class<?> anInterface : aClass.getInterfaces()) {
                        stack.push(anInterface);
                    }

                    aClass = stack.pop();
                    for (Method method : aClass.getDeclaredMethods()) {
                        if (!method.getName().equals(name)) continue;
                        StringBuilder sb = new StringBuilder();
                        for (Class<?> paramType : method.getParameterTypes()) {
                            sb.append(paramType.descriptorString());
                        }

                        if (sb.toString().equals(desc)) {
                            this.method = method;
                            return method.getReturnType().descriptorString();
                        }
                    }
                    for (Method method : aClass.getMethods()) {
                        if (!method.getName().equals(name)) continue;
                        StringBuilder sb = new StringBuilder();
                        for (Class<?> paramType : method.getParameterTypes()) {
                            sb.append(paramType.descriptorString());
                        }

                        if (sb.toString().equals(desc)) {
                            this.method = method;
                            return method.getReturnType().descriptorString();
                        }
                    }
                }
                throw new CompilerException("Java function '" + name + "' not found with arguments '" + desc + "' in " + substring + " (" + compiler.getLocation(this) + ")");
            } catch (ClassNotFoundException e) {
                Symbol symbol = compiler.symbols.get(substring.substring(substring.lastIndexOf(".") + 1));
                if (symbol.name().equals(substring.replace(".", "/"))) return symbol.type(compiler);
                throw compiler.typeNotFound(substring, this);
            }
        }

        private String componentTypeR(String owner) {
            owner = owner.substring(1);
            if (owner.startsWith("[")) owner = componentTypeR(owner);
            return owner;
        }

        private String className(String owner) {
            if (owner.startsWith("L") && owner.endsWith(";")) {
                return owner.substring(1, owner.length() - 1);
            }

            throw new RuntimeException("Unknown JVM descriptor for class name: " + owner);
        }

        private void setupArgs(MethodVisitor mv, PythonCompiler compiler, List<?> visit1) {
            List<PyExpr> exprs = new ArrayList<>();
            for (int j = 0, objectsSize = visit1.size(); j < objectsSize; j++) {
                Object o = visit1.get(j);
                final int line = arguments.args().expression(j).getStart().getLine();
                switch (o) {
                    case String s -> exprs.add(new PyConstant(s, line));
                    case Boolean b -> exprs.add(new PyConstant(b, line));
                    case Float f -> exprs.add(new PyConstant(f, line));
                    case Double d -> exprs.add(new PyConstant(d, line));
                    case Character c -> exprs.add(new PyConstant(c, line));
                    case Byte b -> exprs.add(new PyConstant(b, line));
                    case Short s -> exprs.add(new PyConstant(s, line));
                    case Integer i -> exprs.add(new PyConstant(i, line));
                    case Long l -> exprs.add(new PyConstant(l, line));
                    case PyExpr ref -> exprs.add(ref);
                    case null, default -> {
                        if (o != null) {
                            throw new RuntimeException("argument not supported with type " + o.getClass().getName() + " for:\n" + arguments.getText());
                        }
                        throw new RuntimeException("argument not supported with type <NULL> for:\n" + arguments.getText());
                    }
                }
            }

            Object[] preloaded = new Object[exprs.size()];
            for (int i = 0, exprsSize = exprs.size(); i < exprsSize; i++) {
                PyExpr expr = exprs.get(i);
                //                expr.load(mv, compiler, null, false);
            }

            //            createArray(mv, compiler, exprs, preloaded);
        }

        private void createArray(MethodVisitor mv, PythonCompiler compiler, List<PyExpr> exprs, Object[] preloaded) {
            mv.visitIntInsn(BIPUSH, exprs.size());
            mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");

            // Create an array of objects
            for (int i = 0; i < exprs.size(); i++) {
                mv.visitLineNumber(arguments.args().expression(i).getStart().getLine(), new Label());
                mv.visitInsn(DUP);
                mv.visitIntInsn(BIPUSH, i); // Push the index onto the stack
                preloaded[i] = exprs.get(i).preload(mv, compiler, true);
                exprs.get(i).load(mv, compiler, preloaded[i], true);
                mv.visitInsn(AASTORE);
            }
        }

        @Override
        public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
            return null;
        }

        @Override
        public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
            write(mv, compiler);
        }

        @Override
        public String name() {
            if (atom.NAME() != null) {
                return atom.NAME().getText();
            }

            if (atom.atom() != null) {
                if (atom.children.size() == 1) {
                    return "<init>";
                }
                return atom.atom().NAME().getText();
            }

            throw new RuntimeException("No NAME found for:\n" + atom.getText());
        }

        public String owner(PythonCompiler compiler) {
            Object visit = compiler.visit(atom);
            switch (visit) {
                case PyObjectRef(String name, int lineNo) -> {
                    Symbol symbol = compiler.symbols.get(name);
                    if (symbol instanceof PyBuiltinFunction func) {
                        return func.owner(compiler);
                    }

                    return symbol.type(compiler);
                }
                case PyBuiltinFunction func -> {
                    return ((PyBuiltinFunction) visit).type(compiler);
                }
                case FuncCall func -> {
                    return func.type(compiler);
                }
                case null, default -> throw new RuntimeException("Unknown atom: " + visit.getClass().getSimpleName());
            }
        }

        @Override
        public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit) {
            throw new CompilerException("Cannot set a function " + compiler.getLocation(atom));
        }

        @Override
        public int lineNo() {
            return atom.getStart().getLine();
        }

        public PythonParser.PrimaryContext atom() {
            return atom;
        }

        public PythonParser.PrimaryContext primaryContext() {
            return primaryContext;
        }

        public PythonParser.ArgumentsContext arguments() {
            return arguments;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (FuncCall) obj;
            return Objects.equals(this.atom, that.atom) &&
                   Objects.equals(this.primaryContext, that.primaryContext) &&
                   Objects.equals(this.arguments, that.arguments);
        }

        @Override
        public int hashCode() {
            return Objects.hash(atom, primaryContext, arguments);
        }

        @Override
        public String toString() {
            return "FuncCallWithArgs[" +
                   "atom=" + atom + ", " +
                   "primaryContext=" + primaryContext + ", " +
                   "arguments=" + arguments + ']';
        }

    }

    private static void throwError(MethodVisitor mv, String value) {
        mv.visitTypeInsn(NEW, "java/lang/Error");
        mv.visitInsn(DUP);
        mv.visitLdcInsn(value);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Error", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitInsn(ATHROW);
    }

    private int createVariable(String name, String type, PyExpr expr, boolean boxed) {
        mv.visitLineNumber(expr.lineNo(), new Label());

        Symbol symbol = symbols.get(type);
        if (symbol == null) {
            throw new CompilerException("Symbol '" + type + "' not found (" + getLocation(expr) + ")");
        }
        String descriptor = symbol.type(this);
        int index = 1;
        if (descriptor == null) {
            descriptor = switch (type) {
                case "str" -> "Ljava/lang/String;";
                case "int" -> "J";
                case "float" -> "D";
                case "bool" -> "Z";
                case "bytes", "bytearray" -> "[B";
                case "list" -> "Ljava/util/List;";
                case "dict" -> "Ljava/util/Map;";
                case "set" -> "Ljava/util/Set;";
                case "tuple" -> "[Ljava/lang/Object;";
                default -> {
                    if (imports.get(type) == null) {
                        throw typeNotFound(type, expr);
                    }

                    yield "L" + imports.get(type) + ";";
                }
            };
        }

        if (descriptor.equals("J") || descriptor.equals("D")) {
            index = 2;
        }
        Object preloaded = expr.preload(mv, this, false);
        expr.load(mv, this, preloaded, boxed);

        Label label = new Label();
        symbols.put(name, new PyVariable(name, type, currentVariableIndex, expr.lineNo(), label));
//        mv.visitLabel(label);
        mv.visitLocalVariable(name, descriptor, null, endLabel, endLabel, currentVariableIndex);
        mv.visitLineNumber(expr.lineNo(), label);
        mv.visitVarInsn(switch (type) {
            case "str" -> ASTORE;
            case "int" -> LSTORE;
            case "float" -> DSTORE;
            default -> {
                if (imports.get(type) == null) {
                    throw typeNotFound(type, expr);
                }

                yield ASTORE;
            }
        }, currentVariableIndex);

        return currentVariableIndex += index;
    }

    private CompilerException typeNotFound(String type, PyExpr expr) {
        return new CompilerException("Type '" + type + "' not found " + getLocation(expr));
    }

    private String getLocation(PyExpr expr) {
        return Path.of(path + fileName).toAbsolutePath() + ".py:" + expr.lineNo();
    }

    private String getLocation(ParserRuleContext ctx) {
        return "(" + Path.of(path + fileName).toAbsolutePath() + ".py:" + ctx.start.getLine() + ":" + ctx.start.getCharPositionInLine() + ")";
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
            args.add(visit);

            if (visit == null) {
                throw new RuntimeException("Unknown visitArgs for expression context: " + expressionContext.getText());
            }


            if (!flags.get(F_DYN_CALL) && !flags.get(F_CPL_CLASS_INHERITANCE)) {
                switch (visit) {
                    case String s -> mv.visitLdcInsn(s);
                    case Integer s -> mv.visitLdcInsn(s);
                    case Float s -> mv.visitLdcInsn(s);
                    case Long s -> mv.visitLdcInsn(s);
                    case Double s -> mv.visitLdcInsn(s);
                    case Character s -> mv.visitLdcInsn(s);
                    case Byte s -> mv.visitLdcInsn(s);
                    case Short s -> mv.visitLdcInsn(s);
                    case Boolean s -> mv.visitLdcInsn(s);
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
            } else if (flags.get(F_CPL_CLASS_INHERITANCE)) {
                switch (visit) {
                    case Symbol symbol -> {
                        if (mv == null) {
                            args.add(symbol.type(this));
                            continue;
                        }

                        throw new RuntimeException("TODO");
                    }
                    default ->
                            throw new UnsupportedOperationException("Not implemented: " + visit.getClass().getName());
                }
            }
        }

        return args;
    }

    private record PyObjectRef(String name, int lineNo) implements Symbol {

        @Override
        public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
            switch (compiler.symbols.get(name)) {
                case PyVariable variable -> {
                    // Set variable to "<name>.class"
                    Symbol symbol = compiler.symbols.get(name);
                    if (symbol == null) {
                        throw new CompilerException("Symbol '" + name + "' not found (" + compiler.getLocation(this) + ")");
                    }

                    return symbol.preload(mv, compiler, false);
                }
                case PyObjectRef objectRef ->
                    // Set variable to "<name>.class"
                    //TODO
                        throwError(mv, "TODO");
                case JClass jclass -> {
                    // Set variable to "<name>.class"
                    if (mv == null) {
                        return jclass.getType();
                    }
                    jclass.load(mv, compiler, jclass.preload(mv, compiler, false), false);
                    return jclass;
                }
                case ImportedField importedField -> {

                }
                default ->
                        throw new CompilerException("Symbol '" + name + "' invalid type: " + compiler.symbols.get(name).getClass().getSimpleName() + " (" + compiler.getLocation(this) + ")");
            }

            return null;
        }

        @Override
        public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
            if (compiler.symbols.get(name) == null) {
                throw new CompilerException("Symbol '" + name + "' not found (" + compiler.getLocation(this) + ")");
            }

            switch (compiler.symbols.get(name)) {
                case PyVariable variable -> {
                    // Set variable to "<name>.class"
                    Symbol symbol = compiler.symbols.get(name);
                    if (symbol == null) {
                        throw new CompilerException("Symbol '" + name + "' not found (" + compiler.getLocation(this) + ")");
                    }
                    symbol.load(mv, compiler, preloaded, boxed);
                }
                case PyObjectRef objectRef -> {
                    // Set variable to "<name>.class"
                    mv.visitLdcInsn(name);
                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);

                    // Set variable
                    mv.visitVarInsn(ISTORE, compiler.currentVariableIndex++);
                }
                case ImportedField importedField -> {
                    importedField.load(mv, compiler, importedField.preload(mv, compiler, false), false);
                }
                default ->
                        throw new CompilerException("Symbol '" + name + "' not found (" + compiler.getLocation(this) + ")");
            }
        }

        @Override
        public String type(PythonCompiler compiler) {
            if (compiler.symbols.get(name) == null) {
                throw new CompilerException("Symbol '" + name + "' not found (" + compiler.getLocation(this) + ")");
            }
            return compiler.symbols.get(name).type(compiler);
        }

        @Override
        public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit) {
            throw new CompilerException("Cannot set a object reference (" + compiler.getLocation(visit) + ")");
        }
    }

    private record TypedName(String name, String type) {

    }

    private record Self(int lineNo) implements PyExpr {
        @Override
        public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
            return null;
        }

        @Override
        public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
            // Use "this"
//            mv.visitVarInsn(ALOAD, 0);
            throw new RuntimeException("No supported matching Self found for:\n" + this.lineNo);
        }
    }

    private record PyVariable(String name, String type, int index, int lineNo, Label label) implements Symbol {

        @Override
        public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
            switch (type) {
                case "jint" -> {
                    mv.visitTypeInsn(NEW, "java/lang/Integer");
                    mv.visitInsn(DUP);
                }
                case "jfloat" -> {
                    mv.visitTypeInsn(NEW, "java/lang/Float");
                    mv.visitInsn(DUP);
                }
                case "jdouble", "float" -> {
                    mv.visitTypeInsn(NEW, "java/lang/Double");
                    mv.visitInsn(DUP);
                }
                case "jboolean", "bool" -> {
                    mv.visitTypeInsn(NEW, "java/lang/Boolean");
                    mv.visitInsn(DUP);
                }
                case "jbyte" -> {
                    mv.visitTypeInsn(NEW, "java/lang/Byte");
                    mv.visitInsn(DUP);
                }
                case "jshort" -> {
                    mv.visitTypeInsn(NEW, "java/lang/Short");
                    mv.visitInsn(DUP);
                }
                case "jlong", "int" -> {
                    mv.visitTypeInsn(NEW, "java/lang/Long");
                    mv.visitInsn(DUP);
                }
                case "jchar" -> {
                    mv.visitMethodInsn(NEW, "java/lang/Character", "<init>", "(C)V", false);
                    mv.visitInsn(DUP);
                }
            }

            return null;
        }

        @Override
        public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
            mv.visitVarInsn(switch (type) {
                case "str" -> ALOAD;
                case "int" -> LLOAD;
                case "float" -> DLOAD;
                default -> {
                    if (compiler.imports.get(type) == null) {
                        throw compiler.typeNotFound(type, this);
                    }

                    yield ALOAD;
                }
            }, index);

            if (boxed) {
                switch (type) {
                    case "int" -> mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Long", "<init>", "(J)V", false);
                    case "float" -> mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Float", "<init>", "(F)V", false);
                    case "jint" -> mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(I)V", false);
                    case "jfloat" -> mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Float", "<init>", "(F)V", false);
                    case "jlong" -> mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Long", "<init>", "(J)V", false);
                    case "jdouble" -> mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Double", "<init>", "(D)V", false);
                    case "jboolean" -> mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Boolean", "<init>", "(Z)V", false);
                    case "jbyte" -> mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Byte", "<init>", "(B)V", false);
                    case "jshort" -> mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Short", "<init>", "(S)V", false);
                    case "jchar" -> mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Character", "<init>", "(C)V", false);
                }
            }
        }

        @Override
        public String type(PythonCompiler compiler) {
            return switch (type) {
                case "jlong", "int" -> "J";
                case "jdouble", "float" -> "D";
                case "jint" -> "I";
                case "jfloat" -> "F";
                case "jboolean", "bool" -> "Z";
                case "jchar" -> "C";
                default -> {
                    if (compiler.symbols.get(type) == null) {
                        throw compiler.typeNotFound(type, this);
                    }

                    yield compiler.symbols.get(type).type(compiler);
                }
            };
        }

        @Override
        public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit) {
            mv.visitVarInsn(ISTORE, index);
        }
    }

    private interface PyExpr {
        Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed);

        void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed);

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
            STRING, INTEGER, FLOAT, DOUBLE, BOOLEAN, BYTE, SHORT, LONG, CHARACTER
        }

        @Override
        public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
            return null;
        }

        @Override
        public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
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
            return Objects.equals(this.value, that.value) && this.lineNo == that.lineNo;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value, lineNo);
        }

        @Override
        public String toString() {
            return "PyConstant[" + "value=" + value + ", " + "lineNo=" + lineNo + ']';
        }
    }

    private interface Symbol extends PyExpr {
        void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed);

        String name();

        String type(PythonCompiler compiler);

        void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit);
    }

    private class Decorators {
        public final Map<String, FuncCall> byJvmName = new HashMap<>();
    }

    private record JClass(String className) implements Symbol {
        @Override
        public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
            return null;
        }

        @Override
        public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
            // Do <name>.class
            mv.visitLdcInsn(Type.getType("L" + className + ";"));
        }

        public Type getType() {
            return Type.getType("L" + className + ";");
        }

        @Override
        public int lineNo() {
            return 0;
        }

        @Override
        public String name() {
            return className;
        }

        @Override
        public String type(PythonCompiler compiler) {
            return "L" + className + ";";
        }

        @Override
        public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit) {

        }

        public boolean isInterface(PythonCompiler compiler) {
            try {
                Class<?> aClass = Class.forName(className.replace("/", "."), false, getClass().getClassLoader());
                return aClass.isInterface();
            } catch (ClassNotFoundException e) {
                throw new CompilerException("JVM Class '" + className + "' not found (" + compiler.getLocation(this) + ")");
            }
        }
    }

    private class PyBuiltinClass implements Symbol {
        public final String jvmName;
        public final String jvmUnboxed;
        public final String extName;
        public final String pyName;

        public PyBuiltinClass(String jvmName, String extName, String pyName) {
            this(jvmName, jvmName, extName, pyName);
        }

        public PyBuiltinClass(String jvmName, String jvmUnboxed, String extName, String pyName) {
            this.jvmName = jvmName;
            this.jvmUnboxed = jvmUnboxed;
            this.extName = extName;
            this.pyName = pyName;
        }

        @Override
        public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
            return null;
        }

        @Override
        public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
            if (jvmName.startsWith("L") && jvmName.endsWith(";")) {
                String className = jvmName.substring(1, jvmName.length() - 1).replace("/", ".");
                mv.visitLdcInsn(jvmName);
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
                return;
            }
            throw new RuntimeException("Unknown JVM name: " + jvmName);
        }

        @Override
        public int lineNo() {
            return 0;
        }

        @Override
        public String name() {
            return pyName;
        }

        @Override
        public String type(PythonCompiler compiler) {
            return jvmUnboxed;
        }

        @Override
        public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit) {
            throw new CompilerException("Can't set class: " + pyName);
        }
    }

    private class PyBuiltinFunction implements Symbol {
        public final String jvmOwner;
        public final String mapOwner;
        public final String[] signatures;
        public final int params;
        public final String name;
        public final boolean varArgs;
        public final boolean kwargs;
        public final boolean dynCtor;

        enum Mode {
            NORMAL,
            VARARGS,
            KWARGS,
            DYN_CTOR,
        }

        public PyBuiltinFunction(String jvmOwner, String mapOwner, String[] signatures, int params, String name) {
            this(jvmOwner, mapOwner, signatures, params, name, Mode.NORMAL);
        }

        public PyBuiltinFunction(String jvmOwner, String mapOwner, String[] signatures, int params, String name, Mode mode) {
            this.jvmOwner = jvmOwner;
            this.mapOwner = mapOwner;
            this.signatures = signatures;
            this.params = params;
            this.name = name;
            this.varArgs = mode == Mode.VARARGS;
            this.kwargs = mode == Mode.KWARGS;
            this.dynCtor = mode == Mode.DYN_CTOR;
        }

        @Override
        public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
            return null;
        }

        @Override
        public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
            throw new UnsupportedOperationException("Not allowed");
        }

        @Override
        public int lineNo() {
            return 0;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String type(PythonCompiler compiler) {
            String substring = mapOwner.replace("/", ".");
            try {
                Class<?> aClass = Class.forName(substring, false, getClass().getClassLoader());
                for (Method method : aClass.getMethods()) {
                    String name1 = method.getName();
                    if (!name1.equals(name)) {
                        continue;
                    }

                    return Arrays.stream(method.getParameterTypes()).map(v -> v.descriptorString()).collect(Collectors.joining(""));
                }

                throw new CompilerException("No matching function: " + substring + "." + name + "(...)");
            } catch (ClassNotFoundException ignored) {
                throw new CompilerException("JVM Class '" + substring + "' not found (" + compiler.getLocation(this) + ")");
            }

        }

        @Override
        public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit) {

        }

        public String owner(PythonCompiler compiler) {
            return "L" + mapOwner + ";";
        }
    }

    private class ImportedField implements Symbol {
        public final String name;
        public final String type;
        public final String owner;
        private int lineNo;

        public ImportedField(String name, String type, String owner, int lineNo) {
            this.name = name;
            this.type = type;
            this.owner = owner;
            this.lineNo = lineNo;
        }

        @Override
        public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
            return null;
        }

        @Override
        public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
            mv.visitFieldInsn(GETSTATIC, owner, name, type);
        }

        @Override
        public int lineNo() {
            return lineNo;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String type(PythonCompiler compiler) {
            return type;
        }

        @Override
        public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit) {

        }
    }
}
