package dev.ultreon.pyvm.compiler.context;

import dev.ultreon.pyvm.compiler.ast.PyExprNode;
import dev.ultreon.pyvm.compiler.ast.PyMethodCompileContext;
import dev.ultreon.pyvm.compiler.util.PyClass;
import dev.ultreon.pyvm.compiler.util.PyFunction;
import dev.ultreon.pyvm.compiler.util.Variable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PyFileCompileContext extends PyCompileContext {
    private final String sourceFileName;
    private final String className;
    private final ClassNode classNode;
    private final List<PyClass> classes = new ArrayList<>();
    private final List<PyFunction> function = new ArrayList<>();
    private final MethodNode classInit;
    private final MethodNode init;
    private int nextVar;
    private int counter;

    public PyFileCompileContext(String sourceFileName, String className, ClassNode classNode) {
        this.sourceFileName = sourceFileName;
        this.className = className;
        this.classNode = classNode;
        classNode.superName = "python/types/ModuleType";
        classNode.version = Opcodes.V1_8;
        classNode.access = Opcodes.ACC_PUBLIC;
        classNode.name = className.replace('.', '/');
        classNode.visibleAnnotations = new ArrayList<>();
        AnnotationNode e = new AnnotationNode("Lpython/_core/PyModuleMeta;");
        e.values = new ArrayList<>();
        e.values.add("name");
        e.values.add(className);
        e.values.add("source");
        e.values.add(sourceFileName);
        e.values.add("parent");
        String[] split = className.split("\\.");
        if (split.length > 1) {
            List<String> strings = Arrays.asList(split).subList(0, split.length - 1);
            String join = String.join("/", strings);
            e.values.add(Type.getObjectType(join + "/__Init__"));
        } else {
            e.values.add(Type.getObjectType("python/types/ModuleType"));
        }
        if (sourceFileName.equals("__init__.py"))
            classNode.name = className.replace('.', '/').substring(0, className.lastIndexOf('.')) + "/__Init__";
        classNode.visibleAnnotations.add(e);
        classNode.fields.add(new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "INSTANCE", "L" + className.replace('.', '/') + ";", null, null));
        this.classInit = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
        this.classNode.methods.add(this.classInit);

        classInit.instructions.add(new TypeInsnNode(Opcodes.NEW, className.replace('.', '/')));
        classInit.instructions.add(new InsnNode(Opcodes.DUP));
        classInit.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, className.replace('.', '/'), "<init>", "()V", false));
        classInit.instructions.add(new InsnNode(Opcodes.DUP));
        classInit.instructions.add(new FieldInsnNode(Opcodes.PUTSTATIC, this.classNode.name, "INSTANCE", "L" + this.classNode.name + ";"));
        classInit.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "registerModule", "(Lpython/types/ModuleType;)V", false));
        classInit.instructions.add(new InsnNode(Opcodes.RETURN));

        this.init = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        this.classNode.methods.add(this.init);
        this.init.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        this.init.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "python/types/ModuleType", "<init>", "()V", false));

        MethodNode main = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
        this.classNode.methods.add(main);
        main.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        main.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "init", "([Ljava/lang/String;)V", false));
        main.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, this.classNode.name, "INSTANCE", "L" + this.classNode.name + ";"));
        main.instructions.add(new LdcInsnNode("__name__"));
        main.instructions.add(new LdcInsnNode("__main__"));
        main.instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "python/_core/VMObject", "setAttr", "(Ljava/lang/String;Lpython/_core/PyObject;)V", true));
        main.instructions.add(new InsnNode(Opcodes.RETURN));
    }

    public String getSourceFileName() {
        return this.sourceFileName;
    }

    public String getClassName() {
        return this.className;
    }

    public ClassNode getClassNode() {
        return this.classNode;
    }

    public void finish(Path outputDir) {
        init.instructions.add(new InsnNode(Opcodes.RETURN));
        classNode.methods.add(init);

        for (PyClass pyClass : this.classes) {
            pyClass.finish(outputDir);
        }

    }

    public void writeGlobalsRef(MethodNode method) {
        method.visitFieldInsn(Opcodes.GETSTATIC, this.classNode.name, "__globals__", "Lpython/_core/PyGlobals;");
    }

    public MethodNode getClassInit() {
        return init;
    }

    public PyClassCompileContext createClassContext() {
        return new PyClassCompileContext(this, this.classNode);
    }

    @Override
    public PyFileCompileContext fileContext() {
        return this;
    }

    @Override
    public ClassNode classNode() {
        return null;
    }

    @Override
    protected MethodNode method() {
        return init;
    }

    @Override
    public Variable createVar(String name) {
        return new Variable(name, this.nextVar++);
    }

    @Override
    protected int localsVar() {
        throw new UnsupportedOperationException("Cannot get locals var in a file context");
    }

    @Override
    public void getLocal(String text) {
        method().instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method().instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "python/_core/VMObject", "$", "()Lpython/_core/PyObject;", true));
        method().instructions.add(new LdcInsnNode(text));
        method().instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "python/_core/PyObject", "getAttr", "(Ljava/lang/String;)Lpython/_core/PyObject;", false));
    }

    @Override
    public void setLocal(String text, PyExprNode expr) {
        expr.compile(method(), this);
        method().instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method().instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "python/_core/VMObject", "$", "()Lpython/_core/PyObject;", true));
        method().instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method().instructions.add(new LdcInsnNode(text));
        method().instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "python/_core/PyObject", "setAttr", "(Ljava/lang/String;Lpython/_core/PyObject;)V", false));
    }

    @Override
    public void setLocal(String text, Type asmType, PyExprNode expr) {
        expr.compile(method(), this);
        method().instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method().instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "python/_core/VMObject", "$", "()Lpython/_core/PyObject;", true));
        method().instructions.add(new VarInsnNode(asmType.getOpcode(Opcodes.ILOAD), 0));
        method().instructions.add(new LdcInsnNode(text));
        method().instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "python/_core/PyObject", "setAttr", "(Ljava/lang/String;Lpython/_core/PyObject;)V", false));
    }

    public int getCounter() {
        return counter++;
    }

    @Override
    public PyClassCompileContext classContext() {
        throw new UnsupportedOperationException("Cannot get class context in a file context");
    }

    @Override
    public PyMethodCompileContext getMethodContext(MethodNode method) {
        return null;
    }
}
