package dev.ultreon.pythonc;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class PyBuiltinConstructor implements JvmConstructor {
    private final PyBuiltinClass builtinClass;
    private final JvmFunction method;

    public PyBuiltinConstructor(PyBuiltinClass builtinClass, JvmFunction method) {
        this.builtinClass = builtinClass;
        this.method = method;
    }

    @Override
    public void write(MethodVisitor mv, PythonCompiler compiler, Runnable paramInit) {
        paramInit.run();
        method.write(mv, compiler);
    }

    @Override
    public void invoke(Object callArgs, Runnable paramInit) {
        throw new AssertionError("DEBUG");
    }

    @Override
    public Type returnType(PythonCompiler compiler) {
        return method.returnType(compiler);
    }

    @Override
    public JvmClass returnClass(PythonCompiler compiler) {
        return method.returnClass(compiler);
    }

    @Override
    public Type[] parameterTypes(PythonCompiler compiler) {
        return method.parameterTypes(compiler);
    }

    @Override
    public JvmClass[] parameterClasses(PythonCompiler compiler) {
        return method.parameterClasses(compiler);
    }

    @Override
    public JvmClass owner(PythonCompiler compiler) {
        return builtinClass;
    }

    @Override
    public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        return method.preload(mv, compiler, boxed);
    }

    @Override
    public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
        method.load(mv, compiler, preloaded, boxed);
    }

    @Override
    public int lineNo() {
        return method.lineNo();
    }

    @Override
    public String name() {
        return "<init>";
    }

    @Override
    public Type type(PythonCompiler compiler) {
        return method.returnType(compiler);
    }

    @Override
    public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit) {
        throw new AssertionError("DEBUG");
    }
}
