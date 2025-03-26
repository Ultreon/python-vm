package dev.ultreon.pythonc;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.Arrays;

public class PyBuiltinFunction implements JvmFunction {
    public final Type jvmOwner;
    public final Type mapOwner;
    public final Type[] signatures;
    public final int params;
    public final String name;
    public final boolean varArgs;
    public final boolean kwargs;
    public final boolean dynCall;
    private Type returnType;

    @Override
    public void invoke(Object callArgs, Runnable paramInit) {
        throw new AssertionError("DEBUG");
    }

    @Override
    public Type returnType(PythonCompiler compiler) {
        return type(compiler);
    }

    @Override
    public JvmClass returnClass(PythonCompiler compiler) {
        return null;
    }

    @Override
    public Type[] parameterTypes(PythonCompiler compiler) {
        return new Type[0];
    }

    @Override
    public JvmClass[] parameterClasses(PythonCompiler compiler) {
        return new JvmClass[0];
    }

    @Override
    public boolean isStatic() {
        return true;
    }

    @Override
    public JvmClass ownerClass(PythonCompiler compiler) {
        return null;
    }

    @Override
    public boolean isDynamicCall() {
        return dynCall;
    }

    enum Mode {
        NORMAL,
        VARARGS,
        KWARGS,
        DYN_CTOR,
    }

    public PyBuiltinFunction(String jvmDesc, String mapDesc, String[] signatures, int params, String name, Type returnType) {
        this(Type.getObjectType(jvmDesc), Type.getObjectType(mapDesc), Arrays.stream(signatures).map(Type::getMethodType).toArray(Type[]::new), params, name, returnType);
    }

    public PyBuiltinFunction(String jvmDesc, String mapDesc, String[] signatures, int params, String name, Mode mode, Type returnType) {
        this(Type.getObjectType(jvmDesc), Type.getObjectType(mapDesc), Arrays.stream(signatures).map(Type::getMethodType).toArray(Type[]::new), params, name, mode, returnType);
    }

    public PyBuiltinFunction(Type jvmOwner, Type mapOwner, Type[] signatures, int params, String name, Type returnType) {
        this(jvmOwner, mapOwner, signatures, params, name, Mode.NORMAL, returnType);
    }

    public PyBuiltinFunction(Type jvmOwner, Type mapOwner, Type[] signatures, int params, String name, Mode mode, Type returnType) {
        this.jvmOwner = jvmOwner;
        this.mapOwner = mapOwner;
        this.signatures = signatures;
        this.params = params;
        this.name = name;
        this.varArgs = mode == Mode.VARARGS;
        this.kwargs = mode == Mode.KWARGS;
        this.dynCall = mode == Mode.DYN_CTOR;
        this.returnType = returnType;
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
    public String name() {
        return name;
    }

    @Override
    public Type type(PythonCompiler compiler) {
        String className = mapOwner.getClassName();
        try {
            Class<?> aClass = Class.forName(className, false, getClass().getClassLoader());
            for (Method method : aClass.getMethods()) {
                String name1 = method.getName();
                if (!name1.equals(name)) {
                    continue;
                }

                return Type.getType(method.getReturnType());
            }

            throw new CompilerException("No matching function: " + className + "." + name + "(...)");
        } catch (ClassNotFoundException ignored) {
            return returnType;
        }

    }

    @Override
    public Location location() {
        return new Location("<builtin>", 0, 0, 0, 0);
    }

    @Override
    public void expectReturnType(PythonCompiler compiler, JvmClass returnType, Location location) {
        if (!returnType.doesInherit(compiler, jvmOwner)) {
            if (!returnType.doesInherit(compiler, mapOwner)) {
                throw new CompilerException("Return type '" + returnType.name() + "' does not inherit '" + jvmOwner.getClassName() + "' or '" + mapOwner.getClassName(), location);
            }
        }
    }

    @Override
    public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit) {
        throw new CompilerException("Cannot set a function", visit.location());
    }

    public JvmClass owner(PythonCompiler compiler) {
        if (mapOwner == null) {
            return null;
        }
        return PythonCompiler.classCache.require(compiler, mapOwner);
    }
}
