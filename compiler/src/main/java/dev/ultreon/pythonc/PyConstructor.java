package dev.ultreon.pythonc;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class PyConstructor extends PyFunction implements JvmConstructor {
    public PyConstructor(PyClass owner, Type[] paramTypes, Type returnType, int lineNo) {
        super(owner, "<init>", paramTypes, returnType, lineNo);
    }

    @Override
    public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        return null;
    }

    @Override
    public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
        throw new RuntimeException("not supported");
    }

    @Override
    public void write(MethodVisitor mv, PythonCompiler compiler, Runnable paramInit) {
        compiler.writer.newInstance(owner(compiler).type(compiler).getInternalName(), "<init>", Type.getMethodType(Type.VOID_TYPE, parameterTypes(compiler)).getDescriptor(), false, paramInit);
    }

    @Override
    public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit) {
        throw new RuntimeException("not supported");
    }

    @Override
    public void invoke(Object callArgs, Runnable paramInit) {
        throw new AssertionError("DEBUG");
    }

    @Override
    public Type returnType(PythonCompiler compiler) {
        return owner(compiler).type(compiler);
    }

    @Override
    public JvmClass returnClass(PythonCompiler compiler) {
        return owner(compiler);
    }

    @Override
    public boolean isStatic() {
        return false;
    }
}
