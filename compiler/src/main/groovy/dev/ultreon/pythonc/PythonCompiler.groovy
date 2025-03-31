package dev.ultreon.pythonc

import com.google.common.base.CaseFormat
import dev.ultreon.pythonc.classes.*
import dev.ultreon.pythonc.expr.*
import dev.ultreon.pythonc.expr.fstring.FStringElement
import dev.ultreon.pythonc.expr.fstring.FStringExpr
import dev.ultreon.pythonc.functions.FunctionDefiner
import dev.ultreon.pythonc.functions.PyBuiltinFunction
import dev.ultreon.pythonc.functions.PyFunction
import dev.ultreon.pythonc.functions.param.*
import dev.ultreon.pythonc.lang.PyAST
import dev.ultreon.pythonc.statement.*
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.TerminalNode
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.objectweb.asm.*
import org.objectweb.asm.tree.*
import org.objectweb.asm.util.CheckClassAdapter

import java.nio.file.*
import java.util.function.Consumer
import java.util.function.Predicate
import java.util.jar.JarOutputStream
import java.util.stream.Collectors
import java.util.stream.Stream
import java.util.zip.ZipEntry

import static org.objectweb.asm.Opcodes.*

@SuppressWarnings(value = [ "t" , "UnusedReturnValue" ])
class PythonCompiler extends PythonParserBaseVisitor<Object> {
    public static final int F_CPL_FUNCTION = 0
    public static final int F_CPL_CLASS = 1
    public static final int F_CPL_DECORATOR = 2
    public static final int F_CPL_IMPORT = 3
    public static final int F_CPL_INSTANCE_FUNC = 4
    public static final int F_CPL_STATIC_FUNC = 5
    public static final int F_CPL_CLASS_FUNC = 6
    public static final int F_CPL_ASSIGN = 7
    public static final int F_CPL_TYPE_ANNO = 8
    public static final int F_CPL_CLASS_INHERITANCE = 9
    public static final int F_CPL_FILE = 10
    public static final int F_READ_CALL = 41
    public static final int F_DYN_CALL = 50

    public static final String E_NOT_ALLOWED = "Not allowed"
    public static final String FMT_CLASS = "L%s;"
    public static final PyParameter[] BUILTIN_FUNCTION_PARAMETER_TYPES = new PyParameter[]{new PyArgsParameter("args"), new PyKwargsParameter("kwargs")}
    public static expectations = new CompileExpectations()
    public static Path rootDir
    private static final CURRENT = new ThreadLocal<PythonCompiler>()
    private final Path outputPath
    public builtins = new Builtins(this)

    def undefinedClasses = new LinkedHashSet<>()
    private ClassNode classOut = null
    private Path pathOfFile
    private String path = ""
    private String fileName = "Main"
    PyImports imports
    private State state = State.File
    final BitSet flags = new BitSet()
    final Decorators decorators = new Decorators()
    MethodVisitor methodOut
    MethodVisitor rootInitMv
    int currentVariableIndex = 1
    Label endLabel
    Label curLabel
    def classes = new PyClasses()
    public static classCache = new JvmClassCache()
    private PyClass curPyClass = null
    private static final compileErrors = new ArrayList<CompilerException>()
    final contextStack = new Stack<Context>()

    public final JvmWriter writer = new JvmWriter(this)
    @Nullable
    public JvmClassCompilable compilingClass
    @Nullable
    public PyClass definingClass
    @Nullable
    public Module definingModule
    @Nullable PyClass definingInstance
    private PyFunction definingFunction
    private final Stack<MemberContext> memberContextStack = new Stack<>()
    public static final JvmModuleCache moduleCache = new JvmModuleCache()
    public final PyGlobalUnlocked unlocked = new PyGlobalUnlocked()
    private PyExpression leftExpr

    static boolean isInstanceOf(PythonCompiler pc, Type pop1, String owner) {
        Type type = Type.getObjectType(owner)
        JvmClass require = classCache.require(pc, type)
        JvmClass pop = classCache.require(pc, pop1)
        return pop.doesInherit(pc, require)
    }

    static JvmClass getType(Class<Object> objectClass) {
        return classCache.require(null, Type.getObjectType(objectClass.name))
    }

    static PythonCompiler getCurrent() {
        return CURRENT.get()
    }

    static void compileSources(String sourceDir, String outputDir) {
        Path sourcePath = Path.of(sourceDir)
        rootDir = sourcePath.toAbsolutePath()

        // Walk the directory
        Path path = Path.of(System.getProperty("user.dir")).relativize(sourcePath)
        try {
            try (Stream<Path> walk = Files.walk(path)) {
                walk
                        .map(Path::toString)
                        .filter(string -> string.endsWith(".py"))
                        .collect(Collectors.toSet())
                        .forEach(v -> {
                            try {
                                new PythonCompiler(outputDir).compile(new File(v), new File(sourceDir))
                            } catch (CompilerException e) {
                                compileErrors.add(e)
                            } catch (IOException e) {
                                throw new RuntimeException(e)
                            }
                        })
            }
            try (Stream<Path> walk = Files.walk(path)) {
                walk
                        .filter(p -> !p.toString().endsWith(".py"))
                        .map(v -> sourcePath.relativize(v.toAbsolutePath()).toString())
                        .forEach(v -> {
                            try {
                                // Copy over resources
                                Path path1 = Path.of("build/tmp/compilePython", v)
                                System.out.println("path1 = " + path1)
                                if (Files.notExists(path1)) Files.createDirectories(path1.parent)
                                Path sourceDirPath = Path.of(sourceDir, v)
                                Path path2 = sourceDirPath
                                if (Files.isDirectory(path2)) return
                                Files.copy(sourceDirPath, path1.toAbsolutePath(), StandardCopyOption.REPLACE_EXISTING)
                            } catch (IOException e) {
                                throw new RuntimeException(e)
                            }
                        })
            }
        } catch (IOException e) {
            throw new RuntimeException(e)
        }

        if (!compileErrors.empty) {
            expectations.exceptions.forEach(compileErrors::addFirst)

            for (CompilerException ex : compileErrors) {
                try {
                    System.err.println(ex.toAdvancedString())
                } catch (IOException e) {
                    throw new RuntimeException(e)
                }
            }

            System.exit(1)
        }
    }

    JvmClass getClassSymbol(String className) {
        var name = className.substring(className.lastIndexOf(".") + 1)

        if (!SymbolContext.current().hasSymbol(name)) {
            undefinedClasses.add(className)
            return null
        }

        PySymbol symbol = SymbolContext.current().getSymbol(name)
        if (symbol instanceof JvmClass) {
            return (JvmClass) symbol
        }

        undefinedClasses.add(className)
        return null
    }

    static void writeClass(Type type, ClassNode classNode) {
        String internalName = type.internalName
        Path resolve = rootDir.resolve(internalName + ".class")
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS)
        classNode.accept(cw)
        byte[] byteArray = cw.toByteArray()

        try {
            Files.write(resolve, byteArray, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
        } catch (IOException e) {
            throw new CompilerException("Failed to write classfile '" + resolve + "': " + e)
        }
    }

    static FunctionContext startFunction(FunctionDefiner owner, PyFunction function, String name, PyParameter[] parameters) {
        return FunctionContext.pushContext(owner, function, name, parameters)
    }

    <T> T lastContext(@NotNull Class<T> symbolContextClass) {
        for (int i = contextStack.size() - 1; i >= 0; i--) {
            Context context = contextStack.get(i)
            if (symbolContextClass.isInstance(context)) {
                return symbolContextClass.cast(context)
            }
        }
        throw new RuntimeException("No " + symbolContextClass.simpleName + " context found")
    }

    Context lastContext(@NotNull Predicate<Context> predicate) {
        for (int i = contextStack.size() - 1; i >= 0; i--) {
            Context context = contextStack.get(i)
            if (predicate.test(context)) {
                return context
            }
        }
        throw new RuntimeException("No context found matching the predicate!")
    }

    static void endFunction(FunctionContext functionContext) {
        MethodNode mv = functionContext.function().node()

        AbstractInsnNode last = mv.instructions.last

        if (functionContext.popNeeded) {
            throw new RuntimeException("Function " + functionContext.function().name + " needs to pop:\n" + functionContext.location())
        }

        if (!last instanceof InsnNode || last.opcode !in [RETURN, ARETURN, IRETURN, LRETURN, FRETURN, DRETURN]) {
            Type type = functionContext.function().returnType()
            if (type != null) {
                if (type.sort == Type.BYTE || type.sort == Type.SHORT || type.sort == Type.INT || type.sort == Type.CHAR || type.sort == Type.BOOLEAN) {
                    mv.visitInsn(ICONST_0)
                    mv.visitInsn(IRETURN)
                } else if (type.sort == Type.LONG) {
                    mv.visitInsn(LCONST_0)
                    mv.visitInsn(LRETURN)
                } else if (type.sort == Type.FLOAT) {
                    mv.visitInsn(FCONST_0)
                    mv.visitInsn(FRETURN)
                } else if (type.sort == Type.DOUBLE) {
                    mv.visitInsn(DCONST_0)
                    mv.visitInsn(DRETURN)
                } else if (type.sort == Type.OBJECT || type.sort == Type.ARRAY) {
                    mv.visitInsn(ACONST_NULL)
                    mv.visitInsn(ARETURN)
                } else if (type.sort == Type.VOID) {
                    mv.visitInsn(RETURN)
                } else {
                    throw new RuntimeException("Unknown type: " + type)
                }
            } else {
                mv.visitInsn(ACONST_NULL)
                mv.visitInsn(ARETURN)
            }
        }

        mv.visitMaxs(0, 0)
        mv.visitEnd()
    }

    void checkPop(Location location) {
        if (writer.context.popNeeded)
            throw new RuntimeException("Who forgot to pop the stack?" + location.getFormattedText())
    }

    void checkNoPop(Location location) {
        if (!writer.context.popNeeded)
            throw new RuntimeException("Expression returned void!" + location.getFormattedText())
    }

    void checkNoPop(Location location, Type type) {
        if (!writer.context.popNeeded)
            throw new RuntimeException("Expression should return " + type + " not void!" + location.getFormattedText())
        if (classCache.require(this, type).doesInherit(this, classCache.object(this)))
            throw new RuntimeException("Expression should return " + type + " not void!\n" + location)
    }

    void unlock(GlobalSettable globalSettable) {
        this.unlocked.add(globalSettable.getName())
    }

    void classDefinition(Type owner, Consumer<ClassNode> runnable) {
        ClassNode classNode = new ClassNode(ASM9)
        classNode.access = ACC_PUBLIC
        classNode.version = Opcodes.V1_8
        classNode.name = owner.internalName

        classNode.interfaces = List.of("org/python/_internal/PyObject")

        swapClass(classNode, () -> {
            runnable.accept(classNode)
        })

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS)
        classNode.accept(cw)
        byte[] byteArray = cw.toByteArray()

        try {
            Files.createDirectories(outputPath.resolve(owner.internalName + ".class").parent)
            Files.write(outputPath.resolve(owner.internalName + ".class"), byteArray, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
        } catch (IOException e) {
            compileErrors.add(new CompilerException("Failed to write class file: " + e.toString()))
        }
    }

    void moduleDefinition(ModulePath path, Consumer<ClassNode> runnable) {
        ClassNode classNode = new ClassNode(ASM9)
        classNode.access = ACC_PUBLIC
        classNode.version = Opcodes.V1_8
        classNode.name = path.asType().internalName
        classNode.superName = "java/lang/Object"
        classNode.sourceFile = path.toString().replace(".", "/") + ".py"
        classNode.sourceDebug = path.toString().replace(".", "/") + ".py"

        classNode.interfaces = List.of("org/python/_internal/PyModule")

        swapClass classNode, {
            runnable.accept(classNode)
        }

        dumpClassNodeInfo(classNode)

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS)
        def adapter = new CheckClassAdapter(cw, true)
        classNode.accept(adapter)
        byte[] byteArray = cw.toByteArray()

        try {
            Files.createDirectories(outputPath.resolve(path.asType().internalName + ".class").parent)
            Files.write(outputPath.resolve(path.asType().internalName + ".class"), byteArray, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
        } catch (IOException e) {
            compileErrors.add(new CompilerException("Failed to write class file: " + e.toString()))
        }
    }

    void classInit(Consumer<MethodNode> o) {
        if (classOut == null) throw new IllegalStateException("No class output set!")

        MethodNode methodNode = new MethodNode()
        methodNode.name = "<clinit>"
        methodNode.desc = "()V"
        methodNode.access = ACC_STATIC
        methodNode.instructions = new InsnList()
        methodNode.signature = null
        methodNode.exceptions = null
        methodNode.visibleAnnotations = null
        methodNode.invisibleAnnotations = null
        methodNode.visibleTypeAnnotations = null
        methodNode.invisibleTypeAnnotations = null

        classOut.methods.add(methodNode)

        swapMethod(methodNode, () -> o.accept(methodNode))

        methodNode.visitInsn(RETURN)
        methodNode.visitMaxs(0, 0)
        methodNode.visitEnd()
    }

    void dumpClassNodeInfo(ClassNode classNode) {
        StringBuilder builder = new StringBuilder()

        builder.append(Location.ANSI_RED).append("Class ").append(Location.ANSI_PURPLE).append(classNode.name).append(Location.ANSI_RESET).append(Location.ANSI_YELLOW).append(" {")

        StringBuilder indent = new StringBuilder()
        indent.append(Location.ANSI_RED).append("\nSourceFile ").append(Location.ANSI_BLUE).append(classNode.sourceFile).append("\n")
        indent.append(Location.ANSI_RED).append("SourceDebug ").append(Location.ANSI_BLUE).append(classNode.sourceDebug).append("\n")
        indent.append(Location.ANSI_RED).append("SuperName ").append(Location.ANSI_YELLOW).append(classNode.superName).append("\n")
        indent.append(Location.ANSI_RED).append("Interfaces ").append(Location.ANSI_YELLOW).append(classNode.interfaces).append("\n")
        indent.append(Location.ANSI_RED).append("Access ").append(Location.ANSI_GREEN).append(classNode.access).append("\n")
        indent.append(Location.ANSI_RED).append("Version ").append(Location.ANSI_PURPLE).append(classNode.version).append("\n")

        def extraIndent = new StringBuilder()
        for (MethodNode methodNode : classNode.methods) {
            extraIndent.append("\n").append(dumpMethodNodeInfo(methodNode).replace("\n", "\n"))
        }
        extraIndent.append("\n")

        indent.append(extraIndent.toString().replace("\n", "\n  "))
        indent.append(Location.ANSI_RED).append("Fields ").append(Location.ANSI_RESET).append(" {\n")
        extraIndent = new StringBuilder()
        for (FieldNode fieldNode : classNode.fields) {
            extraIndent.append("\n").append(dumpFieldNodeInfo(fieldNode).replace("\n", "\n"))
        }
        extraIndent.append("\n}");
        indent.append(extraIndent.toString().replace("\n", "\n  "))

        indent.append(Location.ANSI_RED).append("\n  InnerClasses ").append(Location.ANSI_RESET).append(" {\n")

        extraIndent = new StringBuilder()
        for (InnerClassNode innerClassNode : classNode.innerClasses) {
            extraIndent.append("\n").append(dumpInnerClassNodeInfo(innerClassNode)).append("\n")
        }
        extraIndent.append("\n}")

        indent.append(extraIndent.toString().replace("\n", "\n  "))
        indent.append(Location.ANSI_RESET).append("\n}")

        builder.append(indent.toString().replace("\n", "\n  "))
        builder.append(Location.ANSI_RESET).append("\n}")

        System.out.println(builder.toString())

        Thread.sleep(1000)
    }

    String dumpMethodNodeInfo(MethodNode methodNode) {
        StringBuilder builder = new StringBuilder()

        builder.append(Location.ANSI_RED).append("Method ").append(Location.ANSI_PURPLE).append(methodNode.name.replace("/", Location.ANSI_RESET + "/" + Location.ANSI_YELLOW)).append(Location.ANSI_RESET).append(Location.ANSI_YELLOW).append(format(Type.getType(methodNode.desc)))
        builder.append(" {\n")
        builder.append("  ").append(Location.ANSI_RED).append("Access ").append(Location.ANSI_BLUE).append(methodNode.access).append("\n")
        builder.append("  ").append(Location.ANSI_RED).append("Signature ").append(Location.ANSI_BLUE).append(methodNode.signature).append("\n")
        builder.append("  ").append(Location.ANSI_RED).append("Exceptions ").append(Location.ANSI_BLUE).append(methodNode.exceptions).append("\n")

        builder.append("  ").append(Location.ANSI_RED).append("Instructions ").append(Location.ANSI_RESET).append(" {\n    ")
        for (AbstractInsnNode abstractInsnNode : methodNode.instructions) {
            builder.append(dumpAbstractInsnNodeInfo(abstractInsnNode).replace("\n", "\n    "))
            if (abstractInsnNode != methodNode.instructions.last) {
                builder.append("\n    ")
            }
        }
        builder.append(Location.ANSI_RESET).append("\n  }\n}")

        return builder.toString()
    }

    String dumpAbstractInsnNodeInfo(AbstractInsnNode abstractInsnNode) {
        StringBuilder builder = new StringBuilder()

        def name = abstractInsnNode.class.simpleName
        if (name.endsWith("Node")) name = name.substring(0, name.length() - 4)
        if (name.endsWith("Insn") && name != "Insn") name = name.substring(0, name.length() - 4)
        builder.append(Location.ANSI_RED).append(name).append(Location.ANSI_RESET)
        if (abstractInsnNode instanceof MethodInsnNode) {
            MethodInsnNode methodInsnNode = (MethodInsnNode) abstractInsnNode
            builder.append(" ").append(Location.ANSI_RED).append(Location.ANSI_YELLOW).append(methodInsnNode.owner).append(Location.ANSI_RESET).append(":").append(Location.ANSI_PURPLE).append(methodInsnNode.name).append(Location.ANSI_YELLOW).append(format(Type.getType(methodInsnNode.desc)))
        } else if (abstractInsnNode instanceof FieldInsnNode) {
            FieldInsnNode fieldInsnNode = (FieldInsnNode) abstractInsnNode
            builder.append(" ").append(Location.ANSI_RED).append("Field ").append(Location.ANSI_PURPLE).append(fieldInsnNode.name).append(Location.ANSI_RESET).append(":").append(Location.ANSI_YELLOW).append(format(Type.getType(fieldInsnNode.desc)))
        } else if (abstractInsnNode instanceof LdcInsnNode) {
            LdcInsnNode ldcInsnNode = (LdcInsnNode) abstractInsnNode
            builder.append(" ").append(Location.ANSI_RED).append(Location.ANSI_GREEN).append(format(ldcInsnNode.cst))
        } else if (abstractInsnNode instanceof LineNumberNode) {
            LineNumberNode lineNumberNode = (LineNumberNode) abstractInsnNode
            builder.append(" ").append(Location.ANSI_RED).append(Location.ANSI_GREEN).append(lineNumberNode.line)
        } else if (abstractInsnNode instanceof LabelNode) {
            LabelNode labelNode = (LabelNode) abstractInsnNode
            builder.append(" ").append(Location.ANSI_RED).append(Location.ANSI_BLUE).append(format(labelNode.label))
        } else if (abstractInsnNode instanceof JumpInsnNode) {
            JumpInsnNode jumpInsnNode = (JumpInsnNode) abstractInsnNode
            builder.append(" ").append(Location.ANSI_RED).append("Target ").append(Location.ANSI_BLUE).append(jumpInsnNode.label)
        } else if (abstractInsnNode instanceof InsnNode) {
        } else if (abstractInsnNode instanceof VarInsnNode) {
            VarInsnNode varInsnNode = (VarInsnNode) abstractInsnNode
            builder.append(" ").append(Location.ANSI_RED).append(Location.ANSI_BLUE).append(varInsnNode.var)
        } else if (abstractInsnNode instanceof TypeInsnNode) {
            TypeInsnNode typeInsnNode = (TypeInsnNode) abstractInsnNode
            builder.append(" ").append(Location.ANSI_RED).append(Location.ANSI_BLUE).append(format(Type.getObjectType(typeInsnNode.desc)))
        } else if (abstractInsnNode instanceof MultiANewArrayInsnNode) {
            MultiANewArrayInsnNode multiANewArrayInsnNode = (MultiANewArrayInsnNode) abstractInsnNode
            builder.append(" ").append(Location.ANSI_RED).append("Type ").append(Location.ANSI_BLUE).append(multiANewArrayInsnNode.desc)
        } else if (abstractInsnNode instanceof IntInsnNode) {
            IntInsnNode intInsnNode = (IntInsnNode) abstractInsnNode
            builder.append(" ").append(Location.ANSI_RED).append("Value ").append(Location.ANSI_GREEN).append(intInsnNode.operand)
        } else if (abstractInsnNode instanceof FrameNode) {
            FrameNode frameNode = (FrameNode) abstractInsnNode
            builder.append(" ").append(Location.ANSI_RED).append("Type ").append(Location.ANSI_BLUE).append(frameNode.type)
            builder.append(" ").append(Location.ANSI_RED).append("Local ").append(Location.ANSI_PURPLE).append(frameNode.local)
            builder.append(" ").append(Location.ANSI_RED).append("Stack ").append(Location.ANSI_PURPLE).append(frameNode.stack)
        } else if (abstractInsnNode instanceof InvokeDynamicInsnNode) {
            InvokeDynamicInsnNode invokeDynamicInsnNode = (InvokeDynamicInsnNode) abstractInsnNode
            builder.append(" ").append(Location.ANSI_PURPLE).append(invokeDynamicInsnNode.name)
            builder.append(Location.ANSI_YELLOW).append(format(Type.getType(invokeDynamicInsnNode.desc))).append(Location.ANSI_RESET)
            if (abstractInsnNode.opcode != -1) builder.append(" [").append(Location.ANSI_RED).append("Opcode ").append(Location.ANSI_GREEN).append(abstractInsnNode.getOpcode()).append(Location.ANSI_RESET).append("]")
            builder.append(" {\n")
            builder.append("  ").append(Location.ANSI_RED).append("Bsm ").append(Location.ANSI_YELLOW).append(invokeDynamicInsnNode.bsm.owner.replace("/", Location.ANSI_RESET + "/" + Location.ANSI_YELLOW)).append(Location.ANSI_RESET).append(":").append(Location.ANSI_PURPLE).append(invokeDynamicInsnNode.bsm.name).append(Location.ANSI_YELLOW).append(format(Type.getType(invokeDynamicInsnNode.bsm.desc))).append("\n")
            for (int i = 0; i < invokeDynamicInsnNode.bsmArgs.length; i++) {
                builder.append("  ").append(Location.ANSI_RED).append("BsmArg[").append(Location.ANSI_BLUE).append(i).append(Location.ANSI_RED).append("] ").append(Location.ANSI_YELLOW).append(invokeDynamicInsnNode.bsmArgs[i]).append("\n")
            }
            builder.deleteCharAt(builder.length() - 1).append(Location.ANSI_RESET).append("\n}")
            return builder.toString()
        } else {
            builder.append(" ").append(Location.ANSI_RED).append("Unknown ").append(Location.ANSI_BLUE).append(abstractInsnNode)
        }
        if (abstractInsnNode.opcode != -1) builder.append(Location.ANSI_RESET).append(" [").append(Location.ANSI_RED).append("Opcode ").append(Location.ANSI_BLUE).append(abstractInsnNode.getOpcode()).append(Location.ANSI_RESET).append("]")

        return builder.toString()
    }

    String format(object) {
        if (object instanceof String) return Location.ANSI_BLUE + "\"" + object.toString().replace("\\", Location.ANSI_RED + "\\\\" + Location.ANSI_BLUE).replace("\"", Location.ANSI_RED + "\\\"" + Location.ANSI_BLUE).replace("\n", Location.ANSI_RED + "\\n" + Location.ANSI_BLUE).replace("\r", Location.ANSI_RED + "\\r" + Location.ANSI_BLUE).replace("\t", Location.ANSI_RED + "\\t" + Location.ANSI_BLUE).replace("\b", Location.ANSI_RED + "\\b" + Location.ANSI_BLUE).replace("\f", Location.ANSI_RED + "\\f" + Location.ANSI_BLUE) + "\""
        if (object instanceof Character) return Location.ANSI_BLUE + "\"" + object.toString().replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t").replace("\b", "\\b").replace("\f", "\\f") + "\""
        if (object instanceof Label) return object.toString().replace("L", Location.ANSI_WHITE + "L" + Location.ANSI_GREEN)
        if (object instanceof Type) {
            int sort = object.sort
            switch (sort) {
                case Type.OBJECT:
                    return Location.ANSI_RED + "L" + Location.ANSI_YELLOW + object.internalName.replace("/", Location.ANSI_RESET + "/" + Location.ANSI_YELLOW) + Location.ANSI_RED + ";" + Location.ANSI_RESET
                case Type.ARRAY:
                    return Location.ANSI_RED + "[" + Location.ANSI_RESET + format(object.elementType) + Location.ANSI_RESET
                case Type.METHOD:
                    return Location.ANSI_RESET + "(" + object.argumentTypes.collect { format(it) }.join("") + Location.ANSI_RESET + ")" + Location.ANSI_RESET + format(object.returnType) + Location.ANSI_RESET
                default:
                    return Location.ANSI_RED + object.toString()
            }
        }
        return object
    }

    String dumpFieldNodeInfo(FieldNode fieldNode) {
        StringBuilder builder = new StringBuilder()

        builder.append(Location.ANSI_RED).append("Name ").append(Location.ANSI_BLUE).append(fieldNode.name).append("\n")
        builder.append("  ").append(Location.ANSI_RED).append("Desc ").append(Location.ANSI_YELLOW).append(fieldNode.desc).append("\n")
        builder.append("  ").append(Location.ANSI_RED).append("Access ").append(Location.ANSI_GREEN).append(fieldNode.access).append("\n")
        builder.append("  ").append(Location.ANSI_RED).append("Signature ").append(Location.ANSI_YELLOW).append(fieldNode.signature).append("\n")

        return builder.toString()
    }

    String dumpInnerClassNodeInfo(InnerClassNode classNode) {
        StringBuilder builder = new StringBuilder()

        builder.append(Location.ANSI_RED).append("Name ").append(Location.ANSI_BLUE).append(classNode.name).append("\n")
        builder.append("  ").append(Location.ANSI_RED).append("OuterName ").append(Location.ANSI_BLUE).append(classNode.outerName).append("\n")
        builder.append("  ").append(Location.ANSI_RED).append("InnerName ").append(Location.ANSI_BLUE).append(classNode.innerName).append("\n")
        builder.append("  ").append(Location.ANSI_RED).append("Access ").append(Location.ANSI_GREEN).append(classNode.access).append("\n")

        return builder.toString()
    }

    enum State {
        File, Decorators
    }

    PythonCompiler(String outputDir) {
        CURRENT.set(this)
        this.outputPath = Path.of(outputDir)
    }

    static void setSymbol(String name, PySymbol symbol) {
        SymbolContext.current().setSymbol(name, symbol)
    }

    static PySymbol getSymbol(String name) {
        return SymbolContext.current().getSymbol(name)
    }

    static boolean hasSymbol(String name) {
        return SymbolContext.current().hasSymbol(name)
    }

    static PySymbol getSymbol(String name, Location location) {
        PySymbol symbol = getSymbol(name)
        if (symbol == null)
            throw new CompilerException("Unknown symbol: " + name, location)
        return symbol
    }

    @Override
    Object visit(@Nullable ParseTree tree) {
        if (tree == null) {
            throw new RuntimeException("Tree is null")
        }
        System.out.print("Visiting: " + tree.class.simpleName + " " + tree.text)
        if (!contextStack.empty) {
            System.out.println(" (Stack size before: ${context(Context).stackSize})")
        } else {
            System.out.println()
        }

        Object visit = super.visit(tree)
        if (visit == null) {
            String simpleName = tree.class.simpleName
            throw new RuntimeException("Visit unavailable for visit${simpleName.substring(0, simpleName.length() - "Context".length())}:\n$tree.text")
        }
        System.out.print("Visited: " + tree.class.simpleName + " " + tree.text)
        if (!contextStack.empty) {
            System.out.println " (Stack size after: ${context(Context).stackSize})"
        } else {
            System.out.println()
        }

        return visit
    }

    @Override
    List<PyStatement> visitStatements(PythonParser.StatementsContext ctx) {
        List<PyStatement> statements = new ArrayList<>()
        for (int i = 0; i < ctx.statement().size(); i++) {
            checkPopped(ctx, i)
            statements.add(visitStatement(ctx.statement(i)))
        }
        return statements
    }

    private @Nullable ErrorValue checkPopped(PythonParser.StatementsContext ctx, int i) {
        Context context = context(Context.class)
        if (context.popNeeded && i > 0) {
            PythonParser.StatementContext statement = ctx.statement(i - 1)
            if (!compileErrors.empty) {
                return ErrorValue.Instance
            }
            if (statement == null)
                throw new RuntimeException("Didn't fully pop before statement " + (i - 1) + " for:\n" + getTextFor(ctx))

            List<String> classNames = new ArrayList<>()
            int stackSize = context.stackSize
            while (context.popNeeded) {
                classNames.add(context.pop().className)
            }
            throw new RuntimeException("Didn't fully pop (" + stackSize + " left) in statement " + (i - 1) + " for '" + String.join(", ", classNames) + getTextFor(statement))
        }
        return null
    }

    @Override
    PyStatement visitStatement(PythonParser.StatementContext ctx) {
        PythonParser.Compound_stmtContext compoundStmtContext = ctx.compound_stmt()
        PythonParser.Simple_stmtsContext simpleStmtsContext = ctx.simple_stmts()
        if (simpleStmtsContext != null) {
            return visitSimple_stmts(simpleStmtsContext)
        }
        if (compoundStmtContext != null) {
            return visitCompound_stmt(compoundStmtContext)
        }
        throw new RuntimeException("No supported matching statement found for:\n" + getTextFor(ctx))
    }

    @Override
    PyStatement visitCompound_stmt(PythonParser.Compound_stmtContext ctx) {
        PythonParser.Class_defContext classDefContext = ctx.class_def()
        if (classDefContext != null) {
            return visitClass_def(classDefContext).definition()
        }

        PythonParser.Function_defContext functionDefContext = ctx.function_def()
        if (functionDefContext != null) {
            return visitFunction_def(functionDefContext)
        }

        PythonParser.If_stmtContext ifStmtContext = ctx.if_stmt()
        if (ifStmtContext != null) {
            return visitIf_stmt(ifStmtContext)
        }

        PythonParser.While_stmtContext whileStmtContext = ctx.while_stmt()
        if (whileStmtContext != null) {
            return visitWhile_stmt(whileStmtContext)
        }

        PythonParser.For_stmtContext forStmtContext = ctx.for_stmt()
        if (forStmtContext != null) {
            return visitFor_stmt(forStmtContext)
        }

        throw new RuntimeException("No supported matching compound_stmt found for:\n" + getTextFor(ctx))
    }

    @Override
    ForStatement visitFor_stmt(PythonParser.For_stmtContext ctx) {
        PythonParser.BlockContext block = ctx.block()
        if (block == null) throw new CompilerException("While statement doesn't have content", locate(ctx))

        List<PyExpression> expressions = visitStar_targets(ctx.star_targets())
        if (expressions.size() > 1)
            throw new CompilerException("For statement can only have one target", locate(ctx))
        if (expressions.empty)
            throw new CompilerException("For statement must have at least one target", locate(ctx))

        List<StarExpression> starExpressions = visitStar_expressions(ctx.star_expressions())


        PyBlock elseBlock = null
        if (ctx.else_block() != null) {
            elseBlock = visitElse_block(ctx.else_block())
        }

        return new ForStatement(starExpressions.first.expression(), (VariableExpr) expressions.first, visitBlock(block), elseBlock)
    }

    @Override
    WhileStatement visitWhile_stmt(PythonParser.While_stmtContext ctx) {
        PythonParser.BlockContext block = ctx.block()
        if (block == null) throw new CompilerException("While statement doesn't have content", locate(ctx))

        PyExpression expression = visitNamed_expression(ctx.named_expression())

        PyBlock elseBlock = null
        if (ctx.else_block() != null) {
            elseBlock = visitElse_block(ctx.else_block())
        }

        return new WhileStatement(expression, visitBlock(block), elseBlock, locate(ctx))
    }

    @Override
    IfStatement visitIf_stmt(PythonParser.If_stmtContext ctx) {
        PythonParser.Elif_stmtContext elifStmtContext = ctx.elif_stmt()
        Label elifLabel = null
        if (elifStmtContext != null) {
            elifLabel = new Label()
            writer.lineNumber(elifStmtContext.start.line, elifLabel)
        }

        IfStatementContext context = new IfStatementContext(elifLabel, endLabel)
        this.pushContext(context)

        PythonParser.Named_expressionContext namedExpressionContext = ctx.named_expression()
        PyExpression condition
        if (namedExpressionContext != null) {
            condition = (PyExpression) visit(namedExpressionContext)
            return new IfStatement(condition, (PyBlock) visit(ctx.block()), ctx.elif_stmt() != null ? new PyBlock(List.of(visitElif_stmt(ctx.elif_stmt())), locate(ctx.elif_stmt())) : ctx.else_block() != null ? visitElse_block(ctx.else_block()) : null)
        }

        throw new CompilerException("No condition in if statement", locate(ctx))
    }

    @Override
    IfStatement visitElif_stmt(PythonParser.Elif_stmtContext ctx) {
        PythonParser.Elif_stmtContext elifStmtContext = ctx.elif_stmt()
        Label elifLabel = null
        if (elifStmtContext != null) {
            elifLabel = new Label()
            writer.lineNumber(elifStmtContext.start.line, elifLabel)
        }

        IfStatementContext context = new IfStatementContext(elifLabel, endLabel)
        this.pushContext(context)

        PythonParser.Named_expressionContext namedExpressionContext = ctx.named_expression()
        PyExpression condition
        if (namedExpressionContext != null) {
            condition = (PyExpression) visit(namedExpressionContext)
            return new IfStatement(condition, (PyBlock) visit(ctx.block()), ctx.elif_stmt() != null ? new PyBlock(List.of((PyStatement) visit(ctx.elif_stmt())), locate(ctx.elif_stmt())) : (PyBlock) visit(ctx.else_block()))
        }

        throw new CompilerException("No condition in if statement", locate(ctx))
    }

    @Override
    PyBlock visitElse_block(PythonParser.Else_blockContext ctx) {
        return visitBlock(ctx.block())
    }

    void popContext() {
        contextStack.pop()
        unlocked.pop()
    }

    void popContext(Class<? extends Context> contextClass) {
        def context = contextStack.pop()
        if (!contextClass.isInstance(context)) {
            throw new RuntimeException("Expected context " + contextClass.simpleName + " but got " + context.class.simpleName)
        }
    }

    void pushContext(Context ifStatementContext) {
        contextStack.push(ifStatementContext)
        unlocked.push()
    }

    Context getContext() {
        return contextStack.peek()
    }

    def <T extends Context> T context(Class<T> ifStatementContextClass) {
        return ifStatementContextClass.cast(contextStack.peek())
    }

    @Override
    PyFunction visitFunction_def(PythonParser.Function_defContext ctx) {
        flags.set(F_CPL_FUNCTION)
        try {
            PythonParser.DecoratorsContext decorators = ctx.decorators()
            if (decorators != null) {
                Object visit = visit(decorators)
                if (visit == null) {
                    throw new RuntimeException("Decorators not supported for:\n" + getTextFor(ctx))
                }
            }
            PythonParser.Function_def_rawContext functionDefRawContext = ctx.function_def_raw()
            if (functionDefRawContext != null) {
                return visitFunction_def_raw(functionDefRawContext)
            }
            throw new RuntimeException("No supported matching function_def_raw found for:\n" + getTextFor(ctx))
        } finally {
            flags.clear(F_CPL_FUNCTION)
        }
    }

    @Override
    PyFunction visitFunction_def_raw(PythonParser.Function_def_rawContext ctx) {
        TerminalNode define = ctx.DEF()
        if (define == null) {
            throw new RuntimeException("No DEF found for:\n" + getTextFor(ctx))
        }

        TerminalNode name = ctx.NAME()
        if (name == null) {
            throw new RuntimeException("No NAME found for:\n" + getTextFor(ctx))
        }

        PythonParser.ParamsContext params = ctx.params()

        PythonParser.BlockContext block = ctx.block()
        if (block == null) {
            throw new RuntimeException("No block found for:\n" + getTextFor(ctx))
        }

        if (decorators.byJvmName.containsKey("org/pythonutils/Override")) {
            throw new RuntimeException("Override not supported for:\n" + getTextFor(ctx))
        }

        boolean static_ = decorators.byJvmName.containsKey("org/python/builtins/PyStaticmethod") || definingClass == null
        boolean class_ = decorators.byJvmName.containsKey("org/python/builtins/PyClassmethod")

        if (static_) {
            flags.set(F_CPL_STATIC_FUNC)
        } else if (class_) {
            flags.set(F_CPL_CLASS_FUNC)
        } else {
            flags.set(F_CPL_INSTANCE_FUNC)
        }

        List<PyParameter> parameters = new ArrayList<>()
        if (params != null) {
            PyParameters pyParameters = visitParams(params)
            if (pyParameters == null) {
                throw new RuntimeException("params not supported for:\n" + getTextFor(ctx))
            }

            parameters.addAll(pyParameters.parameters())
        }

        StringBuilder signature = new StringBuilder()
        for (PyParameter typedName : parameters) {
            if (typedName.type() == null) {
                signature.append("Ljava/lang/Object;")
                continue
            }
            if (builtins.getClass(typedName.type()) != null) {
                signature.append("L").append(typedName.type().internalName).append(";")
                continue
            }
            signature.append(typedName.type().descriptor)
        }

        StringJoiner joiner = new StringJoiner("")
        for (PyParameter typedName : parameters) {
            Type type = typedName.type()
            if (type == null) type = Type.getType(Object.class)
            joiner.add(type.descriptor)
        }
        String sig = joiner.toString()

        Type returnType = Type.getType(Object.class)
        if (ctx.expression() != null) {
            Object visit = visit(ctx.expression())
            if (visit == null) {
                throw new RuntimeException("expression not supported for:\n" + getTextFor(ctx))
            }
        }

        PyFunction oldDefiningFunction = definingFunction
        MethodVisitor oldMv = methodOut
        Location location = locate(ctx)
        if (name.text == "__init__") {
            if (definingClass == null) {
                throw new RuntimeException("No defining class found for:\n" + getTextFor(ctx))
            }
            var jvmFunctions = definingClass.definition.functions.get("<init>")
            if (jvmFunctions == null || jvmFunctions.empty) {
                methodOut = rootInitMv

                definingFunction = new PyFunction(definingClass, "<init>", parameters.toArray(PyParameter[]::new), classCache.void_(this), false, location)
                MemberCallExpr.Builder builder1 = MemberCallExpr.builder(
                        new MemberAttrExpr(SelfExpr.of(definingClass, location), "__init__", location),
                        location
                )
                for (PyParameter pyParameter : parameters) {
                    builder1.argument(definingFunction.getVariable(pyParameter.name))
                }
                PyBlock.Builder bodyBuilder = PyBlock.builder(location.firstLine())
                        .statement(new ExpressionStatement(
                                List.of(
                                        new StarExpression(builder1.build(), false, location)
                                ), location
                        ))

                callPyInit(ctx, definingClass, definingFunction.node(), parameters, sig)

                definingFunction.body(bodyBuilder.build())
                if (definingClass == null) definingModule.addFunction(definingFunction)
                definingClass.definition.functions.add(definingFunction)

                definingFunction = new PyFunction(definingClass == null ? definingModule : definingClass, name.text, parameters.toArray(PyParameter[]::new), classCache.require(this, returnType), false, location)
                if (definingClass == null) definingModule.addFunction(definingFunction)
                definingClass.definition.functions.add(definingFunction)
            } else {
                definingFunction = new PyFunction(definingClass, "<init>", parameters.toArray(PyParameter[]::new), classCache.void_(this), false, location)
                MemberCallExpr.Builder builder1 = MemberCallExpr.builder(
                        new MemberAttrExpr(SelfExpr.of(definingClass, location), "__init__", location),
                        location
                )
                for (PyParameter pyParameter : parameters) {
                    builder1.argument(definingFunction.getVariable(pyParameter.name))
                }
                PyBlock.Builder bodyBuilder = PyBlock.builder(location.firstLine())
                        .statement(new ExpressionStatement(
                                List.of(
                                        new StarExpression(builder1.build(), false, location)
                                ), location
                        ))


                for (JvmClass jvmClass : definingClass.superClasses()) {
                    // TODO Multiple inheritance
                }

                callPyInit(ctx, definingClass, definingFunction.node(), parameters, sig)

                definingFunction.body(bodyBuilder.build())
                if (definingClass == null) definingModule.addFunction(definingFunction)
                else definingClass.definition.functions.add(definingFunction)

                definingFunction = new PyFunction(definingClass == null ? definingModule : definingClass, name.text, parameters.toArray(PyParameter[]::new), classCache.require(this, returnType), false, location)
            }
        } else {
            definingFunction = new PyFunction(definingClass == null ? definingModule : definingClass, name.text, parameters.toArray(PyParameter[]::new), classCache.require(this, returnType), definingClass == null || static_, location)
        }

        if (definingInstance != null) {
            currentVariableIndex = 2
        }
        definingFunction.body((PyBlock) visit(block))
        if (definingClass != null) {
            definingInstance = definingClass
        }

        methodOut = oldMv

        PyFunction function = definingFunction
        definingFunction = oldDefiningFunction
        return function
    }

    private void callPyInit(PythonParser.Function_def_rawContext ctx, JvmClass jvmClass, MethodNode ctorInitMv, List<PyParameter> parameters, String sig) {
        // Call <inheritor>.super.__init__(<arguments>);
        ctorInitMv.visitVarInsn(ALOAD, 0)
        int index = 1
        for (PyParameter typedName : parameters) {
            @Nullable JvmClass type = typedName.typeClass()
            if (type == null) {
                ctorInitMv.visitVarInsn(ALOAD, index)
                index++
                continue
            }
            switch (type.type.sort) {
                case Type.ARRAY:
                    throw new RuntimeException("Array not supported for:\n" + getTextFor(ctx))
                case Type.OBJECT:
                    ctorInitMv.visitVarInsn(ALOAD, index)
                    index++
                    break
                case Type.BYTE:
                case Type.SHORT:
                case Type.INT:
                case Type.BOOLEAN:
                case Type.CHAR:
                    ctorInitMv.visitVarInsn(ILOAD, index)
                    index++
                    break
                case Type.LONG:
                    ctorInitMv.visitVarInsn(LLOAD, index)
                    index += 2
                    break
                case Type.FLOAT:
                    ctorInitMv.visitVarInsn(FLOAD, index)
                    index++
                    break
                case Type.DOUBLE:
                    ctorInitMv.visitVarInsn(DLOAD, index)
                    index += 2
                    break
                default:
                    throw new RuntimeException("Type not supported for:\n" + getTextFor(ctx))
            }
        }

        ctorInitMv.visitMethodInsn(INVOKEVIRTUAL, jvmClass.type.internalName, "__init__", "(" + sig + ")V", false)
    }

    private void doReturn(Type returnType) {
        if (returnType.sort == Type.VOID) {
            writer.returnVoid()
        } else if (returnType.sort == Type.ARRAY) {
            writer.pushNull()
            writer.returnObject()
        } else if (returnType.sort == Type.OBJECT) {
            writer.pushNull()
            writer.returnObject()
        } else if (returnType.sort == Type.CHAR) {
            writer.pushInt(0)
            writer.returnChar()
        } else if (returnType.sort == Type.BOOLEAN) {
            writer.pushBoolean(false)
            writer.returnBoolean()
        } else if (returnType.sort == Type.BYTE) {
            writer.pushInt(0)
            writer.returnByte()
        } else if (returnType.sort == Type.SHORT) {
            writer.pushInt(0)
            writer.returnShort()
        } else if (returnType.sort == Type.INT) {
            writer.pushInt(0)
            writer.returnInt()
        } else if (returnType.sort == Type.FLOAT) {
            writer.pushFloat(0f)
            writer.returnFloat()
        } else if (returnType.sort == Type.DOUBLE) {
            writer.pushDouble(0.0)
            writer.returnDouble()
        } else if (returnType.sort == Type.LONG) {
            writer.pushLong(0L)
            writer.returnLong()
        }
    }

    @Override
    PyBlock visitBlock(PythonParser.BlockContext ctx) {
        PythonParser.StatementsContext statementsContext = ctx.statements()
        if (statementsContext != null) {
            List<PyStatement> statements = visitStatements(statementsContext)
            return new PyBlock(statements, locate(ctx))
        }
        throw new RuntimeException("No supported matching statements found for:\n" + getTextFor(ctx))
    }

    @Override
    PyParameters visitParams(PythonParser.ParamsContext ctx) {
        return visitParameters(ctx.parameters())
    }

    @Override
    PyParameters visitParameters(PythonParser.ParametersContext ctx) {
        SelfExpr selfExpr = null
        List<PyParameter> parameters = new ArrayList<>()
        if (ctx.star_etc() != null) {
            if (ctx.param_no_default() != null && !ctx.param_no_default().empty) {
                throw new RuntimeException("param_no_default not supported for:\n" + getTextFor(ctx))
            }
            if (ctx.param_with_default() != null && !ctx.param_with_default().empty) {
                throw new RuntimeException("param_with_default not supported for:\n" + getTextFor(ctx))
            }
            if (ctx.slash_no_default() != null && !ctx.slash_no_default().empty) {
                throw new RuntimeException("slash_no_default not supported for:\n" + getTextFor(ctx))
            }
            if (ctx.slash_with_default() != null && !ctx.slash_with_default().empty) {
                throw new RuntimeException("slash_with_default not supported for:\n" + getTextFor(ctx))
            }

            PyStarEtc pyStarEtc = visitStar_etc(ctx.star_etc())
            parameters.add(pyStarEtc.argsParameter)
            parameters.addAll(pyStarEtc.parameters)
        }

        for (int i = 0; i < ctx.param_no_default().size(); i++) {
            Object visit = visitParam_no_default(ctx.param_no_default(i))
            if (visit == null) {
                throw new RuntimeException("param_no_default not supported for:\n" + getTextFor(ctx))
            }

            if (!(visit instanceof TypedName)) {
                if (visit instanceof SelfExpr) {
                    SelfExpr self = (SelfExpr) visit
                    selfExpr = self
                    continue
                }
                if (visit instanceof String) {
                    String name = (String) visit
                    parameters.add(new PyNormalParameter(name, locate(ctx.param_no_default(i))))
                    continue
                }
                throw new RuntimeException("param_no_default not supported for:\n" + ctx.param_no_default(i).text)
            }

            parameters.add(new PyTypedParameter(visit.name, classCache.require(this, visit.type == null ? Type.getType(Object.class) : visit.type), locate(ctx.param_no_default(i))))

            if (flags.get(F_CPL_FUNCTION)) {
                if (visit.name == "self") {
                    if (!flags.get(F_CPL_INSTANCE_FUNC)) {
                        throw new CompilerException("self on a non-instance method:\n" + getTextFor(ctx))
                    }
                } else {
                    // TODO
                }
            } else {
                throw new RuntimeException("param_no_default not supported for:\n" + getTextFor(ctx))
            }
        }

        for (int i = 0; i < ctx.param_with_default().size(); i++) {
            PythonParser.Param_with_defaultContext paramWithDefaultContext = ctx.param_with_default(i)
            Object visitDef = visit(paramWithDefaultContext.default_assignment())
            Object visit = visit(paramWithDefaultContext.param())
            if (visit == null) {
                throw new RuntimeException("param_no_default not supported for:\n" + getTextFor(ctx))
            }

            if (!(visit instanceof TypedName)) {
                if (visit instanceof SelfExpr) {
                    SelfExpr self = (SelfExpr) visit
                    continue
                }
                if (visit instanceof String) {
                    String name = (String) visit
                    parameters.add(new PyDefaultedParameter(name, (PyExpression) visitDef, locate(ctx.param_with_default(i))))
                    continue
                }
                throw new RuntimeException("param_no_default not supported for:\n" + ctx.param_no_default(i).text)
            }
            TypedName typedName = visit as TypedName

            parameters.add(new PyTypedDefaultParameter(typedName.getName(), typedName.getType(), (PyExpression) visitDef, locate(ctx.param_with_default(i))))

            if (flags.get(F_CPL_FUNCTION)) {
                if ((visit as String) == "self") {
                    if (!flags.get(F_CPL_INSTANCE_FUNC)) {
                        throw new CompilerException("self on a non-instance method:\n" + getTextFor(ctx))
                    }
                } else {
                    // TODO
                }
            } else {
                throw new RuntimeException("param_no_default not supported for:\n" + getTextFor(ctx))
            }
        }
        return new PyParameters(selfExpr, parameters, null, null)
    }

    @Override
    PyStarEtc visitStar_etc(PythonParser.Star_etcContext ctx) {
        TerminalNode star = ctx.STAR()
        PythonParser.Param_no_defaultContext paramNoDefaultContext = ctx.param_no_default()
        List<PythonParser.Param_maybe_defaultContext> paramMaybeDefaultContexts = ctx.param_maybe_default()
        PythonParser.Param_no_default_star_annotationContext paramNoDefaultStarAnnotationContext = ctx.param_no_default_star_annotation()

        List<PyParameter> parameters = new ArrayList<>()
        if (star == null) {
            throw new IllegalStateException("Star (*) not found in parameters!")
        }

        if (paramNoDefaultContext != null) {
            if (paramNoDefaultContext.TYPE_COMMENT() != null) {
                throw new IllegalStateException("TYPE_COMMENT not supported for:\n" + getTextFor(ctx))
            }
            Object visit = visit(paramNoDefaultContext.param())
            if (visit == null) {
                throw new IllegalStateException("param_no_default not supported for:\n" + getTextFor(ctx))
            }
            if (visit instanceof TypedName) {
                TypedName typedName = (TypedName) visit
                parameters.add(new PyTypedParameter(typedName.getName(), typedName.typeClass(this), locate(paramNoDefaultContext)))
            } else if (visit instanceof String) {
                String name = (String) visit
                parameters.add(new PyNormalParameter(name, locate(paramNoDefaultContext)))
            } else {
                throw new IllegalStateException("param_no_default not supported for:\n" + getTextFor(ctx))
            }
        }

        if (paramMaybeDefaultContexts != null) {
            for (PythonParser.Param_maybe_defaultContext paramMaybeDefaultContext : paramMaybeDefaultContexts) {
                if (paramMaybeDefaultContext.TYPE_COMMENT() != null) {
                    throw new IllegalStateException("TYPE_COMMENT not supported for:\n" + getTextFor(ctx))
                }
                Object visit = visit(paramMaybeDefaultContext.param())
                PythonParser.Default_assignmentContext tree = paramMaybeDefaultContext.default_assignment()
                if (visit == null) {
                    throw new IllegalStateException("param_maybe_default not supported for:\n" + getTextFor(ctx))
                }
                if (visit instanceof TypedName) {
                    TypedName typedName = (TypedName) visit
                    parameters.add(new PyTypedDefaultParameter(typedName.getName(), typedName.getType(), tree == null ? null : (PyExpression) this.visit(tree), locate(paramMaybeDefaultContext)))
                } else if (visit instanceof String) {
                    String name = (String) visit
                    parameters.add(new PyDefaultedParameter(name, tree == null ? null : (PyExpression) this.visit(tree), locate(paramMaybeDefaultContext)))
                } else {
                    throw new IllegalStateException("param_maybe_default not supported for:\n" + getTextFor(ctx))
                }
            }

            return new PyStarEtc(new PyArgsParameter(parameters.getFirst().name), parameters.subList(1, parameters.size()))
        }

        if (paramNoDefaultStarAnnotationContext != null) {
            throw new IllegalStateException("param_no_default_star_annotation not supported for:\n" + getTextFor(ctx))
        }

        throw new IllegalStateException("No supported matching star_etc found for:\n" + getTextFor(ctx))
    }

    @Override
    Object visitParam_no_default(PythonParser.Param_no_defaultContext ctx) {
        PythonParser.ParamContext param = ctx.param()
        TerminalNode terminalNode = ctx.TYPE_COMMENT()
        if (visit(param) == "self") {
            if (!flags.get(F_CPL_INSTANCE_FUNC)) {
                throw new CompilerException("self on a non-instance method:\n" + getTextFor(ctx))
            } else {
                return new SelfExpr(curPyClass.type, locate(param))
            }
        }
        if (terminalNode != null) {
            TypedName visit = visitParam(param)
            if (visit == null) {
                throw new RuntimeException("param not supported for:\n" + getTextFor(ctx))
            }
            return visit
        }

        return visitParam(param)
    }

    @Override
    TypedName visitParam(PythonParser.ParamContext ctx) {
        PythonParser.AnnotationContext annotation = ctx.annotation()
        if (annotation != null) {
            flags.set(F_CPL_TYPE_ANNO)
            Object visit = visit(annotation)
            flags.clear(F_CPL_TYPE_ANNO)
            if (visit == null) {
                throw new RuntimeException("annotation not supported for 'null' at:\n" + getTextFor(ctx))
            }
            Type type1
            if (visit instanceof Type) {
                Type type = (Type) visit
                type1 = type
            } else if (visit instanceof String) {
                String name = (String) visit
                type1 = Type.getType(name)
            } else if (visit instanceof JvmClass) {
                JvmClass jvmClass = (JvmClass) visit
                type1 = jvmClass.type
            } else if (visit instanceof TypedName) {
                TypedName typedName = (TypedName) visit
                type1 = typedName.getType()
            } else if (visit instanceof SymbolReferenceExpr) {
                SymbolReferenceExpr symbolReferenceExpr = (SymbolReferenceExpr) visit
                PySymbol symbol = symbolReferenceExpr.symbol()
                if (symbol instanceof JvmClass) {
                    JvmClass jvmClass = (JvmClass) symbol
                    type1 = jvmClass.type
                    return new TypedName(ctx.NAME().text, type1)
                }
                throw new RuntimeException("annotation not supported for '" + visit.class.name + "' at:\n" + getTextFor(ctx))
            } else {
                throw new RuntimeException("annotation not supported for '" + visit.class.name + "' at:\n" + getTextFor(ctx))
            }
            return new TypedName(ctx.NAME().text, type1)
        }

        TerminalNode name = ctx.NAME()
        if (name == null) {
            throw new RuntimeException("No NAME found for:\n" + getTextFor(ctx))
        }
        return new TypedName(name.text, null)
    }

    @Override
    PyClass visitClass_def(PythonParser.Class_defContext ctx) {
        MethodVisitor rootInitMv1 = rootInitMv
        PythonParser.DecoratorsContext decorators = ctx.decorators()

        ClassNode cv1 = classOut
        if (decorators != null) {
            PyDecorators visit = (PyDecorators) visit(decorators)

            if (visit instanceof List<?>) {
                List<?> list = (List<?>) visit
                for (Object o : list) {
                    throw new RuntimeException("No supported matching decorators found at:\n" + getTextFor(ctx))
                }
            } else if (visit != null) {
                throw new RuntimeException("No supported matching decorators found at:\n" + getTextFor(ctx))
            }
        }

        PythonParser.Class_def_rawContext classDefRawContext = ctx.class_def_raw()
        if (classDefRawContext != null) {
            PyClass visit = visitClass_def_raw(classDefRawContext)
            this.rootInitMv = rootInitMv1
            classOut = cv1
            return visit
        }
        this.rootInitMv = rootInitMv1
        classOut = cv1
        throw new RuntimeException("No supported matching class_def found for:\n" + getTextFor(ctx))
    }

    private String getTextFor(ParserRuleContext ctx) {
        Location location = locate(ctx)
        return location.getFormattedText()
    }

    @Override
    PyClass visitClass_def_raw(PythonParser.Class_def_rawContext ctx) {
        TerminalNode name = ctx.NAME()

        List<ClassReference> extending = new ArrayList<>()
        PythonParser.ArgumentsContext arguments = ctx.arguments()
        if (arguments != null) {
            flags.set(F_CPL_CLASS_INHERITANCE)
            flags.set(F_CPL_TYPE_ANNO)
            try {
                List<PyExpression> classList = visitArguments(arguments)
                if (classList == null) {
                    throw new RuntimeException("arguments not supported for:\n" + getTextFor(ctx))
                }

                boolean errored = false
                for (int i = 0, listSize = classList.size(); i < listSize; i++) {
                    PyExpression expression = classList.get(i)
                    if (!(expression instanceof SymbolReferenceExpr)) {
                        compileErrors.add(new CompilerException("Invalid class reference!", expression.location))
                        errored = true
                        continue
                    }
                    SymbolReferenceExpr referenceExpr = (SymbolReferenceExpr) expression
                    extending.add(new ClassReference(referenceExpr))
                }

                if (errored) {
                    throw new CompilerException("Class " + path.replace('/', '.') + name.text + " has invalid class inheritors!", locate(arguments))
                }
            } finally {
                flags.clear(F_CPL_CLASS_INHERITANCE)
                flags.clear(F_CPL_TYPE_ANNO)
            }
        }

        String classname = (path + name.text).replace("/", ".")
        PyClass value = PyClass.create(extending, Type.getObjectType(classname), name.text, locate(ctx))
        this.definingClass = value
        JvmClassCompilable oldCompilingClass = compilingClass
        this.compilingClass = value
        imports.add(name.text, value)
        undefinedClasses.remove(classname)

        classes.add(value)
        classCache.add(this, value)

        curPyClass = value

        this.rootInitMv = methodOut

        PyBlock visit = visitBlock(ctx.block())
        if (visit == null) {
            throw new RuntimeException("block not supported for:\n" + getTextFor(ctx))
        }

        this.methodOut = null

        compilingClass = oldCompilingClass

        definingClass = null
        curPyClass = null
        classOut = null
        return value
    }

    @Override
    Object visitDecorators(PythonParser.DecoratorsContext ctx) {
        List<PythonParser.Named_expressionContext> namedExpressionContexts = ctx.named_expression()
        var oldState = state
        state = State.Decorators
        List<Object> visit = new ArrayList<>()
        for (PythonParser.Named_expressionContext namedExpressionContext : namedExpressionContexts) {
            visit.add(visit(namedExpressionContext))
        }
        if (state != State.Decorators) {
            throw new RuntimeException("Not in decorator state!")
        }
        state = oldState
        return visit
    }

    @Override
    PyExpression visitNamed_expression(PythonParser.Named_expressionContext ctx) {
        PythonParser.ExpressionContext expression = ctx.expression()
        if (expression != null) {
            return visitExpression(expression)
        }
        PythonParser.Assignment_expressionContext assignmentExpressionContext = ctx.assignment_expression()
        if (assignmentExpressionContext != null) {
            return visitAssignment_expression(ctx.assignment_expression())
        }
        throw new RuntimeException("No supported matching named_expression found for:\n" + getTextFor(ctx))
    }

    @Override
    PyStatement visitSimple_stmts(PythonParser.Simple_stmtsContext ctx) {
        List<PyStatement> visit = new ArrayList<>()
        for (int i = 0; i < ctx.simple_stmt().size(); i++) {
            PyStatement visit1 = visitSimple_stmt(ctx.simple_stmt(i))
            if (visit1 == null) {
                throw new RuntimeException("simple_stmt not supported for:\n" + getTextFor(ctx))
            }
            visit.add(visit1)
        }
        return new PyBlock(visit, locate(ctx))
    }

    @Override
    PyStatement visitSimple_stmt(PythonParser.Simple_stmtContext ctx) {
        PythonParser.Import_stmtContext importStmtContext = ctx.import_stmt()
        if (importStmtContext != null) {
            return visitImport_stmt(importStmtContext)
        }
        PythonParser.Del_stmtContext delStmtContext = ctx.del_stmt()
        if (delStmtContext != null) {
            throw new RuntimeException("del_stmt not supported for:\n" + delStmtContext.text)
        }

        PythonParser.Global_stmtContext globalStmtContext = ctx.global_stmt()
        if (globalStmtContext != null) {
            throw new RuntimeException("global_stmt not supported for:\n" + globalStmtContext.text)
        }
        PythonParser.Nonlocal_stmtContext nonlocalStmtContext = ctx.nonlocal_stmt()
        if (nonlocalStmtContext != null) {
            throw new RuntimeException("nonlocal_stmt not supported for:\n" + nonlocalStmtContext.text)
        }
        PythonParser.Assert_stmtContext assertStmtContext = ctx.assert_stmt()
        if (assertStmtContext != null) {
            throw new RuntimeException("assert_stmt not supported for:\n" + assertStmtContext.text)
        }

        PythonParser.Star_expressionsContext starExpressionsContext = ctx.star_expressions()
        if (starExpressionsContext != null) {
            return new ExpressionStatement(visitStar_expressions(starExpressionsContext), locate(starExpressionsContext))
        }
        PythonParser.AssignmentContext assignment = ctx.assignment()
        if (assignment != null) {
            AssignmentStatement visit = visitAssignment(assignment)
            if (context.popNeeded) writer.pop()
            if (context.popNeeded)
                throw new RuntimeException("Type stack is not empty after popping once!" + locate(assignment))
            return visit
        }

        TerminalNode aBreak = ctx.BREAK()
        if (aBreak != null) {
            return new BreakStatement()
        }

        TerminalNode aContinue = ctx.CONTINUE()
        if (aContinue != null) {
            return new ContinueStatement()
        }

        PythonParser.Return_stmtContext returnStmtContext = ctx.return_stmt()
        if (returnStmtContext != null) {
            return visitReturn_stmt(returnStmtContext)
        }

        throw new RuntimeException("No supported matching simple_stmt found of owner " + ctx.class.simpleName + " for:\n" + getTextFor(ctx))
    }

    @Override
    ReturnStatement visitReturn_stmt(PythonParser.Return_stmtContext ctx) {
        PythonParser.Star_expressionsContext starExpressionsContext = ctx.star_expressions()
        if (starExpressionsContext != null) {
            List<StarExpression> visit = visitStar_expressions(starExpressionsContext)
            return new ReturnStatement(visit, definingFunction, locate(ctx))
        }
        throw new RuntimeException("No supported matching return_stmt found for:\n" + getTextFor(ctx))
    }

    @Override
    AssignmentStatement visitAssignment(PythonParser.AssignmentContext ctx) {
        flags.set(F_CPL_ASSIGN)
        try {
            curLabel = new Label()
            writer.lineNumber(ctx.start.line, curLabel)
            TerminalNode name1 = ctx.NAME()
            @Nullable String name = name1 == null ? null : name1.text

            if (name == null) {
                List<PythonParser.Star_targetsContext> starTargetsContexts = ctx.star_targets()
                if (starTargetsContexts.size() != 1) {
                    // TODO Add support for multiple star targets
                    throw new RuntimeException("Expected 1 variable target, got " + starTargetsContexts.size() + " which is unsupported for now, code: " + getTextFor(ctx))
                }

                PythonParser.Star_targetsContext starTargetsContext = starTargetsContexts.first
                if (starTargetsContext == null) {
                    throw new RuntimeException("Variable target is not found :\n" + getTextFor(ctx))
                }

                List<PythonParser.Star_targetContext> starTargetContext = starTargetsContext.star_target()
                if (starTargetContext == null) {
                    throw new RuntimeException("Variable target is not found :\n" + getTextFor(ctx))
                }


                if (starTargetContext.size() != 1) {
                    // TODO Add support for multiple star targets
                    throw new RuntimeException("Expected 1 variable target, got " + starTargetContext.size() + " which is unsupported for now.")
                }
                PythonParser.Star_targetContext starTargetContext1 = starTargetContext.first
                if (starTargetContext1 == null) {
                    throw new RuntimeException("Variable target is not found :\n" + getTextFor(ctx))
                }

                PythonParser.Target_with_star_atomContext targetWithStarAtomContext = starTargetContext1.target_with_star_atom()
                if (targetWithStarAtomContext == null) {
                    throw new RuntimeException("Variable target is not found :\n" + getTextFor(ctx))
                }

                PythonParser.Star_atomContext targetContext = targetWithStarAtomContext.star_atom()
                if (targetContext == null) {
                    List<?> visit = (List<?>) visit(starTargetsContext)
                    Object first = visit.first

                    PythonParser.Star_expressionsContext starExpressionsContext = ctx.star_expressions()
                    if (starExpressionsContext == null) {
                        throw new RuntimeException("Value for variable assignment wasn't found.")
                    }

                    List<PythonParser.Star_expressionContext> starExpressionContexts = starExpressionsContext.star_expression()
                    PythonParser.ExpressionContext expressionContext = getExpressionContext(starExpressionContexts)

                    PyExpression expression = visitExpression(expressionContext)
                    if (expression == null) {
                        throw new RuntimeException("Expression for variable assignment wasn't found.")
                    }

                    if (first instanceof MemberAttrExpr) {
                        MemberAttrExpr field = (MemberAttrExpr) first
                        return new AssignmentStatement(new Settable[]{field}, expression, locate(ctx))
                    } else if (first instanceof Settable) {
                        Settable expr = (Settable) first
                        return new AssignmentStatement(new Settable[]{expr}, expression, locate(ctx))
                    } else {
                        throw new RuntimeException("Variable target is not found for '" + first.class.name + "' at:\n" + getTextFor(ctx))
                    }
                }

                name = targetContext.NAME().text

                PythonParser.Star_expressionsContext starExpressionsContext = ctx.star_expressions()
                if (starExpressionsContext == null) {
                    throw new RuntimeException("Value for variable assignment wasn't found.")
                }

                List<PythonParser.Star_expressionContext> starExpressionContexts = starExpressionsContext.star_expression()
                PythonParser.ExpressionContext expressionContext = getExpressionContext(starExpressionContexts)

                PyExpression visit1 = visitExpression(expressionContext)
                if (visit1 == null) {
                    throw new RuntimeException("Expression for variable assignment wasn't found.")
                }

                if (definingFunction == null) {
                    if (definingClass == null) {
                        if (definingModule == null) throw new IllegalStateException("Unknown module!" + getTextFor(ctx))
                        Type type = Type.getType(Object.class)
                        @Nullable String finalName = name
                        JvmClass require = classCache.require(this, type)
                        PyFunction getterFunc = PyFunction.withContent(definingModule, getterName(name), new PyParameter[0], require, true, locate(ctx), getterMethod -> {
                            if (definingModule != null) {
                                writer.loadClass(definingModule)
                            } else {
                                throw new RuntimeException("Not in root class")
                            }
                            writer.dynamicGetAttr(finalName)
                            writer.returnValue(type, locate(ctx))
                        })

                        definingModule.addFunction(getterFunc)

                        PyFunction setterFunc = PyFunction.withContent(definingModule, setterName(name), new PyParameter[]{new PyTypedParameter("value", classCache.object(this), locate(ctx))}, classCache.void_(this), true, locate(ctx), setterMethod -> {
                            if (definingModule != null) {
                                writer.loadClass(definingModule)
                            } else {
                                throw new RuntimeException("Not in root class")
                            }
                            writer.loadValue(0, type)
                            writer.cast(Type.getType(Object.class))
                            writer.dynamicSetAttr(finalName)
                            writer.returnVoid()
                        })

                        definingModule.addFunction(setterFunc)
                        return new AssignmentStatement(new Settable[]{new MemberAttrExpr(definingModule, finalName, locate(ctx))}, visit1, locate(ctx))
                    } else {
                        throw new RuntimeException("Not in root class")
                    }
                } else if (hasSymbol(name)) {
                    PySymbol symbol = getSymbol(name)
                    if (symbol instanceof JvmClass) {
                        throw new TODO()
                    } else if (symbol instanceof Settable) {
                        Settable settable = (Settable) symbol
                        return new AssignmentStatement(new Settable[]{settable}, visit1, locate(ctx))
                    } else {
                        throw new IllegalStateException("Unknown symbol type! Got: " + symbol.class.name)
                    }
                } else {
                    createVariable(name, visit1, locate(ctx))
                }
            }

            PythonParser.AugassignContext augassign = ctx.augassign()
            if (augassign != null) {
                throw new RuntimeException("augassign not supported for:\n" + getTextFor(ctx))
            }
            PythonParser.Star_expressionsContext starExpressionsContext = ctx.star_expressions()
            List<StarExpression> starExpressions = null
            if (starExpressionsContext != null) {
                starExpressions = visitStar_expressions(starExpressionsContext)
            }
            PythonParser.ExpressionContext expressionContext = ctx.expression()
            PythonParser.Annotated_rhsContext value = ctx.annotated_rhs()

            if (definingFunction == null) {
                if (definingClass == null) {
                    if (definingModule == null) throw new IllegalStateException("Unknown module!" + getTextFor(ctx))


                    flags.set(F_CPL_TYPE_ANNO)
                    PyExpression visit1 = null
                    try {
                        visit1 = visitExpression(ctx.expression())
                    } finally {
                        flags.clear(F_CPL_TYPE_ANNO)
                    }
                    PyExpression visit = visitAnnotated_rhs(value)
                    ClassReference reference = new ClassReference((SymbolReferenceExpr) visit1)
                    @Nullable String finalName = name
                    PyFunction getterFunc = PyFunction.withContent(definingModule, getterName(name), new PyParameter[0], reference, true, locate(ctx), getterMethod -> {
                        if (definingModule != null) {
                            writer.loadClass(definingModule)
                        } else {
                            throw new RuntimeException("Not in root class")
                        }
                        writer.dynamicGetAttr(finalName)
                        writer.returnValue(reference.type, locate(ctx))
                    })

                    definingModule.addFunction(getterFunc)

                    PyFunction setterFunc = PyFunction.withContent(definingModule, setterName(name), new PyParameter[]{new PyTypedParameter("value", reference, locate(ctx))}, classCache.void_(this), true, locate(ctx), setterMethod -> {
                        if (definingModule != null) {
                            writer.loadClass(definingModule)
                        } else {
                            throw new RuntimeException("Not in root class")
                        }
                        writer.loadValue(0, reference.type)
                        writer.cast(Type.getType(Object.class))
                        writer.dynamicSetAttr(finalName)
                        writer.returnVoid()
                    })

                    definingModule.addFunction(setterFunc)
                    return new AssignmentStatement(new Settable[]{new MemberAttrExpr(definingModule, finalName, locate(ctx))}, visit, locate(ctx))
                } else {
                    throw new RuntimeException("Not in root class")
                }
            }
            if (expressionContext != null) {
                if (value == null) {
                    flags.set(F_CPL_TYPE_ANNO)
                    PyExpression visit1 = null
                    try {
                        visit1 = visitExpression(ctx.expression())
                    } finally {
                        flags.clear(F_CPL_TYPE_ANNO)
                    }

                    if (methodOut == null) {
                        Type type = visit1.type
                        if (definingModule == null) throw new RuntimeException("Not in a module?" + locate(ctx))
                        definingModule.addProperty(this, name, type)
                        ModuleRootField symbol = new ModuleRootField(definingModule, name, locate(ctx))
                        setSymbol(name, symbol)
                        return new AssignmentStatement(new Settable[]{symbol}, visit1, locate(ctx))
                    } else {
                        return createVariable(name, starExpressions.first.expression(), locate(ctx))
                    }
                }

                System.out.println("annotatedRhsContext = " + value.text)
                PyExpression expression = visitAnnotated_rhs(value)
                flags.set(F_CPL_TYPE_ANNO)
                PyExpression visit1 = null
                try {
                    visit1 = visitExpression(ctx.expression())
                } finally {
                    flags.clear(F_CPL_TYPE_ANNO)
                }

                if (methodOut == null) {
                    Type type = visit1.type
                    if (definingModule == null) throw new RuntimeException("Not in a module?" + locate(ctx))
                    definingModule.addProperty(this, name, type)
                    ModuleRootField symbol = new ModuleRootField(definingModule, name, locate(ctx))
                    setSymbol(name, symbol)
                    return new AssignmentStatement(new Settable[]{symbol}, visit1, locate(ctx))
                } else {
                    return createVariable(name, expression, locate(ctx))
                }
            } else {
                if (definingFunction.name == "<init>" || definingFunction.name == "__init__") {
                    if (definingModule == null) throw new RuntimeException("Not in a module?" + locate(ctx))
                    throw new CompilerException("Annotated RHS is required " + locate(ctx))
                } else {
                    if (starExpressions == null) {
                        throw new CompilerException("Annotated RHS is required ", locate(ctx))
                    }

                    List<PythonParser.Star_targetsContext> starTargetsContexts = ctx.star_targets()
                    if (starTargetsContexts == null) {
                        throw new CompilerException("Targets are required ", locate(ctx))
                    }
                    if (starTargetsContexts.size() != 1) {
                        throw new CompilerException("Not supported", locate(ctx))
                    }

                    List<PyExpression> first = visitStar_targets(starTargetsContexts.first)

                    return new AssignmentStatement(first.toArray(Settable[]::new), starExpressions.first.expression(), locate(ctx))
                }
            }
        } catch (CompilerException e) {
            throw e
        } finally {
            flags.clear(F_CPL_ASSIGN)
        }
    }

    private static PythonParser.ExpressionContext getExpressionContext(List<PythonParser.Star_expressionContext> starExpressionContexts) {
        if (starExpressionContexts.size() != 1) {
            // TODO Add support for multiple star expressions
            throw new RuntimeException("Expected 1 expression, got " + starExpressionContexts.size() + " which is unsupported for now.")
        }

        PythonParser.Star_expressionContext starExpressionContext = starExpressionContexts.first
        TerminalNode star = starExpressionContext.STAR()
        if (star != null) {
            // TODO Add support for star expressions
            throw new RuntimeException("Star expressions are not supported for now.")
        }

        PythonParser.ExpressionContext expressionContext = starExpressionContext.expression()
        if (expressionContext == null) {
            throw new RuntimeException("Expression for variable assignment wasn't found.")
        }
        return expressionContext
    }

    @Override
    PyExpression visitAnnotated_rhs(PythonParser.Annotated_rhsContext ctx) {
        PythonParser.Yield_exprContext yieldExprContext = ctx.yield_expr()
        if (yieldExprContext != null) {
            throw new CompilerException("Generators (yield) are not supported yet!", locate(yieldExprContext))
        }

        List<StarExpression> o = visitStar_expressions(ctx.star_expressions())
        if (o.size() > 1)
            throw new CompilerException("Too many expressions for annotation!", locate(ctx.star_expressions()))
        if (o.empty)
            throw new CompilerException("Expected an annotation", locate(ctx.star_expressions()))
        StarExpression first = o.first
        if (first.expression() instanceof PyExpression) {
            PyExpression expression = first.expression()
            return expression
        }
        throw new CompilerException("Expected a class as annotation", first.location)
    }

    void swapMethod(MethodNode methodNode, Closure func) {
        def oldMethodOut = methodOut
        methodOut = methodNode
        func()
        methodOut = oldMethodOut
    }

    void swapClass(ClassNode classNode, Closure func) {
        def oldClassOut = classOut
        classOut = classNode
        func()
        classOut = oldClassOut
    }

    private static String getterName(@Nullable String name) {
        return "get" + CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name)
    }

    private static String setterName(@Nullable String name) {
        return "set" + CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name)
    }

    @Override
    List<PyExpression> visitStar_targets(PythonParser.Star_targetsContext ctx) {
        if (ctx.star_target().size() != 1) {
            throw new TODO()
        }
        return List.of(visitStar_target(ctx.star_target(0)))
    }

    @Override
    PyExpression visitStar_target(PythonParser.Star_targetContext ctx) {
        PythonParser.Star_targetContext starTargetContext = ctx.star_target()
        if (starTargetContext != null) {
            throw new IllegalStateException("DEBUG")
        }

        TerminalNode star = ctx.STAR()
        if (star != null) {
            throw new IllegalStateException("DEBUG")
        }

        return visitTarget_with_star_atom(ctx.target_with_star_atom())
    }

    @Override
    PyExpression visitTarget_with_star_atom(PythonParser.Target_with_star_atomContext ctx) {
        if (ctx.LSQB() != null)
            throw new IllegalStateException("DEBUG")
        if (ctx.RSQB() != null)
            throw new IllegalStateException("DEBUG")
        if (ctx.star_atom() != null) {
            Object o = visitStar_atom(ctx.star_atom())
            if (o instanceof Settable && (Settable) o instanceof PyExpression) {
                Settable settable = (Settable) o
                PyExpression expression = (PyExpression) settable
                return expression
            } else if (o instanceof PyNameReference && definingFunction != null) {
                PySymbol symbolToSet = getSymbolToSet(o.name)
                if (symbolToSet != null) {
                    if (symbolToSet instanceof Settable && (Settable) symbolToSet instanceof PyExpression) {
                        Settable settable = (Settable) symbolToSet
                        PyExpression expression = (PyExpression) settable
                        return expression
                    }
                }
                return definingFunction.defineVariable(o.name, o.location)
            }

            throw new IllegalStateException("DEBUG")
        }

        PythonParser.T_primaryContext tPrimaryContext = ctx.t_primary()
        TerminalNode name = ctx.NAME()
        TerminalNode dot = ctx.DOT()
        if (tPrimaryContext == null) throw new IllegalStateException("DEBUG")
        if (dot == null) throw new IllegalStateException("DEBUG")
        if (name == null) throw new IllegalStateException("DEBUG")

        Object visit = visit(tPrimaryContext)
        if (visit instanceof JvmClass) {
            JvmClass jvmClass = (JvmClass) visit
            String name1 = name.text
            return new MemberAttrExpr(jvmClass, name1, locate(tPrimaryContext))
        }
        if (visit instanceof SelfExpr) {
            SelfExpr self = (SelfExpr) visit
            self.typeClass()
            MemberAttrExpr attr = self.attr(name.text, locate(tPrimaryContext))
            if (attr == null) {
                return new MemberAttrExpr(self, name.text, locate(tPrimaryContext))
            }
            return attr
        } else if (visit instanceof PyNameReference && definingFunction != null) {
            if (definingInstance != null && visit.name == "self") {
                return new MemberAttrExpr(new SelfExpr(definingInstance.type, visit.location), name.text, locate(ctx))
            }
            if (!definingFunction.static && visit.name == "self") {
                return new MemberAttrExpr(new SelfExpr(definingFunction.owner().getType(), visit.location), name.text, locate(name))
            }
            PySymbol symbolToSet = getSymbolToSet(visit.name)
            if (symbolToSet != null) {
                if (symbolToSet instanceof Settable && (Settable) symbolToSet instanceof PyExpression) {
                    Settable settable = (Settable) symbolToSet
                    PyExpression expression = (PyExpression) settable
                    return expression
                } else throw new RuntimeException("E")
            }
            return definingFunction.defineVariable(visit.name, visit.location)
        } else if (visit instanceof PyNameReference) {
            throw new IllegalStateException("DEBUG")
        }

        throw new IllegalStateException("DEBUG")
    }

    static PySymbol getSymbolToSet(String name) {
        return SymbolContext.current().getSymbolToSet(name)
    }

    private String importType(Type type) {
        if (type == null) throw new RuntimeException("Can't import null owner")
        if (type.sort == Type.ARRAY) {
            importType(type.elementType)
            return null
        }
        if (type.sort != Type.OBJECT) return null
        try {
            Class<?> javaType = Class.forName(type.className, false, getClass().classLoader)
            String simpleName = getSimpleName(type)
            JavaClass value = new JavaClass(type.className, javaType, new Location())
            imports.add(simpleName, value)
            return simpleName
        } catch (ClassNotFoundException ignored) {
            PyClass value = classes.byClassName(type.className)
            if (value == null) throw new CompilerException("JVM Class not found: " + type.className)
            String simpleName = getSimpleName(value.type)
            imports.add(simpleName, value)
            return simpleName
        }
    }

    private static String getSimpleName(Type type) {
        String[] split = type.internalName.split("/")
        return split[split.length - 1]
    }

    @Override
    Object visitStar_atom(PythonParser.Star_atomContext ctx) {
        TerminalNode name = ctx.NAME()
        TerminalNode lpar = ctx.LPAR()
        TerminalNode lsqb = ctx.LSQB()
        if (lpar != null)
            throw new IllegalStateException("DEBUG")
        if (lsqb != null)
            throw new IllegalStateException("DEBUG")
        if (name != null) {
            PythonParser.Target_with_star_atomContext targetWithStarAtomContext = ctx.target_with_star_atom()
            if (targetWithStarAtomContext != null) {
                visitTarget_with_star_atom(targetWithStarAtomContext)
                throw new IllegalStateException("DEBUG")
            }
            return new PyNameReference(name.text, locate(ctx.NAME()))
        }

        throw new IllegalStateException("DEBUG")
    }

    @Override
    List<StarExpression> visitStar_expressions(PythonParser.Star_expressionsContext ctx) {
        if (ctx.star_expression().size() == 1) {
            return List.of(visitStar_expression(ctx.star_expression(0)))
        } else {
            throw new RuntimeException("star_expressions not supported for:\n" + getTextFor(ctx))
        }
    }

    @Override
    StarExpression visitStar_expression(PythonParser.Star_expressionContext ctx) {
        PythonParser.Bitwise_orContext bitwiseOrContext = ctx.bitwise_or()
        if (ctx.STAR() != null) {
            throw new CompilerException("STAR not supported yet!", locate(ctx.STAR()))
        }
        if (bitwiseOrContext != null) {
            return new StarExpression(visitBitwise_or(bitwiseOrContext), ctx.STAR() != null, locate(ctx))
        }
        PythonParser.ExpressionContext expression = ctx.expression()
        if (expression != null) {
            return new StarExpression(visitExpression(expression), ctx.STAR() != null, locate(ctx))
        }
        throw new RuntimeException("No supported matching star_expression found for:\n" + getTextFor(ctx))
    }

    @Override
    AssignmentExpr visitAssignment_expression(PythonParser.Assignment_expressionContext ctx) {
        return (AssignmentExpr) super.visitAssignment_expression(ctx)
    }

    @Override
    PyExpression visitBitwise_or(PythonParser.Bitwise_orContext ctx) {
        PythonParser.Bitwise_orContext otherContext = ctx.bitwise_or()

        PyExpression value
        PyExpression addition = null
        if (otherContext != null) {
            value = visitBitwise_or(otherContext)
            addition = visitBitwise_xor(ctx.bitwise_xor())
        } else {
            value = visitBitwise_xor(ctx.bitwise_xor())
        }
        PyExpression finalValue = value
        PyExpression finalAddition = addition
        if (shouldNotCreateEval()) {
            if (addition != null) {
                throw new RuntimeException("Binary operator is not allowed in owner annotations (at " + fileName + ":" + ctx.start.line + ":" + ctx.start.charPositionInLine + ")")
            }

            if (value != null) {
                return value
            }
        }
        PyEval.Operator operator = null
        if (ctx.VBAR() != null) {
            operator = PyEval.Operator.OR
        }
        return new PyEval(operator, finalValue, finalAddition, locate(ctx))
    }

    private boolean shouldNotCreateEval() {
        return flags.get(F_CPL_TYPE_ANNO) || flags.get(F_CPL_CLASS_INHERITANCE)
    }

    @Override
    PyExpression visitBitwise_xor(PythonParser.Bitwise_xorContext ctx) {
        PythonParser.Bitwise_xorContext otherContext = ctx.bitwise_xor()

        PyExpression value
        PyExpression addition = null
        if (otherContext != null) {
            value = visitBitwise_xor(otherContext)
            addition = visitBitwise_and(ctx.bitwise_and())
        } else {
            value = visitBitwise_and(ctx.bitwise_and())
        }
        PyExpression finalValue = value
        PyExpression finalAddition = addition
        PyEval.Operator operator = null
        if (shouldNotCreateEval()) {
            if (addition != null) {
                throw new RuntimeException("Binary operator is not allowed in owner annotations (at " + fileName + ":" + ctx.start.line + ":" + ctx.start.charPositionInLine + ")")
            }

            if (value != null) {
                return value
            }
        }
        if (ctx.CIRCUMFLEX() != null) {
            operator = PyEval.Operator.XOR
        }
        return new PyEval(operator, finalValue, finalAddition, locate(ctx))
    }

    @Override
    PyExpression visitConjunction(PythonParser.ConjunctionContext ctx) {
        List<PythonParser.InversionContext> inversionContext = ctx.inversion()
        if (!ctx.AND().empty) {
            throw new RuntimeException("conjunction not supported for:\n" + getTextFor(ctx))
        }
        if (inversionContext.size() == 1) {
            return visitInversion(inversionContext.first)
        }
        throw new RuntimeException("No supported matching conjunction found for:\n" + getTextFor(ctx))
    }

    @Override
    PyExpression visitDisjunction(PythonParser.DisjunctionContext ctx) {
        List<PythonParser.ConjunctionContext> conjunctionContext = ctx.conjunction()
        if (!ctx.OR().empty) {
            throw new RuntimeException("disjunction not supported for:\n" + getTextFor(ctx))
        }
        if (conjunctionContext.size() == 1) {
            return visitConjunction(conjunctionContext.first)
        }
        throw new RuntimeException("No supported matching disjunction found for:\n" + getTextFor(ctx))
    }

    @Override
    PyExpression visitExpression(PythonParser.ExpressionContext ctx) {
        List<PythonParser.DisjunctionContext> disjunction = ctx.disjunction()
        if (disjunction.size() == 1) {
            return visitDisjunction(ctx.disjunction(0))
        }

        throw new RuntimeException("No supported matching expression found for:\n" + getTextFor(ctx))
    }

    @Override
    PyExpression visitInversion(PythonParser.InversionContext ctx) {
        ctx.inversion()
        if (ctx.NOT() != null) {
            throw new RuntimeException("inversion not supported for:\n" + getTextFor(ctx))
        }
        PythonParser.ComparisonContext comparison = ctx.comparison()
        if (comparison != null) {
            return visitComparison(comparison)
        }
        throw new RuntimeException("No supported matching inversion found for:\n" + getTextFor(ctx))
    }

    @Override
    PyExpression visitComparison(PythonParser.ComparisonContext ctx) {
        PythonParser.Bitwise_orContext bitwiseOrContext = ctx.bitwise_or()
        List<PythonParser.Compare_op_bitwise_or_pairContext> compareOpBitwiseOrPairContexts = ctx.compare_op_bitwise_or_pair()
        if (bitwiseOrContext != null) {
            PyExpression visit = visitBitwise_or(bitwiseOrContext)
            if (shouldNotCreateEval()) {
                if (!compareOpBitwiseOrPairContexts.empty) {
                    throw new RuntimeException("Comparison is not allowed in owner annotations (at " + fileName + ":" + ctx.start.line + ":" + ctx.start.charPositionInLine + ")")
                }

                if (visit != null) {
                    return visit
                }
            }
            if (!compareOpBitwiseOrPairContexts.empty) {
                for (PythonParser.Compare_op_bitwise_or_pairContext compareOpBitwiseOrPairContext : compareOpBitwiseOrPairContexts) {
                    var oldLeftExpr = leftExpr
                    leftExpr = visit
                    Object visit1 = this.visit(compareOpBitwiseOrPairContext)
                    leftExpr = oldLeftExpr
                    if (visit1 instanceof PyEval) {
                        PyEval comparison = (PyEval) visit1
                        return comparison
                    } else {
                        throw new RuntimeException("compare_op_bitwise_or_pair not supported for '" + visit1.class.name + "' at:\n" + getTextFor(ctx))
                    }
                }
            }
            return visit
        }

        throw new RuntimeException("No supported matching comparison found for:\n" + getTextFor(ctx))
    }

    @Override
    Object visitCompare_op_bitwise_or_pair(PythonParser.Compare_op_bitwise_or_pairContext ctx) {
        if (shouldNotCreateEval()) {
            if (ctx.eq_bitwise_or() != null) {
                throw new RuntimeException("Equality is not allowed in owner annotations (at " + fileName + ":" + ctx.start.line + ":" + ctx.start.charPositionInLine + ")")
            }
            if (ctx.noteq_bitwise_or() != null) {
                throw new RuntimeException("Inequality is not allowed in owner annotations (at " + fileName + ":" + ctx.start.line + ":" + ctx.start.charPositionInLine + ")")
            }
            if (ctx.gt_bitwise_or() != null) {
                throw new RuntimeException("Relational operator is not allowed in owner annotations (at " + fileName + ":" + ctx.start.line + ":" + ctx.start.charPositionInLine + ")")
            }
            if (ctx.gte_bitwise_or() != null) {
                throw new RuntimeException("Relational operator is not allowed in owner annotations (at " + fileName + ":" + ctx.start.line + ":" + ctx.start.charPositionInLine + ")")
            }
            if (ctx.lt_bitwise_or() != null) {
                throw new RuntimeException("Relational operator is not allowed in owner annotations (at " + fileName + ":" + ctx.start.line + ":" + ctx.start.charPositionInLine + ")")
            }
            if (ctx.lte_bitwise_or() != null) {
                throw new RuntimeException("Relational operator is not allowed in owner annotations (at " + fileName + ":" + ctx.start.line + ":" + ctx.start.charPositionInLine + ")")
            }

            throw new RuntimeException("No supported matching compare_op_bitwise_or_pair found for:\n" + getTextFor(ctx))
        }
        PythonParser.Eq_bitwise_orContext eqBitwiseOrContext = ctx.eq_bitwise_or()
        if (eqBitwiseOrContext != null) {
            return new PyEval(PyEval.Operator.EQ, leftExpr, visitBitwise_or(eqBitwiseOrContext.bitwise_or()), locate(eqBitwiseOrContext))
        }
        PythonParser.Noteq_bitwise_orContext noteqBitwiseOrContext = ctx.noteq_bitwise_or()
        if (noteqBitwiseOrContext != null) {
            return new PyEval(PyEval.Operator.NE, leftExpr, visitBitwise_or(noteqBitwiseOrContext.bitwise_or()), locate(noteqBitwiseOrContext))
        }
        PythonParser.Gt_bitwise_orContext gtBitwiseOrContext = ctx.gt_bitwise_or()
        if (gtBitwiseOrContext != null) {
            return new PyEval(PyEval.Operator.GT, leftExpr, visitBitwise_or(gtBitwiseOrContext.bitwise_or()), locate(gtBitwiseOrContext))
        }
        PythonParser.Gte_bitwise_orContext gteBitwiseOrContext = ctx.gte_bitwise_or()
        if (gteBitwiseOrContext != null) {
            return new PyEval(PyEval.Operator.GE, leftExpr, visitBitwise_or(gteBitwiseOrContext.bitwise_or()), locate(gteBitwiseOrContext))
        }
        PythonParser.Lt_bitwise_orContext ltBitwiseOrContext = ctx.lt_bitwise_or()
        if (ltBitwiseOrContext != null) {
            return new PyEval(PyEval.Operator.LT, leftExpr, visitBitwise_or(ltBitwiseOrContext.bitwise_or()), locate(ltBitwiseOrContext))
        }
        PythonParser.Lte_bitwise_orContext lteBitwiseOrContext = ctx.lte_bitwise_or()
        if (lteBitwiseOrContext != null) {
            return new PyEval(PyEval.Operator.LE, leftExpr, visitBitwise_or(lteBitwiseOrContext.bitwise_or()), locate(lteBitwiseOrContext))
        }
        PythonParser.Notin_bitwise_orContext notinBitwiseOrContext = ctx.notin_bitwise_or()
        if (notinBitwiseOrContext != null) {
            return new PyEval(PyEval.Operator.NOT_IN, leftExpr, visitBitwise_or(notinBitwiseOrContext.bitwise_or()), locate(notinBitwiseOrContext))
        }
        PythonParser.In_bitwise_orContext inBitwiseOrContext = ctx.in_bitwise_or()
        if (inBitwiseOrContext != null) {
            return new PyEval(PyEval.Operator.IN, leftExpr, visitBitwise_or(inBitwiseOrContext.bitwise_or()), locate(inBitwiseOrContext))
        }
        PythonParser.Is_bitwise_orContext bitwiseOr = ctx.is_bitwise_or()
        if (bitwiseOr != null) {
            return new PyEval(PyEval.Operator.IS, leftExpr, visitBitwise_or(bitwiseOr.bitwise_or()), locate(bitwiseOr))
        }
        PythonParser.Isnot_bitwise_orContext isnotBitwiseOrContext = ctx.isnot_bitwise_or()
        if (isnotBitwiseOrContext != null) {
            return new PyEval(PyEval.Operator.IS_NOT, leftExpr, visitBitwise_or(isnotBitwiseOrContext.bitwise_or()), locate(isnotBitwiseOrContext))
        }
        throw new RuntimeException("No supported matching compare_op_bitwise_or_pair found for:\n" + getTextFor(ctx))
    }

    @Override
    PyExpression visitBitwise_and(PythonParser.Bitwise_andContext ctx) {
        PythonParser.Bitwise_andContext otherContext = ctx.bitwise_and()

        PyExpression value
        PyExpression addition = null
        if (otherContext != null) {
            value = visitBitwise_and(otherContext)
            addition = visitShift_expr(ctx.shift_expr())
        } else {
            value = visitShift_expr(ctx.shift_expr())
        }
        PyExpression finalValue = value
        PyExpression finalAddition = addition
        PyEval.Operator operator = null
        if (shouldNotCreateEval()) {
            if (addition != null) {
                throw new RuntimeException("Type annotation is not allowed in owner annotations (at " + fileName + ":" + ctx.start.line + ":" + ctx.start.charPositionInLine + ")")
            }

            if (value != null) {
                return value
            }
        }
        if (ctx.AMPER() != null) {
            operator = PyEval.Operator.AND
        }
        return new PyEval(operator, finalValue, finalAddition, locate(ctx))
    }

    @Override
    PyExpression visitShift_expr(PythonParser.Shift_exprContext ctx) {
        PythonParser.Shift_exprContext otherContext = ctx.shift_expr()

        PyExpression value
        PyExpression addition = null
        if (otherContext != null) {
            value = visitShift_expr(otherContext)
            addition = visitSum(ctx.sum())
        } else {
            value = visitSum(ctx.sum())
        }
        PyExpression finalValue = value
        PyExpression finalAddition = addition
        PyEval.Operator operator = null
        if (shouldNotCreateEval()) {
            if (addition != null) {
                throw new RuntimeException("Type annotation is not allowed in owner annotations (at " + fileName + ":" + ctx.start.line + ":" + ctx.start.charPositionInLine + ")")
            }

            if (value != null) {
                return value
            }
        }
        if (ctx.LEFTSHIFT() != null) {
            operator = PyEval.Operator.LSHIFT
        } else if (ctx.RIGHTSHIFT() != null) {
            operator = PyEval.Operator.RSHIFT
        }
        return new PyEval(operator, finalValue, finalAddition, locate(ctx))
    }

    @Override
    PyExpression visitSum(PythonParser.SumContext ctx) {
        PythonParser.SumContext otherContext = ctx.sum()

        PyExpression value
        PyExpression addition = null
        if (otherContext != null) {
            value = visitSum(otherContext)
            addition = visitTerm(ctx.term())
        } else {
            value = visitTerm(ctx.term())
        }
        PyExpression finalValue = value
        PyExpression finalAddition = addition
        PyEval.Operator operator = null
        if (shouldNotCreateEval()) {
            if (addition != null) {
                throw new RuntimeException("Type annotation is not allowed in owner annotations (at " + fileName + ":" + ctx.start.line + ":" + ctx.start.charPositionInLine + ")")
            }

            if (value != null) {
                return value
            }
        }
        if (ctx.PLUS() != null) {
            operator = PyEval.Operator.ADD
        } else if (ctx.MINUS() != null) {
            operator = PyEval.Operator.SUB
        }
        if (operator != null) {
            return new PyEval(operator, finalValue, finalAddition, locate(ctx))
        }
        return value
    }

    @Override
    PyExpression visitTerm(PythonParser.TermContext ctx) {
        PythonParser.TermContext otherContext = ctx.term()

        PyExpression value
        PyExpression addition = null
        if (otherContext != null) {
            value = visitTerm(otherContext)
            addition = visitFactor(ctx.factor())
        } else {
            value = visitFactor(ctx.factor())
        }
        PyExpression finalValue = value
        PyExpression finalAddition = addition
        PyEval.Operator operator = null
        if (shouldNotCreateEval()) {
            if (addition != null) {
                throw new RuntimeException("Type annotation is not allowed in owner annotations (at " + fileName + ":" + ctx.start.line + ":" + ctx.start.charPositionInLine + ")")
            }

            if (value != null) {
                return value
            }
        }
        if (ctx.STAR() != null) {
            operator = PyEval.Operator.MUL
        } else if (ctx.SLASH() != null) {
            operator = PyEval.Operator.DIV
        } else if (ctx.PERCENT() != null) {
            operator = PyEval.Operator.MOD
        } else if (ctx.DOUBLESLASH() != null) {
            operator = PyEval.Operator.FLOORDIV
        }
        return new PyEval(operator, finalValue, finalAddition, locate(ctx))
    }

    @Override
    PyExpression visitFactor(PythonParser.FactorContext ctx) {
        ctx.factor()
        if (ctx.MINUS() != null) {
            PythonParser.FactorContext otherContext = ctx.factor()

            PyExpression value
            if (otherContext != null) {
                value = visitFactor(otherContext)
            } else {
                value = visitPower(ctx.power())
            }
            PyExpression finalValue = value
            PyEval.Operator operator = PyEval.Operator.UNARY_MINUS
            if (shouldNotCreateEval()) {
                if (value != null) {
                    return value
                }
                throw new RuntimeException("Unary operator is not allowed in owner annotations (at " + fileName + ":" + ctx.start.line + ":" + ctx.start.charPositionInLine + ")")
            }
            return new PyEval(operator, finalValue, null, locate(ctx))
        }

        if (ctx.PLUS() != null) {
            PythonParser.FactorContext otherContext = ctx.factor()

            PyExpression value
            if (otherContext != null) {
                value = visitFactor(otherContext)
            } else {
                value = visitPower(ctx.power())
            }
            PyExpression finalValue = value
            PyEval.Operator operator = PyEval.Operator.UNARY_PLUS
            if (shouldNotCreateEval()) {
                if (value != null) {
                    return value
                }
                throw new RuntimeException("Unary operator is not allowed in owner annotations (at " + fileName + ":" + ctx.start.line + ":" + ctx.start.charPositionInLine + ")")
            }
            return new PyEval(operator, finalValue, null, locate(ctx))
        }

        if (ctx.TILDE() != null) {
            PythonParser.FactorContext otherContext = ctx.factor()

            PyExpression value
            if (otherContext != null) {
                value = visitFactor(otherContext)
            } else {
                value = visitPower(ctx.power())
            }
            PyExpression finalValue = value
            PyEval.Operator operator = PyEval.Operator.UNARY_NOT
            if (shouldNotCreateEval()) {
                if (value != null) {
                    return value
                }
                throw new RuntimeException("Unary operator is not allowed in owner annotations (at " + fileName + ":" + ctx.start.line + ":" + ctx.start.charPositionInLine + ")")
            }
            return new PyEval(operator, finalValue, null, locate(ctx))
        }
        PythonParser.PowerContext powerContext = ctx.power()
        if (powerContext != null) {
            return visitPower(powerContext)
        }
        throw new RuntimeException("No supported matching factor found for:\n" + getTextFor(ctx))
    }

    @Override
    PyExpression visitPower(PythonParser.PowerContext ctx) {
        if (ctx.DOUBLESTAR() != null) {
            PythonParser.FactorContext otherContext = ctx.factor()

            PyExpression value
            PyExpression addition = null
            if (otherContext != null) {
                value = visitFactor(otherContext)
                addition = visitAwait_primary(ctx.await_primary())
            } else {
                value = visitAwait_primary(ctx.await_primary())
            }
            PyExpression finalValue = value
            PyExpression finalAddition = addition
            PyEval.Operator operator = PyEval.Operator.POW
            if (shouldNotCreateEval()) {
                if (addition != null) {
                    throw new RuntimeException("Type annotation is not allowed in owner annotations (at " + fileName + ":" + ctx.start.line + ":" + ctx.start.charPositionInLine + ")")
                }

                if (value != null) {
                    return value
                }
            }
            return new PyEval(operator, finalValue, finalAddition, locate(ctx))

        }
        PythonParser.FactorContext factorContext = ctx.factor()
        if (factorContext != null) {
            return visitFactor(factorContext)
        }

        PythonParser.Await_primaryContext awaitPrimaryContext = ctx.await_primary()
        if (awaitPrimaryContext != null) {
            return visitAwait_primary(awaitPrimaryContext)
        }
        throw new RuntimeException("No supported matching power found for:\n" + getTextFor(ctx))
    }

    @Override
    PyExpression visitAwait_primary(PythonParser.Await_primaryContext ctx) {
        PythonParser.PrimaryContext primaryContext = ctx.primary()
        if (ctx.AWAIT() != null)
            throw new CompilerException("Async/await calls aren't supported yet!", locate(ctx))
        if (primaryContext != null) {
            return visitPrimary(primaryContext)
        }
        throw new RuntimeException("No supported matching await_primary found for:\n" + getTextFor(ctx))
    }

    @Override
    Object visitT_primary(PythonParser.T_primaryContext ctx) {
        PythonParser.AtomContext atom = ctx.atom()
        PythonParser.T_primaryContext tPrimaryContext = ctx.t_primary()
        if (tPrimaryContext != null) {
            throw new RuntimeException("t_primary not supported for:\n" + getTextFor(ctx))
        }
        PythonParser.ArgumentsContext arguments = ctx.arguments()
        if (arguments != null) {
            throw new RuntimeException("t_primary not supported for:\n" + getTextFor(ctx))
        }
        if (atom != null) {
            return visit(atom)
        }
        throw new RuntimeException("No supported matching t_primary found for:\n" + getTextFor(ctx))
    }

    @Override
    PyExpression visitPrimary(PythonParser.PrimaryContext ctx) {
        PythonParser.PrimaryContext primaryContext = ctx.primary()
        ctx.arguments()
        if (primaryContext != null) {
            if (primaryContext.LPAR() == null) {
                this.pushMemberContext(MemberContext.FIELD)
            }
            PyExpression parent = (PyExpression) visit(primaryContext)
            if (primaryContext.LPAR() == null) {
                this.popMemberContext()
            }
            if (ctx.DOT() != null && ctx.NAME() != null) {
                String name = ctx.NAME().text
                if (ctx.LPAR() != null) {
                    @SuppressWarnings("unchecked")
                    List<PyExpression> visit = (List<PyExpression>) visit(ctx.arguments())
                    return new MemberCallExpr(new MemberAttrExpr(parent, name, locate(ctx)), visit, locate(ctx))
                }
                return new MemberAttrExpr(parent, name, locate(ctx))
            }
            if (ctx.LPAR() != null) {
                PythonParser.ArgumentsContext arguments = ctx.arguments()
                @SuppressWarnings("unchecked")
                List<PyExpression> visit = arguments == null ? List.of() : visit(arguments) as List<PyExpression>
                return new MemberCallExpr(parent, visit, locate(ctx))
            }
            return parent
        }

        PythonParser.AtomContext atom = ctx.atom()
        if (atom != null) {
            PyAST visited = visitAtom(atom)
            if (Objects.requireNonNull(visited) instanceof PyExpression) {
                PyExpression pyExpression = (PyExpression) Objects.requireNonNull(visited)
                return pyExpression
            } else if (visited instanceof PyNameReference) {
                PyNameReference pyNameReference = (PyNameReference) visited
                if (ctx.LPAR() == null) {
                    return new SymbolReferenceExpr(pyNameReference.getName(), pyNameReference.location)
                } else {
                    throw new RuntimeException("Failed to find suitable expression for symbol: " + pyNameReference.getName() + " at:\n" + getTextFor(ctx))
                }
            }
            throw new RuntimeException("Expression for variable assignment didn't find a match.")
        }
        throw new RuntimeException("No supported matching primary found at:\n" + getTextFor(ctx))
    }

    Location locate(TerminalNode name) {
        return locate(name.symbol)
    }

    void pushMemberContext(MemberContext memberContext) {
        this.memberContextStack.push(memberContext)
    }

    void popMemberContext() {
        this.memberContextStack.pop()
        if (this.memberContextStack.empty) {
            return
        }
        this.memberContextStack.peek()
    }

    @Override
    PyAST visitAtom(PythonParser.AtomContext ctx) {
        PythonParser.DictContext dict = ctx.dict()
        if (dict != null) {
            throw new TODO()
        }
        PythonParser.ListContext list = ctx.list()
        if (list != null) {
            throw new TODO()
        }
        PythonParser.SetContext set = ctx.set()
        if (set != null) {
            throw new TODO()
        }
        TerminalNode ellipsis = ctx.ELLIPSIS()
        if (ellipsis != null) {
            throw new CompilerException("Ellipsis aren't supported as of now!", locate(ctx.ELLIPSIS()))
        }
        TerminalNode name = ctx.NAME()
        if (name != null) {
            return new PyNameReference(name.text, locate(ctx.NAME()))
        }
        PythonParser.StringsContext strings = ctx.strings()
        if (strings != null) {
            return (PyAST) visitStrings(strings)
        }

        TerminalNode number = ctx.NUMBER()
        if (number != null) {
            if (number.text.contains(".")) {
                try {
                    return new ConstantExpr(Double.parseDouble(number.text), locate(ctx.NUMBER()))
                } catch (NumberFormatException ignored) {
                    throw new CompilerException("Invalid floating-point number", locate(ctx.NUMBER()))
                }
            } else {
                try {
                    return new ConstantExpr(Long.parseLong(number.text), locate(ctx.NUMBER()))
                } catch (NumberFormatException ignored) {
                    throw new CompilerException("Invalid integer", locate(ctx.NUMBER()))
                }
            }
        }
        TerminalNode aTrue = ctx.TRUE()
        if (aTrue != null) {
            return new ConstantExpr(true, locate(ctx.TRUE()))
        }
        TerminalNode aFalse = ctx.FALSE()
        if (aFalse != null) {
            return new ConstantExpr(false, locate(ctx.FALSE()))
        }
        TerminalNode none = ctx.NONE()
        if (none != null) {
            return new ConstantExpr(NoneType.None, locate(ctx.NONE()))
        }
        throw new RuntimeException("No supported matching atom found for:\n" + getTextFor(ctx))
    }

    Location locate(Token symbol) {
        return new Location(rootDir.toAbsolutePath().resolve(pathOfFile).resolve(fileName + ".py").toString(), symbol.line, symbol.charPositionInLine, symbol.line, symbol.charPositionInLine + symbol.stopIndex - symbol.startIndex)
    }

    @Override
    def visitStrings(PythonParser.StringsContext ctx) {
        def fstring = ctx.fstring()
        if (fstring != null) {
            if (fstring.size() == 1) {
                return new FStringExpr(visitFstring(fstring.first), locate(ctx))
            }
        }
        def string = ctx.string()
        if (string != null) {
            if (string.size() == 1) {
                return new ConstantExpr(visitString(string.first), locate(string.first))
            }

            throw new RuntimeException("strings not supported for:\n" + getTextFor(ctx))
        }
        throw new RuntimeException("No supported matching strings found for:\n" + getTextFor(ctx))
    }

    @Override
    List<FStringElement> visitFstring(PythonParser.FstringContext ctx) {
        def middle = ctx.fstring_middle()
        return middle.collect { visitFstring_middle(it) }
    }

    @Override
    FStringElement visitFstring_middle(PythonParser.Fstring_middleContext ctx) {
        if (ctx.FSTRING_MIDDLE() != null) {
            if (ctx.fstring_replacement_field() != null) throw new TODO()

            return FStringElement.text(ctx.FSTRING_MIDDLE().toString(), locate(ctx))
        }

        return visitFstring_replacement_field(ctx.fstring_replacement_field())
    }

    @Override
    FStringElement.Replacement visitFstring_replacement_field(PythonParser.Fstring_replacement_fieldContext ctx) {
        return FStringElement.replacement(visitStar_expressions(ctx.star_expressions()), locate(ctx))
    }

    @Override
    def visitString(PythonParser.StringContext ctx) {
        TerminalNode STRING = ctx.STRING()
        if (STRING != null) {
            return STRING.text.substring(1, STRING.text.length() - 1)
        }
        throw new RuntimeException("No supported matching string found for:\n" + getTextFor(ctx))
    }

    @Override
    def visitImport_stmt(PythonParser.Import_stmtContext ctx) {
        PythonParser.Import_nameContext importNameContext = ctx.import_name()
        if (importNameContext != null) {
//            return visit(importNameContext);
            throw new RuntimeException("import_name not supported for:\n" + getTextFor(ctx))
        }
        if (ctx.import_from() != null) {
            return visitImport_from(ctx.import_from())
        }
        throw new RuntimeException("No supported matching import_stmt found for:\n" + getTextFor(ctx))
    }

    @Override
    PyFromImportStatement visitImport_from(PythonParser.Import_fromContext ctx) {
        PythonParser.Import_from_targetsContext importFromTargetsContext = ctx.import_from_targets()
        TerminalNode from = ctx.FROM()
        TerminalNode import_ = ctx.IMPORT()
        if (from == null || import_ == null) {
            throw new RuntimeException("No supported matching import_from found for:\n" + getTextFor(ctx))
        }

        if (importFromTargetsContext != null) {
            PythonParser.Dotted_nameContext dottedNameContext = ctx.dotted_name()
            System.out.println("dottedNameContext = " + dottedNameContext.text)
            System.out.println("importFromTargetsContext.getText() = " + importFromTargetsContext.text)

            PyFromImportStatement.Builder builder = PyFromImportStatement.builder(new ModulePath(dottedNameContext.text))

            List<Map.Entry<String, String>> visit = visitImport_from_targets(importFromTargetsContext)
            for (Map.Entry<String, String> s : visit) {
                visitDotted_name(dottedNameContext)

                if (s.key == "*") {
                    throw new TODO()
                }

                if (s.key == s.value) {
                    builder.name(s.value)
                } else {
                    builder.alias(s.key, s.value)
                }
            }

            return builder.build()
        }
        throw new RuntimeException("No supported matching import_from_targets found for:\n" + getTextFor(ctx))
    }

    @Override
    List<Map.Entry<String, String>> visitImport_from_targets(PythonParser.Import_from_targetsContext ctx) {
        PythonParser.Import_from_as_namesContext importFromAsNamesContext = ctx.import_from_as_names()
        if (importFromAsNamesContext != null) {
            return visitImport_from_as_names(importFromAsNamesContext)
        }
        throw new RuntimeException("No supported matching import_from_as_names found for:\n" + getTextFor(ctx))
    }

    @Override
    List<Map.Entry<String, String>> visitImport_from_as_names(PythonParser.Import_from_as_namesContext ctx) {
        List<Map.Entry<String, String>> importFromAsNames = new ArrayList<>()
        for (int i = 0; i < ctx.import_from_as_name().size(); i++) {
            importFromAsNames.add(visitImport_from_as_name(ctx.import_from_as_name(i)))
        }
        return importFromAsNames
    }

    @Override
    Map.Entry<String, String> visitImport_from_as_name(PythonParser.Import_from_as_nameContext ctx) {
        TerminalNode as_ = ctx.AS()
        if (as_ == null) {
            String text = ctx.NAME(0).text
            return new AbstractMap.SimpleEntry<>(text, text)
        }

        if (ctx.NAME().size() == 2) {
            return new AbstractMap.SimpleEntry<>(ctx.NAME(1).text, ctx.NAME(0).text)
        }

        throw new RuntimeException("No supported matching import_from_as_name found for:\n" + getTextFor(ctx))
    }

    @Override
    String visitDotted_name(PythonParser.Dotted_nameContext ctx) {
        return ctx.text
    }

    @Override
    Object visitFile_input(PythonParser.File_inputContext ctx) {
        classCache.init(this)

        PythonParser.StatementsContext statements = ctx.statements()

        ModuleContext context = ModuleContext.pushContext()

        if (imports == null) imports = new PyImports(this)
        for (PyBuiltinClass builtinClass : (builtins.classes)) {
            setSymbol(builtinClass.pyName, builtinClass)
        }

        for (PyBuiltinFunction builtinFunction : (builtins.functions)) {
            setSymbol(builtinFunction.name, builtinFunction)
        }

        definingModule = Module.create(new ModulePath((path + fileName).replace("/", ".")), locate(ctx))
        context.module(definingModule)
        JvmClassCompilable oldCompilingClass = compilingClass
        compilingClass = definingModule
        classCache.add(this, definingModule)

        if (statements == null) {
            return Unit.Instance
        }

        List<PyStatement> pyStatements = visitStatements(statements)
        for (PyStatement statement : pyStatements) {
            definingModule.addStatement(statement)
        }

        definingModule.writeClass(this, writer)

        context.popContext(this)

        while (context.popNeeded) {
            throw new CompilerException("Context still isn't empty after compiling module", writer.lastLocation())
        }

        if (methodOut != null) {
            throw new IllegalStateException("mv != null")
        }

        compilingClass = oldCompilingClass
        definingModule = null

        classOut = null
        return Unit.Instance
    }

    private String getName() {
        return path + CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, fileName) + "Py"
    }

    void compile(File file, File rootDir) throws IOException {
        CURRENT.set(this)
        String absolutePath = file.absolutePath
        String absolutePath1 = rootDir.absolutePath
        if (!absolutePath1.endsWith("/")) {
            absolutePath1 += "/"
        }
        if (!absolutePath.startsWith(absolutePath1)) {
            throw new RuntimeException("File is not in the root directory")
        }
        String path1 = absolutePath.replaceFirst(absolutePath1, "")
        PythonLexer lexer = new PythonLexer(CharStreams.fromPath(file.toPath().toAbsolutePath()))
        PythonParser parser = new PythonParser(new CommonTokenStream(lexer))
        PythonParser.File_inputContext fileInputContext = parser.file_input()
        var p = path1.substring(0, path1.length() - ".py".length())
        this.fileName = p.substring(p.lastIndexOf("/") + 1)
        this.path = p.substring(0, p.lastIndexOf("/") + 1)
        this.pathOfFile = Path.of(path)

        try {
            visit(fileInputContext)
        } catch (CompilerException e) {
            compileErrors.add(e)
        }
    }

    static void pack(String outputDir, String outputJar) {
        try (JarOutputStream jarOut = new JarOutputStream(new FileOutputStream(outputJar))) {
            Path sourcePath = Paths.get(outputDir)
            try (var walker = Files.walk(sourcePath)) {
                walker.filter(path -> !Files.isDirectory(path))
                        .forEach(path -> {
                            String entryName = sourcePath.relativize(path).toString().replace("\\", "/")
                            try {
                                jarOut.putNextEntry(new ZipEntry(entryName))
                                Files.copy(path, jarOut)
                                jarOut.closeEntry()
                            } catch (IOException e) {
                                throw new UncheckedIOException(e)
                            }
                        })
            }
        } catch (IOException e) {
            throw new RuntimeException(e)
        }
    }

    static void throwError(MethodVisitor mv, String value) {
        mv.visitTypeInsn(NEW, "java/lang/Error")
        mv.visitInsn(DUP)
        mv.visitLdcInsn(value)
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Error", "<init>", "(Ljava/lang/String;)V", false)
        mv.visitInsn(ATHROW)
    }

    AssignmentStatement createVariable(String name, PyExpression expr, Location location) {
        if (definingFunction == null) {
            throw new RuntimeException("Defining function is null")
        }

        VariableExpr variableExpr = definingFunction.defineVariable(name, location)
        SymbolContext.current().setSymbol(name, variableExpr)
        return new AssignmentStatement(new Settable[]{variableExpr}, expr, location)
    }

    Location locate(ParserRuleContext ctx) {
        return new Location(rootDir.toAbsolutePath().resolve(pathOfFile).resolve(fileName + ".py").toString(), ctx.start.line, ctx.start.charPositionInLine, ctx.stop.line, ctx.stop.charPositionInLine)
    }

    @Override
    List<PyExpression> visitArguments(PythonParser.ArgumentsContext ctx) {
        return visitArgs(ctx.args())
    }

    @Override
    List<PyExpression> visitArgs(PythonParser.ArgsContext ctx) {
        PythonParser.KwargsContext kwargs = ctx.kwargs()
        if (kwargs != null) {
            throw new CompilerException("Keyword arguments not supported yet!", locate(ctx))
        }

        List<PyExpression> args = new ArrayList<>()
        for (PythonParser.ExpressionContext expressionContext : ctx.expression()) {
            PyExpression visit = visitExpression(expressionContext)

            if (visit == null) {
                throw new RuntimeException("Unknown visitArgs for expression context: " + expressionContext.text)
            }


            args.add(visit)
        }

        return args
    }
}
