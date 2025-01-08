package dev.ultreon.pythonc;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

class PyBuiltinFunction implements Symbol {
    public final Type jvmOwner;
    public final Type mapOwner;
    public final Type[] signatures;
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

    public PyBuiltinFunction(String jvmDesc, String mapDesc, String[] signatures, int params, String name) {
        this(Type.getObjectType(jvmDesc), Type.getObjectType(mapDesc), Arrays.stream(signatures).map(Type::getMethodType).toArray(Type[]::new), params, name);
    }

    public PyBuiltinFunction(String jvmDesc, String mapDesc, String[] signatures, int params, String name, Mode mode) {
        this(Type.getObjectType(jvmDesc), Type.getObjectType(mapDesc), Arrays.stream(signatures).map(Type::getMethodType).toArray(Type[]::new), params, name, mode);
    }

    public PyBuiltinFunction(Type jvmOwner, Type mapOwner, Type[] signatures, int params, String name) {
        this(jvmOwner, mapOwner, signatures, params, name, Mode.NORMAL);
    }

    public PyBuiltinFunction(Type jvmOwner, Type mapOwner, Type[] signatures, int params, String name, Mode mode) {
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
            throw new CompilerException("JVM Class '" + className + "' not found (" + compiler.getLocation(this) + ")");
        }

    }

    @Override
    public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit) {

    }

    public Type owner(PythonCompiler compiler) {
        return jvmOwner;
    }
}
