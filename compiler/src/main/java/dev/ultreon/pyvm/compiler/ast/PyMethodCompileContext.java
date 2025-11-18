package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyClassCompileContext;
import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import dev.ultreon.pyvm.compiler.context.PyFileCompileContext;
import dev.ultreon.pyvm.compiler.util.Variable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public class PyMethodCompileContext extends PyCompileContext {
    private final MethodNode method;
    private final ClassNode classNode;
    private final PyFileCompileContext context;
    private final int localsVar;
    private int nextVar;
    private PyClassCompileContext classContext;
    private final LabelNode endLabel = new LabelNode();

    public PyMethodCompileContext(MethodNode method, ClassNode classNode, PyFileCompileContext context, PyClassCompileContext classContext) {
        this.method = method;
        this.classNode = classNode;
        this.context = context;
        this.classContext = classContext;

        if ((method().access & Opcodes.ACC_STATIC) == 0) {
            this.nextVar = 1;
        } else {
            this.nextVar = 0;
        }

        for (Type t : Type.getArgumentTypes(method.desc)) {
            this.nextVar++;
        }

        this.localsVar = nextVar++;

        LabelNode startLabel = new LabelNode();
        method().instructions.add(startLabel);
        method().instructions.add(new TypeInsnNode(Opcodes.NEW, "python/_core/PyLocals"));
        method().instructions.add(new InsnNode(Opcodes.DUP));
        context.writeGlobalsRef(method());
        method().instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "python/_core/PyLocals", "<init>", "(Lpython/_core/PyGlobals;)V", false));
        method().instructions.add(new VarInsnNode(Opcodes.ASTORE, localsVar));

        method().localVariables.add(new LocalVariableNode("$$locals", "Lpython/_core/PyLocals;", null, startLabel, endLabel, localsVar));
    }

    public void finish() {
        method().instructions.add(endLabel);
    }

    @Override
    public ClassNode classNode() {
        return classNode;
    }

    @Override
    protected MethodNode method() {
        return this.method;
    }

    @Override
    public Variable createVar(String name) {
        return new Variable(name, this.nextVar++);
    }

    @Override
    public PyFileCompileContext fileContext() {
        return context;
    }

    @Override
    protected int localsVar() {
        return localsVar;
    }

    @Override
    public void getLocal(String text) {
        if (text == null) throw new NullPointerException("text");

        method().instructions.add(new VarInsnNode(Opcodes.ALOAD, localsVar()));
        method().instructions.add(new LdcInsnNode(text));
        method().instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "python/_core/PyLocals", "get", "(Ljava/lang/String;)Ljava/lang/Object;", false));

    }

    @Override
    public void setLocal(String text, PyExprNode expr) {
        if (text == null) throw new NullPointerException("text");

        method().instructions.add(new VarInsnNode(Opcodes.ALOAD, localsVar()));
        method().instructions.add(new LdcInsnNode(text));
        expr.compile(method(), this);
        method().instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "python/_core/PyLocals", "set", "(Ljava/lang/String;Ljava/lang/Object;)V", false));
    }

    @Override
    public void setLocal(String text, Type asmType, PyExprNode expr) {
        if (text == null) throw new NullPointerException("text");

        method().instructions.add(new VarInsnNode(Opcodes.ALOAD, localsVar()));
        method().instructions.add(new LdcInsnNode(text));
        expr.compile(method(), this);
        method().instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "python/_core/PyLocals", "set", "(Ljava/lang/String;Ljava/lang/Object;)V", false));
    }

    @Override
    public PyClassCompileContext classContext() {
        return classContext;
    }

    @Override
    public PyMethodCompileContext getMethodContext(MethodNode method) {
        return new PyMethodCompileContext(method, classNode, context, classContext);
    }
}
