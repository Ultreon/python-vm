package dev.ultreon.pythonc;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.stream.Collectors;

public record ExpectedConstructor(JvmClass owner, Type[] parameters) implements JvmConstructor {

    @Override
    public void invoke(Object callArgs, Runnable paramInit) {
        throw new AssertionError("DEBUG");
    }

    @Override
    public Type returnType(PythonCompiler compiler) {
        return owner.type(compiler);
    }

    @Override
    public JvmClass returnClass(PythonCompiler compiler) {
        return owner;
    }

    @Override
    public Type[] parameterTypes(PythonCompiler compiler) {
        return parameters;
    }

    @Override
    public JvmClass[] parameterClasses(PythonCompiler compiler) {
        JvmClass[] classes = new JvmClass[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            classes[i] = PythonCompiler.classCache.require(compiler, parameters[i]);
        }
        return classes;
    }

    @Override
    public void write(MethodVisitor mv, PythonCompiler compiler, Runnable paramInit) {
//        compiler.writer.newInstance(owner.type(compiler).getInternalName(), "<init>", "(" + Arrays.stream(parameters).map(Type::getDescriptor).collect(Collectors.joining("")) + ")V", false, paramInit);
        compiler.writer.dynamicCall("<init>", "(" + Arrays.stream(parameters).map(Type::getDescriptor).collect(Collectors.joining("")) + ")V");
    }

    @Override
    public JvmClass ownerClass(PythonCompiler compiler) {
        return owner;
    }

    @Override
    public boolean isDynamicCall() {
        return false;
    }

    @Override
    public JvmClass owner(PythonCompiler compiler) {
        return owner;
    }

    @Override
    public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        return null;
    }

    @Override
    public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
        throw new AssertionError("Constructor not supported");
    }

    @Override
    public String name() {
        return "<init>";
    }

    @Override
    public Type type(PythonCompiler compiler) {
        return owner.type(compiler);
    }

    @Override
    public void expectReturnType(PythonCompiler compiler, JvmClass returnType, Location location) {
        if (!returnType.equals(owner)) {
            throw new CompilerException("Expected " + owner + " but got " + returnType + " at ", location);
        }
    }

    @Override
    public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit) {
        throw new AssertionError("DEBUG");
    }

    @Override
    public String toString() {
        return "ExpectedConstructor[" +
                "owner=" + owner + ", " +
                "name=<init>, " +
                "parameters=" + parameters + ']';
    }

    @Override
    public Location location() {
        return new Location("<unknown>", 0, 0, 0, 0);
    }
}
