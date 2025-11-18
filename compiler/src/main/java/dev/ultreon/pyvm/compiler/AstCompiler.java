package dev.ultreon.pyvm.compiler;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.CheckClassAdapter;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;

import static org.objectweb.asm.Opcodes.*;

public class AstCompiler {
    private final AstParser.AstNode ast;
    private final ClassNode fileNode = new ClassNode();
    private final MethodNode fileInit = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
    private final MethodNode fileMain = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
    private final MethodNode fileClassInit = new MethodNode(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);

    private final Stack<MethodNode> methodStack = new Stack<>();
    private final Stack<ClassNode> classStack = new Stack<>();
    private final Stack<MethodNode> classInitStack = new Stack<>();
    private final Stack<MethodNode> initStack = new Stack<>();
    private final Stack<LoopContext> loopContextStack = new Stack<>();
    private final Stack<AstParser.AstNode> exprStack = new Stack<>();
    private final Stack<Integer> varIndexStack = new Stack<>();
    private final Stack<BitSet> varInUseStack = new Stack<>();
    private final Stack<Map<Integer, LabelNode>> varEndLabelStack = new Stack<>();
    private final Stack<Map<Integer, AbstractInsnNode>> varUseStack = new Stack<>();
    private Path outputDir;
    private int lambdaCount;
    private int curLineNo;

    private AstCompiler(AstParser.AstNode ast, String className) {
        this.ast = ast;

        this.fileNode.superName = "java/lang/Object";
        this.fileNode.version = Opcodes.V1_8;
        this.fileNode.access = Opcodes.ACC_PUBLIC;
        this.fileNode.name = className.replace('.', '/');
        this.fileNode.methods.add(this.fileInit);
        this.fileNode.methods.add(this.fileMain);
        this.fileNode.methods.add(this.fileClassInit);
        this.fileNode.sourceFile = "ast.json";

        fileInit.instructions.add(new TypeInsnNode(Opcodes.NEW, "python/_core/PyLocals"));
        fileInit.instructions.add(new InsnNode(Opcodes.DUP));
        fileInit.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "python/_core/PyLocals", "<init>", "()V", false));
        fileInit.instructions.add(new VarInsnNode(Opcodes.ASTORE, 1));

        fileClassInit.instructions.add(new TypeInsnNode(Opcodes.NEW, "python/_core/PyLocals"));
        fileClassInit.instructions.add(new InsnNode(Opcodes.DUP));
        fileClassInit.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "python/_core/PyLocals", "<init>", "()V", false));
        fileClassInit.instructions.add(new VarInsnNode(Opcodes.ASTORE, 0));
    }

    public void finish(Path outputDir) {
        this.fileInit.instructions.add(new InsnNode(Opcodes.RETURN));
        this.fileMain.instructions.add(new InsnNode(Opcodes.RETURN));
        this.fileClassInit.instructions.add(new InsnNode(Opcodes.RETURN));

        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        CheckClassAdapter checkClassAdapter = new CheckClassAdapter(classWriter);
        fileNode.accept(checkClassAdapter);

        Main.writeClassToFile(outputDir.resolve(this.fileNode.name + ".class"), this.fileNode);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("No output file specified");
            System.exit(2);
        }
        AstParser.AstNode parse = AstParser.parseFile(args[0]);
        AstCompiler compiler = new AstCompiler(parse, args[1]);


        compiler.outputDir = Path.of(args[2]);
        compiler.compile();

        compiler.finish(Path.of(args[2]));
    }

    private void compile() {
        this.emit(this.ast);
    }

    private Type typeOf(AstParser.AstNode returnType) {
        if (returnType == null) return Type.getType(Object.class);
        System.err.println("Warning: Unknown return type: " + returnType.getClass().getName());
        return Type.getObjectType("python/_core/VMObject");
    }

    private void addFunction(String name, AstParser.Arguments args, List<AstParser.AstNode> body, AstParser.AstNode returnType) {
        ClassNode classNode;
        if (classStack.isEmpty()) {
            classNode = this.fileNode;
        } else {
            classNode = classStack.peek();
        }
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, "!" + name, describe(args), null, null);
        pushMethod(method);
        method.instructions.add(new TypeInsnNode(Opcodes.NEW, "python/_core/PyLocals"));
        method.instructions.add(new InsnNode(Opcodes.DUP));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "python/_core/PyLocals", "<init>", "()V", false));
        asmStoreVar(Type.getArgumentCount(describe(args)) + 1);
        for (AstParser.AstNode node : body) {
            this.emit(node);
        }
        popMethod();
        if (classNode.methods == null) {
            classNode.methods = new ArrayList<>();
        }
        AbstractInsnNode lastInsn = method.instructions.getLast();
        if (lastInsn == null) {
            method.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
            method.instructions.add(new InsnNode(Opcodes.ARETURN));
        } else if (lastInsn.getOpcode() != Opcodes.ARETURN) {
            method.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
            method.instructions.add(new InsnNode(Opcodes.ARETURN));
        }
        System.out.println("Adding method " + name + " to class " + classNode.name);
        classNode.methods.add(method);

        StringBuilder accessorDesc = new StringBuilder("(");
        for (var arg : args.args) {
            System.err.println("Warning: Unknown argument type: " + arg.getClass().getName());
            accessorDesc.append("Lpython/_core/VMObject;");
        }
        accessorDesc.append(")").append(typeOf(returnType).getDescriptor());
        MethodNode accessorMethod = new MethodNode(Opcodes.ACC_PUBLIC, name, accessorDesc.toString(), null, null);
        accessorMethod.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        accessorMethod.instructions.add(new IntInsnNode(Opcodes.BIPUSH, args.args.size()));
        accessorMethod.instructions.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));
        int var = 1;
        for (var arg : args.args) {
            accessorMethod.instructions.add(new InsnNode(Opcodes.DUP));
            accessorMethod.instructions.add(new IntInsnNode(Opcodes.BIPUSH, var - 1));
            if (arg instanceof AstParser.arg) {
                accessorMethod.instructions.add(new VarInsnNode(Opcodes.ALOAD, var));
                accessorMethod.instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, "python/_core/VMObject"));
                var++;
            } else {
                System.err.println("Warning: Unknown argument type: " + arg.getClass().getName());
                accessorMethod.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
            }
            accessorMethod.instructions.add(new InsnNode(Opcodes.AASTORE));
        }
        accessorMethod.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "createTuple", "([Ljava/lang/Object;)Lpython/builtins/Tuple;", false));
        accessorMethod.instructions.add(new TypeInsnNode(Opcodes.NEW, "python/builtins/Dict"));
        accessorMethod.instructions.add(new InsnNode(Opcodes.DUP));
        accessorMethod.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "python/builtins/Dict", "<init>", "()V", false));
        accessorMethod.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "call", "(Ljava/lang/Object;Lpython/builtins/Tuple;Lpython/builtins/Dict;)Lpython/_core/VMObject;", false));
        int type = typeOf(returnType).getSort();
        if (type == Type.VOID) {
            accessorMethod.instructions.add(new InsnNode(Opcodes.RETURN));
        } else {
            accessorMethod.instructions.add(new InsnNode(Opcodes.ARETURN));
        }
        classNode.methods.add(accessorMethod);

        if (classInitStack.peek() == methodStack.peek()) {
            methodStack.peek().instructions.add(new LdcInsnNode(Type.getObjectType(classNode.name)));
        } else {
            System.err.println("Warning: Untested behavior for class " + classNode.name + " in " + fileNode.sourceFile);
            methodStack.peek().instructions.add(new LdcInsnNode(Type.getObjectType(classNode.name)));
        }
        methodStack.peek().instructions.add(new LdcInsnNode("!" + name));
        methodStack.peek().instructions.add(new InsnNode(Opcodes.DUP));
        methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "createFunction", "(Ljava/lang/String;)Lpython/_core/VMObject;", false));
        methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "python/_core/PyLocals", "set", "(Ljava/lang/String;Lpython/_core/VMObject;)V", false));
    }

    private void popMethod() {

        Map<Integer, LabelNode> endLabels = varEndLabelStack.pop();
        Map<Integer, AbstractInsnNode> lastUses = varUseStack.pop();
        for (var entry : endLabels.entrySet()) {
            LabelNode label = entry.getValue();
            AbstractInsnNode lastUse = lastUses.get(entry.getKey());
            if (lastUse != null) {
                methodStack.peek().instructions.insert(lastUse, label);
            }
        }

        varIndexStack.pop();
        methodStack.pop();
        varInUseStack.pop();
    }

    private void pushMethod(MethodNode method) {
        if (methodStack.size() >= 3) {
            String name = methodStack.peek().name;
            if (name.startsWith("!")) {
                method.name = name + "$" + method.name;
            }
        }
        methodStack.push(method);
        varIndexStack.push(Type.getArgumentCount(method.desc) + 1);
        varEndLabelStack.push(new HashMap<>());
        varUseStack.push(new HashMap<>());
        varInUseStack.push(new BitSet());
    }

    private <T> List<T> safeList(List<T> list) {
        if (list != null) return list;
        else return List.of();
    }

    private String describe(AstParser.Arguments args) {
        StringBuilder sb = new StringBuilder("(");
        for (var arg : args.args) {
            sb.append("Lpython/_core/VMObject;");
        }
        sb.append(")Lpython/_core/VMObject;");
        return sb.toString();
    }

    private void emit(AstParser.AstNode node) {
        if (node == null) throw new NullPointerException("node cannot be null");

        System.out.println("Emitting: " + node.getClass().getName() + "...");

        int lineNo = getLineNo(node);
        if (lineNo > 0 && lineNo != curLineNo) {
            curLineNo = lineNo;
            AbstractInsnNode lastInsn = methodStack.peek().instructions.getLast();
            LabelNode start;
            if (lastInsn instanceof LabelNode) {
                start = (LabelNode) lastInsn;
            } else {
                start = new LabelNode();
                asmLabel(start);
            }
            methodStack.peek().instructions.add(new LineNumberNode(lineNo, start));
        }

        if (node instanceof AstParser.Module) emitModule((AstParser.Module) node);
        else if (node instanceof AstParser.FunctionDef) emitFunctionDef((AstParser.FunctionDef) node);
        else if (node instanceof AstParser.ImportFrom) emitImportFrom((AstParser.ImportFrom) node);
        else if (node instanceof AstParser.Import) emitImport((AstParser.Import) node);
        else if (node instanceof AstParser.Expr) emitExpr((AstParser.Expr) node);
        else if (node instanceof AstParser.ClassDef) emitClassDef((AstParser.ClassDef) node);
        else if (node instanceof AstParser.If) emitIf((AstParser.If) node);
        else if (node instanceof AstParser.For) emitFor((AstParser.For) node);
        else if (node instanceof AstParser.While) emitWhile((AstParser.While) node);
        else if (node instanceof AstParser.Try) emitTry((AstParser.Try) node);
        else if (node instanceof AstParser.With) emitWith((AstParser.With) node);
        else if (node instanceof AstParser.Assert) emitAssert((AstParser.Assert) node);
        else if (node instanceof AstParser.Global) emitGlobal((AstParser.Global) node);
        else if (node instanceof AstParser.Nonlocal) emitNonlocal((AstParser.Nonlocal) node);
        else if (node instanceof AstParser.Break) emitBreak((AstParser.Break) node);
        else if (node instanceof AstParser.Continue) emitContinue((AstParser.Continue) node);
        else if (node instanceof AstParser.Raise) emitRaise((AstParser.Raise) node);
        else if (node instanceof AstParser.Return) emitReturn((AstParser.Return) node);
        else if (node instanceof AstParser.Delete) emitDelStmt((AstParser.Delete) node);
        else if (node instanceof AstParser.Pass) emitPass((AstParser.Pass) node);
        else if (node instanceof AstParser.Assign) emitAssign((AstParser.Assign) node);
        else if (node instanceof AstParser.AugAssign) emitAugAssign((AstParser.AugAssign) node);
        else if (node instanceof AstParser.AnnAssign) emitAnnAssign((AstParser.AnnAssign) node);
        else if (node instanceof AstParser.BoolOp) emitBoolOp((AstParser.BoolOp) node);
        else if (node instanceof AstParser.BinOp) emitBinOp((AstParser.BinOp) node);
        else if (node instanceof AstParser.UnaryOp) emitUnaryOp((AstParser.UnaryOp) node);
        else if (node instanceof AstParser.Lambda) emitLambda((AstParser.Lambda) node);
        else if (node instanceof AstParser.Tuple) emitTuple((AstParser.Tuple) node);
        else if (node instanceof AstParser.ListNode) emitListNode((AstParser.ListNode) node);
        else if (node instanceof AstParser.SetNode) emitSetNode((AstParser.SetNode) node);
        else if (node instanceof AstParser.Dict) emitDict((AstParser.Dict) node);
        else if (node instanceof AstParser.ListComp) emitListComp((AstParser.ListComp) node);
        else if (node instanceof AstParser.SetComp) emitSetComp((AstParser.SetComp) node);
        else if (node instanceof AstParser.DictComp) emitDictComp((AstParser.DictComp) node);
        else if (node instanceof AstParser.Compare) emitCompare((AstParser.Compare) node);
        else if (node instanceof AstParser.Constant) emitConstant((AstParser.Constant) node);
        else if (node instanceof AstParser.Name) emitName((AstParser.Name) node);
        else if (node instanceof AstParser.Attribute) emitAttribute((AstParser.Attribute) node);
        else if (node instanceof AstParser.Call) emitCall((AstParser.Call) node);
        else if (node instanceof AstParser.Subscript) emitSubscript((AstParser.Subscript) node);
        else if (node instanceof AstParser.Slice) emitSlice((AstParser.Slice) node);
        else if (node instanceof AstParser.Match) emitMatch((AstParser.Match) node);
        else if (node instanceof AstParser.Yield) emitYield((AstParser.Yield) node);
        else if (node instanceof AstParser.JoinedStr) emitJoinedStr((AstParser.JoinedStr) node);
        else if (node instanceof AstParser.FormattedValue) emitFormattedValue((AstParser.FormattedValue) node);
        else if (node instanceof AstParser.GenericNode) {
//            throw new IllegalArgumentException("Unknown node type: " + ((AstParser.GenericNode) node).props.get("$class"));
            System.err.println("Warning: Unknown node type: " + ((AstParser.GenericNode) node).props.get("$class"));
        } else {
            throw new IllegalArgumentException("Unknown node type: " + node.getClass().getName());
        }
    }

    private void emitYield(AstParser.Yield node) {
        if (node.value == null) {
            System.err.println("Warning: Yield statement has no value");
            return;
        }
        emit(node.value);
        methodStack.peek().instructions.add(new InsnNode(Opcodes.DUP));
        methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "python/_core/VMObject", "yield", "()V", false));
    }

    private int getLineNo(AstParser.AstNode node) {
        Class<?> clazz = node.getClass();
        try {
            Field lineno = clazz.getField("lineno");
            return (int) lineno.get(node);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return -1;
        }
    }

    private void emitModule(AstParser.Module node) {
        if (classStack.isEmpty()) {
            this.classStack.push(this.fileNode);
            this.initStack.push(this.fileInit);
            this.pushMethod(this.fileInit);
            this.classInitStack.push(this.fileClassInit);
            var excVar = Type.getArgumentCount(this.fileInit.desc) + 1;
            AbstractInsnNode insnNode = methodStack.peek().instructions.getLast();
            varEndLabelStack.peek().computeIfAbsent(
                    excVar, k -> {
                        LabelNode start = new LabelNode();
                        LabelNode labelNode = new LabelNode();
                        asmLabel(start);
//                        methodStack.peek().localVariables.add(new LocalVariableNode("$!tmpVar" + excVar, "Ljava/lang/Object;", null, start, labelNode, excVar));
                        return labelNode;
                    }
            );
            varUseStack.peek().put(excVar, insnNode);
            for (AstParser.AstNode child : safeList(node.body)) {
                this.emit(child);
            }
            this.classInitStack.pop();
            this.popMethod();
            this.initStack.pop();
            this.classStack.pop();
        }
    }

    private void emitFunctionDef(AstParser.FunctionDef node) {
        this.addFunction(node.name, node.args, node.body, node.returns);
    }

    private void emitImportFrom(AstParser.ImportFrom node) {
        String module = node.module;
        for (AstParser.Alias name : node.names) {
            asmSetLocal(
                    name.asname == null ? name.name.substring(0, name.name.indexOf('.') == -1 ? name.name.length() : name.name.indexOf('.')) : name.asname, () -> {
                        methodStack.peek().instructions.add(new LdcInsnNode(module));
                        methodStack.peek().instructions.add(new LdcInsnNode(name.name));
                        methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "importModule", "(Ljava/lang/String;Ljava/lang/String;)Lpython/_core/VMObject;", false));
                    }
            );
        }
    }

    private void emitImport(AstParser.Import node) {
        for (AstParser.Alias name : node.names) {
            asmSetLocal(
                    name.asname == null ? name.name : name.asname, () -> {
                        methodStack.peek().instructions.add(new LdcInsnNode(name.name));
                        methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "importModule", "(Ljava/lang/String;)Lpython/_core/VMObject;", false));
                    }
            );
        }
    }

    private void emitExpr(AstParser.Expr node) {
        AstParser.AstNode value = node.value;
        exprStack.push(value);
        emit(value);
        exprStack.pop();
        if (exprStack.isEmpty()) {
            methodStack.peek().instructions.add(new InsnNode(Opcodes.POP));
        }
    }

    private void emitClassDef(AstParser.ClassDef node) {
        System.out.println("Adding class " + node.name + " to class " + classStack.peek().name);
        addClass(node.name, node.bases, node.body);
    }

    private void addClass(String name, List<AstParser.AstNode> bases, List<AstParser.AstNode> body) {
        ClassNode parent = classStack.peek();
        ClassNode classNode = new ClassNode();
        classNode.superName = "java/lang/Object";
        classNode.version = Opcodes.V1_8;
        classNode.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER;
        classNode.name = parent.name + "$" + name;
        classNode.interfaces = new ArrayList<>();
        classNode.interfaces.add("python/_core/VMObject");
        classStack.push(classNode);
        MethodNode clinit = new MethodNode(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
        classInitStack.push(clinit);
        pushMethod(clinit);
        clinit.instructions.add(new TypeInsnNode(Opcodes.NEW, "python/_core/PyLocals"));
        clinit.instructions.add(new InsnNode(Opcodes.DUP));
        clinit.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "python/_core/PyLocals", "<init>", "()V", false));
        clinit.instructions.add(new VarInsnNode(Opcodes.ASTORE, 0));
        AbstractInsnNode insnNode = methodStack.peek().instructions.getLast();
        varUseStack.peek().put(0, insnNode);
        varEndLabelStack.peek().computeIfAbsent(
                0, k -> {
                    LabelNode start = new LabelNode();
                    LabelNode labelNode = new LabelNode();
                    asmLabel(start);
//            methodStack.peek().localVariables.add(new LocalVariableNode("$!tmpVar0", "Lpython/_core/PyLocals;", null, start, labelNode, 0));
                    return labelNode;
                }
        );
        for (AstParser.AstNode child : safeList(body)) {
            this.emit(child);
        }

        parent.innerClasses.add(new InnerClassNode(classNode.name, parent.name, name, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC));
        if (parent.nestMembers == null) parent.nestMembers = new ArrayList<>();
        parent.nestMembers.add(classNode.name);

        classNode.outerClass = parent.name;
        classNode.nestHostClass = parent.name;
        if (classNode.methods == null) {
            classNode.methods = new ArrayList<>();
        }
        clinit.instructions.add(new InsnNode(Opcodes.RETURN));
        classNode.methods.add(clinit);
        popMethod();
        classInitStack.pop();
        classStack.pop();

        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        CheckClassAdapter checkClassAdapter = new CheckClassAdapter(classWriter);
        classNode.accept(checkClassAdapter);
        Main.writeBytesToFile(outputDir.resolve(classNode.name + ".class"), classWriter.toByteArray());
    }

    private void emitIf(AstParser.If node) {
        if (node.test == null) {
            System.err.println("Warning: If statement has no test");
            return;
        }

        emit(node.test);
        methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "toBoolean", "(Ljava/lang/Object;)Z", false));
        LabelNode endLabel = new LabelNode();
        LabelNode orElseLabel = new LabelNode();
        methodStack.peek().instructions.add(new JumpInsnNode(Opcodes.IFEQ, orElseLabel));
        for (AstParser.AstNode child : safeList(node.body)) {
            this.emit(child);
        }
        methodStack.peek().instructions.add(new JumpInsnNode(Opcodes.GOTO, endLabel));
        asmLabel(orElseLabel);
        for (AstParser.AstNode child : safeList(node.orelse)) {
            this.emit(child);
        }
        asmLabel(endLabel);
        if (node.orelse != null) {
            System.err.println("Warning: If statement has no else clause");
        }
    }

    private void emitFor(AstParser.For node) {
        if (node.iter == null) {
            System.err.println("Warning: For statement has no iterator");
            return;
        }
        emit(node.iter);
        getAttr("__iter__");
        call(null);
        int varIndex = createVarIndex();
        asmStoreVar(varIndex);
        LabelNode endLabel = new LabelNode();
        LabelNode continueLabel = new LabelNode();
        LabelNode breakLabel = new LabelNode();
        LoopContext loopContext = new LoopContext();
        loopContext.start = continueLabel;
        loopContext.end = endLabel;
        loopContext.continueLabel = continueLabel;
        loopContext.breakLabel = breakLabel;

        set(
                node.target, () -> {
                    asmLoadVar(varIndex);
                    methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "python/_core/VMObject", "next", "()Lpython/_core/VMObject;", true));
                }
        );

        for (AstParser.AstNode child : safeList(node.body)) {
            this.emit(child);
        }
    }

    private void call(@Nullable AstParser.Arguments node) {
        if (node == null) {
            methodStack.peek().instructions.add(new MethodInsnNode(INVOKESTATIC, "python/_core/Py", "call", "(Ljava/lang/Object;)Lpython/_core/VMObject;", false));
            return;
        }
        throw new CompilerException("Not Implemented!");
    }

    private void getAttr(String name) {
        methodStack.peek().instructions.add(new LdcInsnNode(name));
        methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "python/_core/VMObject", "getAttr", "(Ljava/lang/String;)Lpython/_core/VMObject;", true));
    }

    private int createVarIndex() {
        return varIndexStack.push(varIndexStack.isEmpty() ? Type.getArgumentCount(methodStack.peek().desc) + 2 : varIndexStack.pop() + 1);
    }

    private void emitWhile(AstParser.While node) {
        if (node.test == null) {
            System.err.println("Warning: While statement has no test");
            return;
        }

        MethodNode mv = methodStack.peek();
        LabelNode startLabel = new LabelNode();
        LabelNode endLabel = new LabelNode();
        LabelNode elseLabel = node.orelse == null ? endLabel : new LabelNode();

        LoopContext loopContext = new LoopContext();
        loopContext.start = startLabel;
        loopContext.end = endLabel;
        loopContext.continueLabel = startLabel;
        loopContext.breakLabel = endLabel;
        loopContextStack.push(loopContext);

        // Initial test
        emit(node.test);
        mv.instructions.add(new MethodInsnNode(INVOKESTATIC, "python/_core/Py", "toBoolean", "(Ljava/lang/Object;)Z", false));
        mv.instructions.add(new JumpInsnNode(Opcodes.IFEQ, elseLabel));

        mv.instructions.add(startLabel);

        // Loop body
        for (AstParser.AstNode child : safeList(node.body)) {
            emit(child);
        }

        // Test for next iteration
        emit(node.test);
        mv.instructions.add(new MethodInsnNode(INVOKESTATIC, "python/_core/Py", "toBoolean", "(Ljava/lang/Object;)Z", false));
        mv.instructions.add(new JumpInsnNode(Opcodes.IFNE, startLabel));

        // Else block
        if (node.orelse != null) {
            mv.instructions.add(elseLabel);
            for (AstParser.AstNode child : safeList(node.orelse)) {
                emit(child);
            }
        }

        mv.instructions.add(endLabel);
        loopContextStack.pop();
    }

    private void emitTry(AstParser.Try node) {
        MethodNode mv = methodStack.peek();
        if (mv == null) throw new IllegalStateException("No active method in methodStack");

        Label tryStart = new Label();
        Label tryEnd = new Label();
        Label finallyStart = node.finalbody.isEmpty() ? null : new Label();
        Label finallyEnd = node.finalbody.isEmpty() ? null : new Label();
        Label endTry = new Label();

        mv.visitLabel(tryStart);

        // --- Step 1: Emit try body ---
        for (AstParser.AstNode stmt : node.body) {
            emit(stmt);
        }

        mv.visitLabel(tryEnd);

        // --- Step 2: Jump to else / finally / end ---
        if (!node.orelse.isEmpty()) {
            for (AstParser.AstNode stmt : node.orelse) emit(stmt);
        }

        if (!node.finalbody.isEmpty()) {
            mv.visitJumpInsn(GOTO, finallyStart);
        } else {
            mv.visitJumpInsn(GOTO, endTry);
        }

        // --- Step 3: Emit except handlers ---
        Label handlerFinally = finallyStart != null ? finallyStart : endTry;
        for (AstParser.ExceptHandler handler : node.handlers) {
            Label handlerStart = new Label();
            Label handlerEnd = new Label();

            // Try-catch block for this handler
            mv.visitTryCatchBlock(
                    tryStart, tryEnd, handlerStart,
                    "python/builtins/Throwable"
            );

            mv.visitLabel(handlerStart);
            lineBarrier(handler);

            // If the handler has a target, store the exception
            if (handler.name != null) {
                int excVar = nextVar();
                asmStoreVar(excVar);
                asmLoadVar(excVar);
                asmSetLocal(handler.name);
            } else {
                mv.visitInsn(POP); // discard exception
            }

            // Emit handler body
            for (AstParser.AstNode stmt : handler.body) emit(stmt);

            mv.visitJumpInsn(GOTO, handlerFinally);
            mv.visitLabel(handlerEnd);
        }

        mv.visitLabel(handlerFinally);

        // --- Step 4: Emit finally body ---
        if (!node.finalbody.isEmpty()) {
            mv.visitLabel(finallyStart);
            for (AstParser.AstNode stmt : node.finalbody) emit(stmt);
            mv.visitLabel(finallyEnd);
        }

        mv.visitLabel(endTry);
    }

    private void asmStoreVar(int excVar) {
        VarInsnNode insnNode = new VarInsnNode(ASTORE, excVar);
        methodStack.peek().instructions.add(insnNode);
        varEndLabelStack.peek().computeIfAbsent(
                excVar, k -> {
                    LabelNode start = new LabelNode();
                    LabelNode labelNode = new LabelNode();
                    asmLabel(start);
//                    methodStack.peek().localVariables.add(new LocalVariableNode("$!tmpVar" + excVar, "Ljava/lang/Object;", null, start, labelNode, excVar));
                    return labelNode;
                }
        );
        varUseStack.peek().put(excVar, insnNode);
    }

    private void asmLabel(LabelNode start) {
        AbstractInsnNode last = methodStack.peek().instructions.getLast();
        if (last instanceof JumpInsnNode) {
            JumpInsnNode lastJumpInsnNode = (JumpInsnNode) last;
            if (lastJumpInsnNode.getOpcode() == GOTO && lastJumpInsnNode.label == start) {
                methodStack.peek().instructions.remove(last);
            }
        }
        methodStack.peek().instructions.add(start);
    }

    private void asmLoadVar(int excVar) {
        VarInsnNode insnNode = new VarInsnNode(ALOAD, excVar);
        methodStack.peek().instructions.add(insnNode);
        if (varUseStack.peek().get(excVar) == null) {
            throw new IllegalStateException("Variable " + excVar + " is not in use stack");
        }
        varUseStack.peek().put(excVar, insnNode);
    }

    private void emitWith(AstParser.With node) {
        MethodNode mv = methodStack.peek();
        if (mv == null)
            throw new IllegalStateException("No active method in methodStack");

        // --- Step 1: Evaluate and enter all context managers ---
        List<Integer> contextVars = new ArrayList<>();
        List<Integer> valueVars = new ArrayList<>();

        for (AstParser.withitem item : node.items) {
            // Evaluate context expression and store it
            emit(item.context_expr);
            int ctxVar = nextVar();
            asmStoreVar(ctxVar);
            contextVars.add(ctxVar);

            // --- Call __enter__ (via Py.call) ---
            asmLoadVar(ctxVar);
            mv.visitMethodInsn(
                    INVOKESTATIC,
                    "python/_core/Py",
                    "enter",
                    "(Lpython/_core/VMObject;)Lpython/_core/VMObject;",
                    false
            );

            // Store the result of __enter__()
            int valVar = nextVar();
            asmStoreVar(valVar);
            valueVars.add(valVar);

            // If "as <var>", assign __enter__ result to variable
            if (item.optional_vars != null) {
                lineBarrier(item.optional_vars);
                if (item.optional_vars instanceof AstParser.Name) {
                    AstParser.Name name = (AstParser.Name) item.optional_vars;
                    asmLoadVar(valVar);
                    asmSetLocal(name.id);
                } else {
                    throw new CompilerException("Unsupported 'as' target: " + item.optional_vars.getClass());
                }
            }
        }

        // --- Step 2: Setup try/finally for exit calls ---
        LabelNode tryStart = new LabelNode();
        LabelNode tryEnd = new LabelNode();
        LabelNode finallyStart = new LabelNode();
        LabelNode finallyEnd = new LabelNode();

        asmLabel(tryStart);

        // --- Step 3: Body of the with-statement ---
        for (AstParser.AstNode stmt : node.body) {
            emit(stmt);
        }

        mv.visitInsn(ACONST_NULL);
        asmLabel(tryEnd);

        // --- Step 4: Ensure __exit__ is called (finally) ---
        asmLabel(finallyStart);
        mv.visitInsn(POP);

        // Exit contexts in reverse order (Python semantics)
        for (int i = contextVars.size() - 1; i >= 0; i--) {
            int ctxVar = contextVars.get(i);

            asmLoadVar(ctxVar);
            mv.visitMethodInsn(
                    INVOKESTATIC,
                    "python/_core/Py",
                    "exit",
                    "(Lpython/_core/VMObject;)Lpython/_core/VMObject;",
                    false
            );

            // discard return value from __exit__
            mv.visitInsn(POP);
        }

        tryCatchBlock(tryStart, finallyEnd, finallyStart, null);
        asmLabel(finallyEnd);
    }

    private void tryCatchBlock(LabelNode start, LabelNode end, LabelNode handler, String type) {
        methodStack.peek().tryCatchBlocks.add(new TryCatchBlockNode(start, end, handler, type));
    }

    private void asmGoto(LabelNode finallyStart) {
        methodStack.peek().instructions.add(new JumpInsnNode(GOTO, finallyStart));
    }

    private void emitAssert(AstParser.Assert node) {
        MethodNode mv = methodStack.peek();
        if (mv == null)
            throw new IllegalStateException("No active method in methodStack");

        Label end = new Label();
        Label fail = new Label();

        // --- Step 1: Evaluate the test expression ---
        emit(node.test);  // result (boolean) on stack

        // --- Step 2: Jump if true (assertion passed) ---
        mv.visitMethodInsn(INVOKESTATIC, "python/_core/Py", "toBoolean", "(Ljava/lang/Object;)Z", false);
        mv.visitJumpInsn(IFNE, end);

        // --- Step 3: Assertion failed — create AssertionError instance ---
        mv.visitLabel(fail);

        mv.visitTypeInsn(NEW, "python/builtins/AssertionError");
        mv.visitInsn(DUP);

        if (node.msg != null) {
            // Emit message expression
            emit(node.msg);

            // Call constructor with message (Object)
            mv.visitMethodInsn(
                    INVOKESPECIAL,
                    "python/builtins/AssertionError",
                    "<init>",
                    "(Lpython/runtime/PyObject;)V",
                    false
            );
        } else {
            // Call no-arg constructor
            mv.visitMethodInsn(
                    INVOKESPECIAL,
                    "python/builtins/AssertionError",
                    "<init>",
                    "()V",
                    false
            );
        }

        // --- Step 4: Throw it ---
        mv.visitInsn(ATHROW);

        // --- Step 5: Continue if successful ---
        mv.visitLabel(end);
    }

    private void emitGlobal(AstParser.Global node) {
        MethodNode mv = methodStack.peek();
        for (String name : node.names) {
            asmLoadVar(Type.getArgumentCount(mv.desc) + 1);
            mv.visitTypeInsn(CHECKCAST, "python/_core/PyLocals");
            mv.visitLdcInsn(name);
            mv.visitMethodInsn(INVOKEVIRTUAL, "python/_core/PyLocals", "globalize", "(Ljava/lang/String;)V", false);
        }
    }

    private void emitNonlocal(AstParser.Nonlocal node) {
        MethodNode mv = methodStack.peek();
        if (mv == null)
            throw new IllegalStateException("No active method in methodStack");

        for (String name : node.names) {
            // Load locals
            asmLoadVar(Type.getArgumentCount(mv.desc) + 1);

            // Cast to PyLocals
            mv.visitTypeInsn(CHECKCAST, "python/_core/PyLocals");

            // Push name constant
            mv.visitLdcInsn(name);

            // Call PyLocals.nonlocalize(String)
            mv.visitMethodInsn(
                    INVOKEVIRTUAL, "python/_core/PyLocals",
                    "nonlocalize", "(Ljava/lang/String;)V", false
            );
        }
    }

    private void emitBreak(AstParser.Break node) {
        if (loopContextStack.isEmpty()) {
            raise("Break statement outside loop");
            return;
        }
        LoopContext loopContext = loopContextStack.peek();
        methodStack.peek().instructions.add(new JumpInsnNode(Opcodes.GOTO, loopContext.breakLabel));
    }

    private void raise(String message) {
        methodStack.peek().instructions.add(new TypeInsnNode(Opcodes.NEW, "python/builtins/SyntaxError"));
        methodStack.peek().instructions.add(new InsnNode(Opcodes.DUP));
        methodStack.peek().instructions.add(new LdcInsnNode(message));
        methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "python/builtins/SyntaxError", "<init>", "(Ljava/lang/String;)V", false));
        methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "raise", "(Lpython/builtins/VMObject;)V", false));
        System.err.println("Error: " + message);
    }


    private void emitContinue(AstParser.Continue node) {
        if (loopContextStack.isEmpty()) {
            raise("Continue statement outside loop");
            return;
        }
        LoopContext loopContext = loopContextStack.peek();
        methodStack.peek().instructions.add(new JumpInsnNode(Opcodes.GOTO, loopContext.continueLabel));
    }

    private void emitRaise(AstParser.Raise node) {
        if (node.exc == null) {
            methodStack.peek().instructions.add(new InsnNode(Opcodes.ACONST_NULL));
            methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "raise", "(Lpython/builtins/VMObject;)V", false));
        } else {
            emit(node.exc);
            methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "raise", "(Lpython/builtins/VMObject;)V", false));
        }
    }

    private void emitReturn(AstParser.Return node) {
        if (node.value != null) {
            int size = methodStack.peek().instructions.size();
            emit(node.value);
            if (methodStack.peek().instructions.size() == size) {
                System.err.println("Warning: Return statement has no effect");
            }
            methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "toJava", "(Ljava/lang/Object;)Ljava/lang/Object;", false));
        } else {
            methodStack.peek().instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        }
        methodStack.peek().instructions.add(new InsnNode(Opcodes.DUP));
        methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "returnFrom", "(Ljava/lang/Object;)V", false));
        methodStack.peek().instructions.add(new InsnNode(Opcodes.ARETURN));
    }

    private void emitExprStmt(AstParser.Expr node) {throw new CompilerException("Not Implemented!");}

    private void emitDelStmt(AstParser.Delete node) {
        MethodNode mv = methodStack.peek();
        if (mv == null)
            throw new IllegalStateException("No active method in methodStack");

        for (AstParser.AstNode target : node.targets) {
            if (target instanceof AstParser.Name) {
                AstParser.Name nameTarget = (AstParser.Name) target;
                // del <name>
                asmLoadVar(Type.getArgumentCount(mv.desc) + 1);
                mv.visitTypeInsn(CHECKCAST, "python/_core/PyLocals");
                mv.visitLdcInsn(nameTarget.id);
                mv.visitMethodInsn(
                        INVOKEVIRTUAL,
                        "python/_core/PyLocals",
                        "delete",
                        "(Ljava/lang/String;)V",
                        false
                );

            } else if (target instanceof AstParser.Attribute) {
                AstParser.Attribute attrTarget = (AstParser.Attribute) target;
                // del <object>.<attr>
                emit(attrTarget.value);
                mv.visitLdcInsn(attrTarget.attr);
                mv.visitMethodInsn(
                        INVOKESTATIC,
                        "python/_core/Py",
                        "delAttr",
                        "(Lpython/_core/VMObject;Ljava/lang/String;)V",
                        false
                );

            } else if (target instanceof AstParser.Subscript) {
                AstParser.Subscript subTarget = (AstParser.Subscript) target;
                // del <object>[<key>]
                emit(subTarget.value);
                emit(subTarget.slice);
                mv.visitMethodInsn(INVOKESTATIC,
                        "python/_core/Py",
                        "delItem",
                        "(Lpython/_core/VMObject;Lpython/_core/VMObject;)V",
                        false);

            } else {
                throw new CompilerException("Unsupported delete target: " + target.getClass().getName());
            }
        }
    }

    private void emitPass(AstParser.Pass node) {
        // Do nothing
    }

    private void emitAssign(AstParser.Assign node) {
        emit(node.value);
        if (node.targets.size() == 1) {
            AstParser.AstNode target = node.targets.get(0);
            if (target instanceof AstParser.Name) {
                String name = ((AstParser.Name) target).id;
                asmSetLocal(name);
            } else if (target instanceof AstParser.Attribute) {
                AstParser.Attribute attrTarget = (AstParser.Attribute) target;
                // Assign <value> to <object>.<attr>
                emit(attrTarget.value);
                methodStack.peek().instructions.add(new LdcInsnNode(attrTarget.attr));
                methodStack.peek().instructions.add(new MethodInsnNode(INVOKESTATIC, "python/_core/Py", "setAttr", "(Lpython/_core/VMObject;Ljava/lang/String;Lpython/_core/VMObject;)V", false));
            } else if (target instanceof AstParser.Subscript) {
                AstParser.Subscript subTarget = (AstParser.Subscript) target;
                // Assign <value> to <object>[<key>]
                emit(subTarget.value);
                emit(subTarget.slice);
                methodStack.peek().instructions.add(new MethodInsnNode(INVOKESTATIC, "python/_core/Py", "setItem", "(Lpython/_core/VMObject;Lpython/_core/VMObject;Lpython/_core/VMObject;)V", false));
            } else if (target instanceof AstParser.Tuple) {
                AstParser.Tuple tuple = (AstParser.Tuple) target;
                List<AstParser.AstNode> elts = tuple.elts;
                for (int i = 0, eltsSize = elts.size(); i < eltsSize; i++) {
                    AstParser.AstNode t = elts.get(i);
                    if (t instanceof AstParser.Name) {
                        if (i < eltsSize - 1)
                            methodStack.peek().instructions.add(new InsnNode(DUP));
                        constant(i);
                        methodStack.peek().instructions.add(new MethodInsnNode(INVOKESTATIC, "python/_core/Py", "getItem", "(Ljava/lang/Object;I)Ljava/lang/Object;", false));
                        String name = ((AstParser.Name) t).id;
                        asmSetLocal(name);
                    } else {
                        throw new CompilerException("Unsupported assignment target: " + t.getClass().getName());
                    }
                }
            }
        } else {
            // a = b = c = <value>
            // a is set to b = c = <value>
            // b is set to c = <value>
            // c is set to <value>
            //
            // So we set c to the value first, then set b to c, then set a to b
            for (int i = node.targets.size() - 1; i >= 0; i--) {
                AstParser.AstNode target = node.targets.get(i);
                if (target instanceof AstParser.Name) {
                    String name = ((AstParser.Name) target).id;
                    asmSetLocal(name);
                    if (i > 0)
                        getLocal(name);
                } else if (target instanceof AstParser.Attribute) {
                    AstParser.Attribute attrTarget = (AstParser.Attribute) target;
                    // Assign <value> to <object>.<attr>
                    emit(attrTarget.value);
                    methodStack.peek().instructions.add(new InsnNode(DUP));
                    methodStack.peek().instructions.add(new LdcInsnNode(attrTarget.attr));
                    methodStack.peek().instructions.add(new MethodInsnNode(INVOKESTATIC, "python/_core/Py", "setAttr", "(Lpython/_core/VMObject;Ljava/lang/String;Lpython/_core/VMObject;)V", false));
                    if (i > 0) {
                        methodStack.peek().instructions.add(new LdcInsnNode(attrTarget.attr));
                        methodStack.peek().instructions.add(new MethodInsnNode(INVOKESTATIC, "python/_core/Py", "getAttr", "(Lpython/_core/VMObject;Ljava/lang/String;)Lpython/_core/VMObject;", false));
                    }
                } else if (target instanceof AstParser.Subscript) {
                    AstParser.Subscript subTarget = (AstParser.Subscript) target;
                    // Assign <value> to <object>[<key>]
                    emit(subTarget.value);
                    emit(subTarget.slice);
                    methodStack.peek().instructions.add(new MethodInsnNode(INVOKESTATIC, "python/_core/Py", "setItem", "(Lpython/_core/VMObject;Lpython/_core/VMObject;Lpython/_core/VMObject;)V", false));
                    if (i > 0) {
                        methodStack.peek().instructions.add(new MethodInsnNode(INVOKESTATIC, "python/_core/Py", "getItem", "(Lpython/_core/VMObject;I)Ljava/lang/Object;", false));
                    }
                } else {
                    throw new CompilerException("Unsupported assignment target: " + target.getClass().getName());
                }
            }
        }
    }

    @Deprecated
    private void asmSetLocal(String name) {
        if ((methodStack.peek().access & Opcodes.ACC_STATIC) == 0) {
            asmLoadVar(Type.getArgumentCount(methodStack.peek().desc) + 1);
            methodStack.peek().instructions.add(new LdcInsnNode(name));
            methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "setLocal", "(Ljava/lang/Object;Lpython/_core/PyLocals;Ljava/lang/String;)V", false));
        } else {
            methodStack.peek().instructions.add(new LdcInsnNode(Type.getObjectType(classStack.peek().name)));
            methodStack.peek().instructions.add(new LdcInsnNode(name));
            asmLoadVar(Type.getArgumentCount(methodStack.peek().desc));
            methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "setAttr", "(Ljava/lang/String;Ljava/lang/Object;)V", false));
        }
    }

    private void asmSetLocal(String name, Runnable block) {
        if (methodStack.peek() == classInitStack.peek()) {
            asmLoadVar(Type.getArgumentCount(methodStack.peek().desc) + 1);
            methodStack.peek().instructions.add(new LdcInsnNode(name));
            block.run();
            methodStack.peek().instructions.add(new MethodInsnNode(INVOKEINTERFACE, "python/_core/PyLocals", "set", "(Ljava/lang/String;Ljava/lang/Object;)V", true));
        } else if ((methodStack.peek().access & Opcodes.ACC_STATIC) == 0) {
            asmLoadVar(Type.getArgumentCount(methodStack.peek().desc) + 1);
            methodStack.peek().instructions.add(new LdcInsnNode(name));
            block.run();
            methodStack.peek().instructions.add(new MethodInsnNode(INVOKEINTERFACE, "python/_core/PyLocals", "set", "(Ljava/lang/String;Ljava/lang/Object;)V", true));
        } else {
            asmLoadVar(Type.getArgumentCount(methodStack.peek().desc));
            methodStack.peek().instructions.add(new LdcInsnNode(name));
            block.run();
            methodStack.peek().instructions.add(new MethodInsnNode(INVOKEINTERFACE, "python/_core/PyLocals", "set", "(Ljava/lang/String;Ljava/lang/Object;)V", true));
        }
    }

    private void getLocal(String name) {
        if (methodStack.peek() == classInitStack.peek()) {
            int i = nextVar();
            asmLoadVar(i);
            methodStack.peek().instructions.add(new LdcInsnNode(Type.getObjectType(classStack.peek().name)));
            methodStack.peek().instructions.add(new LdcInsnNode(name));
            methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "python/_core/PyLocals", "get", "(Ljava/lang/String;)Lpython/_core/VMObject;", false));
        } else if ((methodStack.peek().access & Opcodes.ACC_STATIC) == 0) {
            asmLoadVar(Type.getArgumentCount(methodStack.peek().desc) + 1);
            methodStack.peek().instructions.add(new LdcInsnNode(name));
            methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "getLocal", "(Ljava/lang/Object;Ljava/lang/String;)Lpython/_core/VMObject;", false));
        } else {
            methodStack.peek().instructions.add(new LdcInsnNode(Type.getObjectType(classStack.peek().name)));
            methodStack.peek().instructions.add(new LdcInsnNode(name));
            asmLoadVar(Type.getArgumentCount(methodStack.peek().desc));
            methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "getAttr", "(Ljava/lang/String;Ljava/lang/Object;)Lpython/_core/VMObject;", false));
        }
    }

    private void constant(int i) {
        if (i == -1) {
            methodStack.peek().instructions.add(new InsnNode(Opcodes.ICONST_M1));
            return;
        } else if (i == 0) {
            methodStack.peek().instructions.add(new InsnNode(Opcodes.ICONST_0));
            return;
        } else if (i == 1) {
            methodStack.peek().instructions.add(new InsnNode(Opcodes.ICONST_1));
            return;
        } else if (i == 2) {
            methodStack.peek().instructions.add(new InsnNode(Opcodes.ICONST_2));
            return;
        } else if (i == 3) {
            methodStack.peek().instructions.add(new InsnNode(Opcodes.ICONST_3));
            return;
        } else if (i == 4) {
            methodStack.peek().instructions.add(new InsnNode(Opcodes.ICONST_4));
            return;
        } else if (i == 5) {
            methodStack.peek().instructions.add(new InsnNode(Opcodes.ICONST_5));
            return;
        }

        if (i < 0) {
            methodStack.peek().instructions.add(new LdcInsnNode(i));
        } else if (i < 256) {
            methodStack.peek().instructions.add(new IntInsnNode(Opcodes.BIPUSH, i));
        } else if (i < 65536) {
            methodStack.peek().instructions.add(new IntInsnNode(Opcodes.SIPUSH, i));
        } else {
            methodStack.peek().instructions.add(new LdcInsnNode(i));
        }
    }

    private void constant(long i) {
        if (i == 0) {
            methodStack.peek().instructions.add(new InsnNode(Opcodes.LCONST_0));
            return;
        } else if (i == 1) {
            methodStack.peek().instructions.add(new InsnNode(Opcodes.LCONST_1));
            return;
        }

        methodStack.peek().instructions.add(new LdcInsnNode(i));
    }

    private void constant(float i) {
        if (i == 0.0f) {
            methodStack.peek().instructions.add(new InsnNode(Opcodes.FCONST_0));
            return;
        } else if (i == 1.0f) {
            methodStack.peek().instructions.add(new InsnNode(Opcodes.FCONST_1));
            return;
        } else if (i == 2.0f) {
            methodStack.peek().instructions.add(new InsnNode(Opcodes.FCONST_2));
            return;
        }

        methodStack.peek().instructions.add(new LdcInsnNode(i));
    }

    private void constant(double i) {
        if (i == 0.0) {
            methodStack.peek().instructions.add(new InsnNode(Opcodes.DCONST_0));
            return;
        } else if (i == 1.0) {
            methodStack.peek().instructions.add(new InsnNode(Opcodes.DCONST_1));
            return;
        }

        methodStack.peek().instructions.add(new LdcInsnNode(i));
    }

    private void constant(boolean v) {
        methodStack.peek().instructions.add(new InsnNode(v ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
    }

    private void constant(String s) {
        if (s == null) {
            methodStack.peek().instructions.add(new InsnNode(Opcodes.ACONST_NULL));
            return;
        }
        methodStack.peek().instructions.add(new LdcInsnNode(s));
    }

    private void emitAugAssign(AstParser.AugAssign node) {
        if (node.target instanceof AstParser.Attribute) {
            AstParser.Attribute attrTarget = (AstParser.Attribute) node.target;
            // Assign <value> to <object>.<attr>
            emit(attrTarget.value);
            methodStack.peek().instructions.add(new LdcInsnNode(attrTarget.attr));
        }
        emit(node.target);
        emit(node.value);
        if (node.op instanceof AstParser.Add)
            methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "add", "(Ljava/lang/Object;Ljava/lang/Object;)Lpython/builtins/Object;", false));
        else if (node.op instanceof AstParser.Sub)
            methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "sub", "(Ljava/lang/Object;Ljava/lang/Object;)Lpython/builtins/Object;", false));
        else if (node.op instanceof AstParser.Mult)
            methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "mul", "(Ljava/lang/Object;Ljava/lang/Object;)Lpython/builtins/Object;", false));
        else if (node.op instanceof AstParser.Mod)
            methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "mod", "(Ljava/lang/Object;Ljava/lang/Object;)Lpython/builtins/Object;", false));
        else if (node.op instanceof AstParser.Div)
            methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "div", "(Ljava/lang/Object;Ljava/lang/Object;)Lpython/builtins/Object;", false));
        else if (node.op instanceof AstParser.FloorDiv)
            methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "floorDiv", "(Ljava/lang/Object;Ljava/lang/Object;)Lpython/builtins/Object;", false));
        else if (node.op instanceof AstParser.LShift)
            methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "lShift", "(Ljava/lang/Object;Ljava/lang/Object;)Lpython/builtins/Object;", false));
        else if (node.op instanceof AstParser.RShift)
            methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "rShift", "(Ljava/lang/Object;Ljava/lang/Object;)Lpython/builtins/Object;", false));
        else if (node.op instanceof AstParser.BitAnd)
            methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "bitAnd", "(Ljava/lang/Object;Ljava/lang/Object;)Lpython/builtins/Object;", false));
        else if (node.op instanceof AstParser.BitOr)
            methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "bitOr", "(Ljava/lang/Object;Ljava/lang/Object;)Lpython/builtins/Object;", false));
        else if (node.op instanceof AstParser.BitXor)
            methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "bitXor", "(Ljava/lang/Object;Ljava/lang/Object;)Lpython/builtins/Object;", false));
        else if (node.op instanceof AstParser.Pow)
            methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "pow", "(Ljava/lang/Object;Ljava/lang/Object;)Lpython/builtins/Object;", false));
        else
            System.err.println("Warning: Unknown augmented assignment operator: " + node.op.getClass().getName());
        if (node.target instanceof AstParser.Name) {
            String name = ((AstParser.Name) node.target).id;
            asmSetLocal(name);
        } else if (node.target instanceof AstParser.Attribute) {
            methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "setAttr", "(Lpython/_core/VMObject;Ljava/lang/String;Lpython/_core/VMObject;)V", false));
        }
    }

    private void emitAnnAssign(AstParser.AnnAssign node) {
        set(node.target, () -> emit(node.value));
    }

    private void target(AstParser.AstNode target, AstParser.AstNode value) {
        if (target instanceof AstParser.Name) {
            String name = ((AstParser.Name) target).id;
            asmSetLocal(name);
        } else {
            System.err.println("Warning: Unknown target: " + target.getClass().getName());
        }
    }

    private void emitBoolOp(AstParser.BoolOp node) {
        if (node.op instanceof AstParser.And) {
            LabelNode endLabel = new LabelNode();
            List<AstParser.AstNode> safeList = safeList(node.values);
            for (int i = 0, safeListSize = safeList.size(); i < safeListSize; i += 2) {
                AstParser.AstNode child = safeList.get(i);
                emit(child);
                if (i < safeListSize - 1) {
                    emit(safeList.get(i + 1));
                    methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "and", "(Ljava/lang/Object;Ljava/lang/Object;)Lpython/builtins/Bool;", false));
                }
            }
            asmLabel(endLabel);
        } else if (node.op instanceof AstParser.Or) {
            LabelNode endLabel = new LabelNode();
            List<AstParser.AstNode> safeList = safeList(node.values);
            for (int i = 0, safeListSize = safeList.size(); i < safeListSize; i++) {
                AstParser.AstNode child = safeList.get(i);
                emit(child);
                if (i < safeListSize - 1) {
                    methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "or", "(Ljava/lang/Object;Ljava/lang/Object;)Lpython/builtins/Bool;", false));
                }
            }
        } else {
            System.err.println("Warning: Unknown boolean operator: " + node.op.getClass().getName());
            methodStack.peek().instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        }
    }

    private void emitBinOp(AstParser.BinOp node) {
        AstParser.AstNode left = node.left;
        AstParser.AstNode right = node.right;
        emit(left);
        if (right != null) {
            emit(right);
            if (node.op instanceof AstParser.Add)
                methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "add", "(Ljava/lang/Object;Ljava/lang/Object;)Lpython/builtins/Object;", false));
            else if (node.op instanceof AstParser.Sub)
                methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "sub", "(Ljava/lang/Object;Ljava/lang/Object;)Lpython/builtins/Object;", false));
            else if (node.op instanceof AstParser.Mult)
                methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "mul", "(Ljava/lang/Object;Ljava/lang/Object;)Lpython/builtins/Object;", false));
            else if (node.op instanceof AstParser.Mod)
                methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "mod", "(Ljava/lang/Object;Ljava/lang/Object;)Lpython/builtins/Object;", false));
            else if (node.op instanceof AstParser.Div)
                methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "div", "(Ljava/lang/Object;Ljava/lang/Object;)Lpython/builtins/Object;", false));
            else if (node.op instanceof AstParser.FloorDiv)
                methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "floorDiv", "(Ljava/lang/Object;Ljava/lang/Object;)Lpython/builtins/Object;", false));
            else if (node.op instanceof AstParser.LShift)
                methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "lshift", "(Ljava/lang/Object;Ljava/lang/Object;)Lpython/builtins/Object;", false));
            else if (node.op instanceof AstParser.RShift)
                methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "rshift", "(Ljava/lang/Object;Ljava/lang/Object;)Lpython/builtins/Object;", false));
            else if (node.op instanceof AstParser.BitOr)
                methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "bitOr", "(Ljava/lang/Object;Ljava/lang/Object;)Lpython/builtins/Object;", false));
            else if (node.op instanceof AstParser.BitXor)
                methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "bitXor", "(Ljava/lang/Object;Ljava/lang/Object;)Lpython/builtins/Object;", false));
            else if (node.op instanceof AstParser.BitAnd)
                methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "bitAnd", "(Ljava/lang/Object;Ljava/lang/Object;)Lpython/builtins/Object;", false));
            else if (node.op instanceof AstParser.Pow)
                methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "pow", "(Ljava/lang/Object;Ljava/lang/Object;)Lpython/builtins/Object;", false));
            else {
                System.err.println("Warning: Unknown binary operator: " + node.op.getClass().getName());
                methodStack.peek().instructions.add(new InsnNode(Opcodes.ACONST_NULL));
            }
        }
    }

    private void emitUnaryOp(AstParser.UnaryOp node) {
        emit(node.operand);
        if (node.op instanceof AstParser.Not)
            methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "not", "(Ljava/lang/Object;)Lpython/builtins/Bool;", false));
        else if (node.op instanceof AstParser.Add)
            methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "uadd", "(Ljava/lang/Object;)Lpython/builtins/Object;", false));
        else if (node.op instanceof AstParser.Sub)
            methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "usub", "(Ljava/lang/Object;)Lpython/builtins/Object;", false));
        else {
            System.err.println("Warning: Unknown unary operator: " + node.op.getClass().getName());
            methodStack.peek().instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        }
    }

    private void emitLambda(AstParser.Lambda node) {
        // Step 1: define the lambda function
        ClassNode classNode = classStack.peek();

        int lambdaId = lambdaCount++;
        String lambdaName = "lambda$" + lambdaId;

        // Each lambda argument is a VMObject
        String lambdaDesc = "(" + "Lpython/_core/VMObject;".repeat(node.args.args.size()) + ")Lpython/_core/VMObject;";

        MethodNode lambdaMethod = new MethodNode(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                lambdaName,
                lambdaDesc,
                null,
                null
        );

        pushMethod(lambdaMethod);

        List<AstParser.arg> args = node.args.args;

        methodStack.peek().instructions.add(new VarInsnNode(ALOAD, 0));
        methodStack.peek().instructions.add(new TypeInsnNode(CHECKCAST, "python/_core/PyLocals"));
        asmStoreVar(Type.getArgumentCount(lambdaMethod.desc));


        for (int i = 0, argsSize = args.size(); i < argsSize; i++) {
            AstParser.arg arg = args.get(i);
            LdcInsnNode insnNode = new LdcInsnNode(arg.arg);
            lambdaMethod.instructions.add(insnNode);
            int finalI = i;
            asmSetLocal(
                    arg.arg, () -> {
                        lambdaMethod.instructions.add(new VarInsnNode(ALOAD, finalI));
                        lambdaMethod.instructions.add(new TypeInsnNode(CHECKCAST, "python/_core/VMObject"));
                    }
            );

            varUseStack.peek().put(i, insnNode);
            varEndLabelStack.peek().put(i, new LabelNode());
        }

        // Emit the body of the lambda
        emit(node.body);  // assume this emits code that leaves a VMObject on stack

        lambdaMethod.instructions.add(new InsnNode(Opcodes.ARETURN));
        popMethod();

        // Save the lambda method for later writing
        classNode.methods.add(lambdaMethod);
        // Create a handle to the lambda method
        Handle lambdaHandle = new Handle(
                Opcodes.H_INVOKESTATIC,
                classNode.name,
                lambdaName,
                lambdaDesc,
                false
        );

        // We assume Py.makeCallable(MethodHandle) returns a VMObject
        MethodVisitor mv = methodStack.peek();

        mv.visitLdcInsn(lambdaHandle);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "python/_core/Py",
                "makeCallable",
                "(Ljava/lang/invoke/MethodHandle;)Lpython/_core/VMObject;",
                false
        );
    }

    private void emitTuple(AstParser.Tuple node) {
        // Step 1: create a new tuple
        MethodVisitor mv = methodStack.peek();
        List<AstParser.AstNode> elts = node.elts;
        mv.visitTypeInsn(Opcodes.NEW, "python/builtins/Tuple");
        mv.visitInsn(Opcodes.DUP);
        mv.visitIntInsn(Opcodes.BIPUSH, elts.size());
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "python/builtins/Object");
        // Step 2: populate the tuple
        for (int i = 0; i < elts.size(); i++) {
            mv.visitInsn(Opcodes.DUP);
            mv.visitIntInsn(Opcodes.BIPUSH, i);
            emit(elts.get(i));
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "python/_core/Py", "toPython", "(Ljava/lang/Object;)Lpython/builtins/Object;", false);
            mv.visitInsn(Opcodes.AASTORE);
        }
        // Step 3: return the tuple
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "python/builtins/Tuple", "<init>", "([Lpython/builtins/Object;)V", false);
    }

    private void emitListNode(AstParser.ListNode node) {
        // Step 1: create a new tuple
        MethodVisitor mv = methodStack.peek();
        List<AstParser.AstNode> elts = node.elts;
        mv.visitTypeInsn(Opcodes.NEW, "python/builtins/List");
        mv.visitInsn(Opcodes.DUP);
        mv.visitIntInsn(Opcodes.BIPUSH, elts.size());
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "python/builtins/Object");
        // Step 2: populate the tuple
        for (int i = 0; i < elts.size(); i++) {
            mv.visitInsn(Opcodes.DUP);
            mv.visitIntInsn(Opcodes.BIPUSH, i);
            emit(elts.get(i));
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "python/_core/Py", "toPython", "(Ljava/lang/Object;)Lpython/builtins/Object;", false);
            mv.visitInsn(Opcodes.AASTORE);
        }
        // Step 3: return the tuple
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "python/builtins/List", "<init>", "([Lpython/builtins/Object;)V", false);
    }

    private void emitSetNode(AstParser.SetNode node) {
        // Step 1: create a new tuple
        MethodVisitor mv = methodStack.peek();
        List<AstParser.AstNode> elts = node.elts;
        mv.visitTypeInsn(Opcodes.NEW, "python/builtins/Set");
        mv.visitInsn(Opcodes.DUP);
        mv.visitIntInsn(Opcodes.BIPUSH, elts.size());
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "python/builtins/Object");
        // Step 2: populate the tuple
        for (int i = 0; i < elts.size(); i++) {
            mv.visitInsn(Opcodes.DUP);
            mv.visitIntInsn(Opcodes.BIPUSH, i);
            emit(elts.get(i));
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "python/_core/Py", "toPython", "(Ljava/lang/Object;)Lpython/builtins/Object;", false);
            mv.visitInsn(Opcodes.AASTORE);
        }
        // Step 3: return the tuple
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "python/builtins/Set", "<init>", "([Lpython/builtins/Object;)V", false);
    }

    private void emitDict(AstParser.Dict node) {
        // Step 1: create a new dict
        MethodVisitor mv = methodStack.peek();
        mv.visitTypeInsn(Opcodes.NEW, "python/builtins/Dict");
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "python/builtins/Dict", "<init>", "()V", false);

        for (int i = 0; i < node.keys.size(); i++) {
            mv.visitInsn(Opcodes.DUP);
            emit(node.keys.get(i));
            emit(node.values.get(i));
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "python/_core/Py", "setItem", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V", false);
        }

    }

    private void emitListComp(AstParser.ListComp node) {
        MethodNode mv = methodStack.peek();
        if (mv == null)
            throw new IllegalStateException("No active method in methodStack");

        // --- Step 1: Create python.builtins.List instance ---
        mv.visitTypeInsn(NEW, "python/builtins/List");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "python/builtins/List", "<init>", "()V", false);

        int listVar = nextVar();
        asmStoreVar(listVar);

        // --- Step 2: Each generator ---
        for (AstParser.Comprehension gen : node.generators) {
            Label startLoop = new Label();
            Label endLoop = new Label();

            // Iterable → iterator
            emit(gen.iter);
            mv.visitMethodInsn(
                    INVOKESTATIC, "python/_core/Py", "iter",
                    "(Ljava/lang/Object;)Lpython/_core/PyObject;", false
            );

            int iterVar = nextVar();
            asmStoreVar(iterVar);

            mv.visitLabel(startLoop);

            // Try-catch for StopIteration
            Label tryStart = new Label();
            Label tryEnd = new Label();
            Label endTry = new Label();
            Label catchBlock = new Label();
            mv.visitTryCatchBlock(tryStart, tryEnd, catchBlock, "python/builtins/StopIteration");

            mv.visitLabel(tryStart);
            // Assign to target variable
            if (gen.target instanceof AstParser.Name) {
                AstParser.Name nameTarget = (AstParser.Name) gen.target;
                lineBarrier(nameTarget);
                asmSetLocal(
                        nameTarget.id, () -> {
                            asmLoadVar(iterVar);
                            mv.visitMethodInsn(
                                    INVOKESTATIC, "python/_core/Py", "next",
                                    "(Ljava/lang/Object;)Ljava/lang/Object;", false
                            );
                        }
                );
            } else {
                asmLoadVar(iterVar);
                mv.visitMethodInsn(
                        INVOKESTATIC, "python/_core/Py", "next",
                        "(Ljava/lang/Object;)Ljava/lang/Object;", false
                );
                mv.visitInsn(POP);
            }

            mv.visitLabel(tryEnd);
            mv.visitJumpInsn(GOTO, endTry);

            mv.visitLabel(catchBlock);
            mv.visitJumpInsn(GOTO, endLoop);
            mv.visitLabel(endTry);

            // --- Step 3: filters ---
            Label skipAdd = new Label();
            if (gen.ifs != null && !gen.ifs.isEmpty()) {
                for (AstParser.AstNode cond : gen.ifs) {
                    emit(cond);
                    // ensure bool unboxed
                    mv.visitMethodInsn(
                            INVOKESTATIC, "python/_core/Py", "toBoolean",
                            "(Ljava/lang/Object;)Z", false
                    );
                    mv.visitJumpInsn(IFEQ, skipAdd);
                }
            }

            // --- Step 4: add element ---
            asmLoadVar(listVar);
            emit(node.elt);
            mv.visitMethodInsn(
                    INVOKEVIRTUAL, "python/builtins/List",
                    "append",
                    "(Lpython/builtins/Object;)Lpython/builtins/Object;", false
            );
            mv.visitInsn(POP);

            if (gen.ifs != null && !gen.ifs.isEmpty())
                mv.visitLabel(skipAdd);

            mv.visitJumpInsn(GOTO, startLoop);
            mv.visitLabel(endLoop);
        }

        // --- Step 5: leave list on stack ---
        asmLoadVar(listVar);
    }

    private void lineBarrier(AstParser.AstNode node) {
        int lineno = getLineNo(node);
        if (lineno > 0 && lineno != curLineNo) {
            curLineNo = lineno;
            LabelNode start;
            AbstractInsnNode lastInsn = methodStack.peek().instructions.getLast();
            if (lastInsn instanceof LabelNode) {
                start = (LabelNode) lastInsn;
            } else {
                start = new LabelNode();
                asmLabel(start);
            }
            methodStack.peek().instructions.add(new LineNumberNode(lineno, start));
        }
    }

    private int nextVar() {
        return varIndexStack.push(varIndexStack.pop() + 1);
    }

    private void emitSetComp(AstParser.SetComp node) {
        MethodNode mv = methodStack.peek();
        if (mv == null)
            throw new IllegalStateException("No active method in methodStack");

        // --- Step 1: Create python.builtins.Set instance ---
        mv.visitTypeInsn(NEW, "python/builtins/Set");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "python/builtins/Set", "<init>", "()V", false);

        int setVar = nextVar();
        asmStoreVar(setVar);

        // --- Step 2: Loop over each generator ---
        for (AstParser.Comprehension gen : node.generators) {
            Label startLoop = new Label();
            Label endLoop = new Label();

            // Compile the iterable expression
            emit(gen.iter);

            // Get iterator
            mv.visitMethodInsn(
                    INVOKESTATIC, "python/_core/Py", "iter",
                    "(Ljava/lang/Object;)Lpython/_core/PyObject;", false
            );

            int iterVar = nextVar();
            asmStoreVar(iterVar);

            mv.visitLabel(startLoop);

            // Try-catch for StopIteration
            Label tryStart = new Label();
            Label tryEnd = new Label();
            Label endTry = new Label();
            Label catchBlock = new Label();
            mv.visitTryCatchBlock(tryStart, tryEnd, catchBlock, "python/builtins/StopIteration");

            mv.visitLabel(tryStart);
            // Assign to target variable
            if (gen.target instanceof AstParser.Name) {
                AstParser.Name nameTarget = (AstParser.Name) gen.target;
                lineBarrier(nameTarget);
                asmSetLocal(
                        nameTarget.id, () -> {
                            asmLoadVar(iterVar);
                            mv.visitMethodInsn(
                                    INVOKESTATIC, "python/_core/Py", "next",
                                    "(Ljava/lang/Object;)Ljava/lang/Object;", false
                            );
                        }
                );
            } else {
                asmLoadVar(iterVar);
                mv.visitMethodInsn(
                        INVOKESTATIC, "python/_core/Py", "next",
                        "(Ljava/lang/Object;)Ljava/lang/Object;", false
                );
                mv.visitInsn(POP);
            }

            mv.visitLabel(tryEnd);
            mv.visitJumpInsn(GOTO, endTry);

            mv.visitLabel(catchBlock);
            mv.visitJumpInsn(GOTO, endLoop);
            mv.visitLabel(endTry);

            // --- Step 3: Filters ---
            Label skipAdd = new Label();
            if (gen.ifs != null && !gen.ifs.isEmpty()) {
                for (AstParser.AstNode cond : gen.ifs) {
                    emit(cond);
                    mv.visitMethodInsn(
                            INVOKESTATIC, "python/_core/Py", "toBoolean",
                            "(Ljava/lang/Object;)Z", false
                    );
                    mv.visitJumpInsn(IFEQ, skipAdd);
                }
            }

            // --- Step 4: Add to set ---
            asmLoadVar(setVar);
            emit(node.elt);
            mv.visitMethodInsn(
                    INVOKEVIRTUAL, "python/builtins/Set",
                    "add", "(Lpython/builtins/Object;)Lpython/builtins/Object;", false
            );
            mv.visitInsn(POP);

            if (gen.ifs != null && !gen.ifs.isEmpty())
                mv.visitLabel(skipAdd);

            mv.visitJumpInsn(GOTO, startLoop);
            mv.visitLabel(endLoop);
        }

        // --- Step 5: Leave set on stack ---
        asmLoadVar(setVar);
    }

    private void emitDictComp(AstParser.DictComp node) {
        MethodNode mv = methodStack.peek();
        if (mv == null)
            throw new IllegalStateException("No active method in methodStack");

        // --- Step 1: Create python.builtins.Dict instance ---
        mv.visitTypeInsn(NEW, "python/builtins/Dict");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "python/builtins/Dict", "<init>", "()V", false);

        int dictVar = nextVar();
        asmStoreVar(dictVar);

        // --- Step 2: Loop over generators ---
        for (AstParser.Comprehension gen : node.generators) {
            LabelNode startLoop = new LabelNode();
            LabelNode endLoop = new LabelNode();

            emit(gen.iter);
            asmIter();

            int iterVar = nextVar();
            asmStoreVar(iterVar);
            asmLabel(startLoop);
            asmTry(() -> {
                // Assign to target variable
                if (gen.target instanceof AstParser.Name) {
                    AstParser.Name nameTarget = (AstParser.Name) gen.target;
                    lineBarrier(nameTarget);
                    asmSetLocal(
                            nameTarget.id, () -> {
                                asmLoadVar(iterVar);
                                asmNext();
                            }
                    );
                } else {
                    asmLoadVar(iterVar);
                    asmNext();
                    mv.visitInsn(POP);
                }
            }).asmCatch(
                    Type.getObjectType("python/builtins/StopIteration"),
                    () -> asmGoto(endLoop)
            ).asmEnd();

            // --- Step 3: Filters ---
            var skipPut = new LabelNode();
            if (gen.ifs != null && !gen.ifs.isEmpty()) {
                for (AstParser.AstNode cond : gen.ifs) {
                    emit(cond);
                    asmJumpIf(skipPut);
                }
            }

            // --- Step 4: Add to dict (key:value) ---
            int i = nextVar();
            setItem(
                    () -> asmLoadVar(dictVar),
                    () -> {
                        set(node.key, () -> asmLoadVar(dictVar));
                        asmLoadVar(dictVar);
                    },
                    () -> {
                        set(node.value, () -> asmLoadVar(dictVar));
                        asmLoadVar(dictVar);
                    }
            );
            if (gen.ifs != null && !gen.ifs.isEmpty())
                asmLabel(skipPut);

            asmGoto(startLoop);
            asmLabel(endLoop);
        }

        // --- Step 5: Leave dict on stack ---
        asmLoadVar(dictVar);
    }

    private void asmJumpIf(LabelNode skipPut) {
        asmJBool();
        asmJJumpIf(skipPut);
    }

    private void asmJJumpIf(LabelNode skipPut) {
        methodStack.peek().instructions.add(new JumpInsnNode(IFEQ, skipPut));
    }

    private void asmJumpIfNot(LabelNode skipPut) {
        asmJBool();
        asmJJumpIfNot(skipPut);
    }

    private void asmJJumpIfNot(LabelNode skipPut) {
        methodStack.peek().instructions.add(new JumpInsnNode(IFNE, skipPut));
    }

    private void asmJBool() {
        methodStack.peek().instructions.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "python/_core/Py",
                "toBoolean",
                "(Ljava/lang/Object;)Z",
                false
        ));
    }

    private void asmIter() {
        methodStack.peek().instructions.add(new MethodInsnNode(
                INVOKESTATIC,
                "python/_core/Py",
                "iter",
                "(Ljava/lang/Object;)Lpython/_core/PyObject;",
                false
        ));
    }

    private void asmNext() {
        methodStack.peek().instructions.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "python/_core/Py",
                "next",
                "(Ljava/lang/Object;)Ljava/lang/Object;",
                false
        ));
    }

    private void tryCatch(Runnable trying, Runnable onAnyError) {
        LabelNode start = new LabelNode();
        LabelNode end = new LabelNode();
        LabelNode catchBlock = new LabelNode();
        LabelNode endTry = new LabelNode();

        asmLabel(start);
        trying.run();
        asmLabel(end);
        asmGoto(endTry);

        asmLabel(catchBlock);
        pop();
        onAnyError.run();
        asmLabel(endTry);

        tryCatchBlock(start, end, catchBlock, "java/lang/Throwable");
    }

    private CaptureBuilder asmTry(Runnable trying) {
        return new CaptureBuilder(trying);
    }

    private class CaptureBuilder {
        private final Runnable trying;
        private Runnable onElse;
        private final List<Capture> onCatch = new ArrayList<>();

        public CaptureBuilder(Runnable trying) {
            this.trying = trying;
        }

        public CaptureBuilder asmCatch(Type errorType, Runnable onCatch) {
            this.onCatch.add(new Capture(errorType, onCatch));
            return this;
        }

        public CaptureBuilder finally_(Runnable onFinally) {
            onCatch.add(new Capture(null, onFinally));
            return this;
        }

        public CaptureBuilder else_(Runnable onElse) {
            this.onElse = onElse;
            return this;
        }

        public AstCompiler asmEnd() {
            LabelNode start = new LabelNode();
            LabelNode end = new LabelNode();
            LabelNode endTry = new LabelNode();

            asmLabel(start);
            this.trying.run();
            AbstractInsnNode lastInsn = methodStack.peek().instructions.getLast();
            if (lastInsn instanceof LabelNode) {
                LabelNode label = (LabelNode) lastInsn;
                if (label == start) {
                    System.err.println("WARNING: Empty try block");
                    pushNull();
                    pop();
                }
            }
            asmLabel(end);

            if (this.onElse != null) {
                this.onElse.run();
            }
            asmGoto(endTry);

            for (Capture c : this.onCatch) {
                LabelNode catchBlock = new LabelNode();
                tryCatchBlock(start, end, catchBlock, c.errorType == null ? null : c.errorType.getInternalName());
                asmLabel(catchBlock);
                c.onCatch.run();
            }

            asmLabel(endTry);

            return AstCompiler.this;
        }

        private class Capture {

            private final Type errorType;
            private final Runnable onCatch;

            public Capture(Type errorType, Runnable onCatch) {
                this.errorType = errorType;
                this.onCatch = onCatch;
            }

        }
    }

    private void pushNull() {
        methodStack.peek().instructions.add(new InsnNode(ACONST_NULL));
    }

    private void tryCatch(Runnable trying, Runnable onError, String errorType) {
        LabelNode start = new LabelNode();
        LabelNode end = new LabelNode();
        LabelNode catchBlock = new LabelNode();
        LabelNode endTry = new LabelNode();

        asmLabel(start);
        trying.run();
        asmLabel(end);
        asmGoto(endTry);

        asmLabel(catchBlock);
        pop();
        onError.run();
        asmLabel(endTry);

        tryCatchBlock(start, end, catchBlock, errorType);
    }

    private void pop() {
        methodStack.peek().instructions.add(new InsnNode(Opcodes.POP));
    }

    private void setItem(Runnable owner, Runnable key, Runnable value) {
        owner.run();
        key.run();
        value.run();

        methodStack.peek().instructions.add(new MethodInsnNode(
                INVOKESTATIC, "python/_core/Py", "setItem",
                "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V", false
        ));
    }

    private void set(AstParser.AstNode target, int varIndex) {
        MethodVisitor mv = methodStack.peek();
        if (target instanceof AstParser.Name) {
            AstParser.Name name = (AstParser.Name) target;
            asmLoadVar(varIndex);
            asmSetLocal(name.id);
        } else if (target instanceof AstParser.Attribute) {
            AstParser.Attribute attr = (AstParser.Attribute) target;
            emit(attr.value);
            mv.visitLdcInsn(attr.attr);
            asmLoadVar(varIndex);
            mv.visitMethodInsn(
                    INVOKESTATIC, "python/_core/Py",
                    "setAttr", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)Lpython/_core/PyObject;",
                    false
            );
//        } else if (target instanceof AstParser.Subscript) {
//            AstParser.Subscript subscript = (AstParser.Subscript) target;
//            emit(subscript.value);
//            mv.visitInsn(DUP);
//            mv.visitFieldInsn(PUTFIELD, subscript.owner, subscript.index, "Lpython/builtins/Object;");
        } else {
            throw new IllegalStateException("Cannot set non-name target");
        }
    }

    private void set(AstParser.AstNode target, Runnable block) {
        MethodVisitor mv = methodStack.peek();
        if (target instanceof AstParser.Name) {
            AstParser.Name name = (AstParser.Name) target;
            asmSetLocal(name.id, block);
        } else if (target instanceof AstParser.Attribute) {
            AstParser.Attribute attr = (AstParser.Attribute) target;
            emit(attr.value);
            mv.visitLdcInsn(attr.attr);
            block.run();
            mv.visitMethodInsn(
                    INVOKESTATIC, "python/_core/Py",
                    "setAttr", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)Lpython/_core/PyObject;",
                    false
            );
        } else if (target instanceof AstParser.Tuple) {
            int i = nextVar();
            block.run();
            asmStoreVar(i);
            AstParser.Tuple tuple = (AstParser.Tuple) target;
            List<AstParser.AstNode> elts = tuple.elts;
            for (int j = 0, eltsSize = elts.size(); j < eltsSize; j++) {
                AstParser.AstNode item = elts.get(j);
                final int idx = j;
                set(item, () -> getItem(() -> emit(item), () -> constant(idx)));
            }
//        } else if (target instanceof AstParser.Subscript) {
//            AstParser.Subscript subscript = (AstParser.Subscript) target;
//            emit(subscript.value);
//            mv.visitInsn(DUP);
//            mv.visitFieldInsn(PUTFIELD, subscript.owner, subscript.index, "Lpython/builtins/Object;");
        } else {
            throw new CompilerException("Cannot set non-name target: " + target);
        }
    }

    private void getItem(Runnable owner, Runnable index) {
        owner.run();
        index.run();
        methodStack.peek().instructions.add(new MethodInsnNode(
                INVOKESTATIC, "python/_core/Py",
                "getItem", "(Ljava/lang/Object;Ljava/lang/Object;)Lpython/_core/PyObject;",
                false
        ));
    }

    private void emitCompare(AstParser.Compare node) {
        emit(node.left);
        List<AstParser.AstNode> comparators = node.comparators;
        for (int i = 0, comparatorsSize = comparators.size(); i < comparatorsSize; i++) {
            AstParser.AstNode comparator = comparators.get(i);
            AstParser.Op op = node.ops.get(i);
            emit(comparator);
            if (op instanceof AstParser.Eq) {
                methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "eq", "(Ljava/lang/Object;Ljava/lang/Object;)Lpython/builtins/Bool;", false));
            } else if (op instanceof AstParser.Lt) {
                methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "lt", "(Ljava/lang/Object;Ljava/lang/Object;)Lpython/builtins/Bool;", false));
            } else if (op instanceof AstParser.Gt) {
                methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "gt", "(Ljava/lang/Object;Ljava/lang/Object;)Lpython/builtins/Bool;", false));
            } else if (op instanceof AstParser.LtE) {
                methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "lte", "(Ljava/lang/Object;Ljava/lang/Object;)Lpython/builtins/Bool;", false));
            } else if (op instanceof AstParser.GtE) {
                methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "gte", "(Ljava/lang/Object;Ljava/lang/Object;)Lpython/builtins/Bool;", false));
            } else {
                System.err.println("Warning: Unknown comparison operator: " + op.getClass().getName());
                methodStack.peek().instructions.add(new InsnNode(Opcodes.ACONST_NULL));
            }
        }
    }

    private void emitComprehension(AstParser.Comprehension node) {throw new CompilerException("Not Implemented!");}

    private void emitMatch(AstParser.Match node) {
        MethodNode mv = methodStack.peek();
        if (mv == null)
            throw new IllegalStateException("No active method in methodStack");

        // --- Step 1: Evaluate the subject and store in local ---
        emit(node.subject);
        int subjectVar = nextVar();
        asmStoreVar(subjectVar);

        // --- Step 2: Loop over cases ---
        Label endMatch = new Label();
        for (AstParser.AstNode cNode : node.cases) {
            AstParser.match_case caseNode = (AstParser.match_case) cNode;
            Label nextCase = new Label();

            // --- Step 2a: Check pattern ---
            emitMatchCasePattern(caseNode.pattern, subjectVar, nextCase);

            // --- Step 2b: Guard check ---
            if (caseNode.guard != null) {
                emit(caseNode.guard);
                mv.visitMethodInsn(
                        INVOKESTATIC, "python/_core/Py", "toBoolean",
                        "(Ljava/lang/Object;)Z", false
                );
                mv.visitJumpInsn(IFEQ, nextCase);
            }

            // --- Step 2c: Case body ---
            for (AstParser.AstNode stmt : caseNode.body) {
                emit(stmt);
            }

            mv.visitJumpInsn(GOTO, endMatch);
            mv.visitLabel(nextCase);
        }

        mv.visitLabel(endMatch);
    }

    private void emitMatchCasePattern(AstParser.AstNode pattern, int subjectVar, Label nextCase) {
        MethodNode mv = methodStack.peek();

        if (pattern instanceof AstParser.MatchValue) {
            // Load subject
            asmLoadVar(subjectVar);

            // Evaluate pattern value
            emit(((AstParser.MatchValue) pattern).value);

            // Call equality check
            mv.visitMethodInsn(
                    INVOKESTATIC, "python/_core/Py", "eq",
                    "(Ljava/lang/Object;Ljava/lang/Object;)Z", false
            );

            // If not equal, jump to next case
            mv.visitJumpInsn(IFEQ, nextCase);
        } else if (pattern instanceof AstParser.MatchAs) {
            AstParser.MatchAs matchAs = (AstParser.MatchAs) pattern;
            if (matchAs.name != null) {
                // Assign subject to variable
                asmLoadVar(subjectVar);
                asmSetLocal(matchAs.name);
            }
            // Always matches, no jump
        } else {
            throw new CompilerException("Unsupported pattern type: " + pattern.getClass());
        }
    }

    private void emitConstant(AstParser.Constant node) {
        Object value = node.value;
        if (value instanceof String) {
            methodStack.peek().instructions.add(new LdcInsnNode(value));
            methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "createString", "(Ljava/lang/String;)Lpython/builtins/Str;", false));
        } else if (value instanceof Integer) {
            constant((Integer) value);
            methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "createInt", "(I)Lpython/builtins/Int;", false));
        } else if (value instanceof Long) {
            constant((Long) value);
            methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "createInt", "(J)Lpython/builtins/Int;", false));
        } else if (value instanceof Float) {
            constant((Float) value);
            methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "createFloat", "(F)Lpython/builtins/Float;", false));
        } else if (value instanceof Double) {
            constant((Double) value);
            methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "createFloat", "(D)Lpython/builtins/Float;", false));
        } else if (value instanceof Boolean) {
            constant((Boolean) value);
            methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "createBool", "(Z)Lpython/builtins/Bool;", false));
        } else if (value == null) {
            System.err.println("Warning: Null constant value");
            methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "createNone", "()Lpython/builtins/None;", false));
        } else {
            System.err.println("Warning: Unknown constant type: " + value.getClass().getName());
            methodStack.peek().instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        }
    }

    private void emitName(AstParser.Name node) {
        String name = node.id;
        if ((methodStack.peek().access & Opcodes.ACC_STATIC) == 0) {
            asmLoadVar(Type.getArgumentCount(methodStack.peek().desc) + 1);
        } else {
            asmLoadVar(0);
        }
        methodStack.peek().instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, "python/_core/PyLocals"));
        methodStack.peek().instructions.add(new LdcInsnNode(name));
        methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "python/_core/PyLocals", "get", "(Ljava/lang/String;)Ljava/lang/Object;", false));
        methodStack.peek().instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, "python/_core/VMObject"));
    }

    private void emitAttribute(AstParser.Attribute node) {
        emit(node.value);
        methodStack.peek().instructions.add(new LdcInsnNode(node.attr));
        methodStack.peek().instructions.add(new MethodInsnNode(INVOKEINTERFACE, "python/_core/VMObject", "getAttr", "(Ljava/lang/String;)Lpython/_core/VMObject;"));
    }

    private void emitJoinedStr(AstParser.JoinedStr node) {
        MethodNode mv = methodStack.peek();
        if (mv == null) throw new IllegalStateException("No active method");

        // Create new python/builtins/StrBuilder (similar to StringBuilder)
        mv.visitTypeInsn(Opcodes.NEW, "python/builtins/StrBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "python/builtins/StrBuilder", "<init>", "()V", false);

        for (AstParser.AstNode part : node.values) {
            if (part instanceof AstParser.Constant && ((AstParser.Constant) part).value instanceof String) {
                AstParser.Constant c = (AstParser.Constant) part;
                // Append literal string
                mv.visitInsn(Opcodes.DUP);
                emitConstant(c); // pushes a python/builtins/Str
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "python/builtins/StrBuilder", "append", "(Lpython/_core/VMObject;)Lpython/builtins/StrBuilder;", false);
                mv.visitInsn(Opcodes.POP); // discard return of append
            } else if (part instanceof AstParser.FormattedValue) {
                AstParser.FormattedValue f = (AstParser.FormattedValue) part;
                // Append formatted value
                mv.visitInsn(Opcodes.DUP);
                emitFormattedValue(f);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "python/builtins/StrBuilder", "append", "(Lpython/_core/VMObject;)Lpython/builtins/StrBuilder;", false);
                mv.visitInsn(Opcodes.POP);
            } else {
                throw new CompilerException("Unexpected node in JoinedStr: " + part);
            }
        }

        // Convert StrBuilder → Str
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "python/builtins/StrBuilder", "toStr", "()Lpython/builtins/Str;", false);
    }

    private void emitFormattedValue(AstParser.FormattedValue node) {
        MethodNode mv = methodStack.peek();
        if (mv == null) throw new IllegalStateException("No active method");

        // Evaluate the expression inside {}
        emit(node.value); // pushes VMObject

        // Convert it to string
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "python/_core/Py", "str", "(Lpython/_core/VMObject;)Lpython/builtins/Str;", false);

        // Handle conversion flag (!r, !s, !a)
        switch (node.conversion) {
            case 114:// 'r'
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "python/_core/Py", "repr", "(Lpython/_core/VMObject;)Lpython/builtins/Str;", false);
                break;
            case 115:// 's'
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "python/_core/Py", "str", "(Lpython/_core/VMObject;)Lpython/builtins/Str;", false);
                break;
            case 97:// 'a'
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "python/_core/Py", "ascii", "(Lpython/_core/VMObject;)Lpython/builtins/Str;", false);
                break;
        }

        // Apply format spec if available (e.g., f"{x:.2f}")
        if (node.format_spec != null) {
            // Swap: value, format_spec
            mv.visitInsn(Opcodes.SWAP);
            emit(node.format_spec);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "python/_core/Py", "format", "(Lpython/_core/VMObject;Lpython/_core/VMObject;)Lpython/builtins/Str;", false);
        }
    }

    private void emitCall(AstParser.Call node) {
        emit(node.func);

        constant(node.args.size());
        methodStack.peek().instructions.add(new TypeInsnNode(Opcodes.ANEWARRAY, "python/_core/VMObject"));
        List<AstParser.AstNode> args = node.args;
        for (int i = 0, argsSize = args.size(); i < argsSize; i++) {
            methodStack.peek().instructions.add(new InsnNode(Opcodes.DUP));
            AstParser.AstNode arg = args.get(i);
            constant(i);
            emit(arg);
            methodStack.peek().instructions.add(new InsnNode(Opcodes.AASTORE));
        }
        methodStack.peek().instructions.add(new MethodInsnNode(
                INVOKESTATIC, "python/_core/Py", "createTuple", "([Lpython/_core/VMObject;)Lpython/builtins/Tuple;", false));

        methodStack.peek().instructions.add(new TypeInsnNode(Opcodes.NEW, "python/builtins/Dict"));
        methodStack.peek().instructions.add(new InsnNode(Opcodes.DUP));
        methodStack.peek().instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "python/builtins/Dict", "<init>", "()V", false));
        for (AstParser.keyword arg : node.keywords) {
            if (!(arg instanceof AstParser.keyword)) {
                throw new CompilerException("Expected keyword argument, got: " + arg.getClass().getName());
            }
            String arg1 = arg.arg;
            if (arg1 == null) {
                emit(arg.value);
                methodStack.peek().instructions.add(new InsnNode(Opcodes.DUP));
                methodStack.peek().instructions.add(new MethodInsnNode(INVOKESTATIC, "python/_core/Py", "extendDict", "(Lpython/builtins/Dict;)V", false));
                continue;
            }
            emit(arg.value);
            methodStack.peek().instructions.add(new InsnNode(Opcodes.DUP));
            methodStack.peek().instructions.add(new LdcInsnNode(arg1));
            methodStack.peek().instructions.add(new MethodInsnNode(INVOKESTATIC, "python/_core/Py", "createString", "(Ljava/lang/String;)Lpython/builtins/Str;", false));
            methodStack.peek().instructions.add(new MethodInsnNode(INVOKESTATIC, "python/_core/PyDict", "setItem", "(Lpython/builtins/Dict;Lpython/builtins/Str;Lpython/_core/VMObject;)V"));
        }
        methodStack.peek().instructions.add(new MethodInsnNode(INVOKESTATIC, "python/_core/Py", "call", "(Lpython/_core/VMObject;[Lpython/_core/VMObject;[Ljava/lang/String;)Lpython/_core/VMObject;", false));
    }
    private void emitSubscript(AstParser.Subscript node) {
        MethodNode mv = methodStack.peek();
        if (mv == null) throw new IllegalStateException("No active method in methodStack");

        // Evaluate the slice or index
        if (node.slice instanceof AstParser.Slice) {
            AstParser.Slice slice = (AstParser.Slice) node.slice;
            // Stack: [container, slice]
            getItem(() -> emit(node.value), () -> emitSlice(slice));
        } else {
            getItem(() -> emit(node.value), () -> emit(node.slice));
        }
    }
    private void emitSlice(AstParser.Slice node) {
        MethodNode mv = methodStack.peek();
        if (mv == null) throw new IllegalStateException("No active method");

        // --- Push lower ---
        if (node.lower != null) {
            emit(node.lower);
        } else {
            mv.visitInsn(Opcodes.ACONST_NULL);
        }

        // --- Push upper ---
        if (node.upper != null) {
            emit(node.upper);
        } else {
            mv.visitInsn(Opcodes.ACONST_NULL);
        }

        // --- Push step ---
        if (node.step != null) {
            emit(node.step);
        } else {
            mv.visitInsn(Opcodes.ACONST_NULL);
        }

        // --- Create slice object ---
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "python/_core/Py",
                "slice",
                "(Lpython/_core/VMObject;Lpython/_core/VMObject;Lpython/_core/VMObject;)Lpython/_core/VMObject;",
                false
        );
    }

    private static class LoopContext {
        LabelNode start = new LabelNode();
        LabelNode end = new LabelNode();
        LabelNode continueLabel = new LabelNode();
        LabelNode breakLabel = new LabelNode();
    }
}
