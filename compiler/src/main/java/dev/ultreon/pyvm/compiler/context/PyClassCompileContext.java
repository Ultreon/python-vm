package dev.ultreon.pyvm.compiler.context;

import dev.ultreon.pyvm.compiler.util.Variable;
import dev.ultreon.pyvm.compiler.ast.PyExprNode;
import dev.ultreon.pyvm.compiler.ast.PyMethodCompileContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class PyClassCompileContext extends PyCompileContext {
    private final PyFileCompileContext fileContext;
    private final ClassNode classNode;
    private final MethodNode classInit;

    PyClassCompileContext(PyFileCompileContext fileContext, ClassNode classNode) {
        this.fileContext = fileContext;
        this.classNode = classNode;
        this.classInit = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        this.classInit.visitCode();
        this.classInit.visitVarInsn(Opcodes.ALOAD, 0);
        this.classInit.visitMethodInsn(Opcodes.INVOKESPECIAL, "python/types/ModuleType", "<init>", "()V", false);
        this.classInit.visitInsn(Opcodes.RETURN);
        this.classInit.visitMaxs(1, 1);
        this.classInit.visitEnd();
    }

    public PyFileCompileContext getFileContext() {
        return fileContext;
    }

    public String getClassName() {
        return this.fileContext.getClassName();
    }

    public String getSourceFileName() {
        return this.fileContext.getSourceFileName();
    }

    @Override
    public PyFileCompileContext fileContext() {
        return this.fileContext;
    }

    @Override
    public ClassNode classNode() {
        return classNode;
    }

    @Override
    protected MethodNode method() {
        return classInit;
    }

    @Override
    public Variable createVar(String name) {
        throw new UnsupportedOperationException("Cannot create variables in a class context");
    }

    @Override
    protected int localsVar() {
        throw new UnsupportedOperationException("Cannot get local variables in a class context");
    }

    @Override
    public void getLocal(String text) {
        throw new UnsupportedOperationException("Cannot get local variables in a class context");
    }

    @Override
    public void setLocal(String text, PyExprNode expr) {
        throw new UnsupportedOperationException("Cannot set local variables in a class context");
    }

    @Override
    public void setLocal(String text, Type asmType, PyExprNode expr) {
        throw new UnsupportedOperationException("Cannot set local variables in a class context");
    }

    @Override
    public PyClassCompileContext classContext() {
        return this;
    }

    public PyMethodCompileContext getMethodContext(MethodNode method) {
        return new PyMethodCompileContext(method, classNode, this.fileContext, this);
    }

    public void finish() {

    }
}
