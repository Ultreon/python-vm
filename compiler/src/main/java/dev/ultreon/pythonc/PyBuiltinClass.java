package dev.ultreon.pythonc;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static dev.ultreon.pythonc.FuncCall.E_CLASS_NOT_IN_CP;

class PyBuiltinClass implements JvmClass {
    public final Type jvmName;
    public final Type jvmUnboxed;
    public final Type extName;
    public final String pyName;
    private boolean isAbstract;

    public PyBuiltinClass(String jvmDesc, String extName, String pyName) {
        this(Type.getType(jvmDesc), extName, pyName);
    }

    public PyBuiltinClass(String jvmDesc, String jvmUnboxedDesc, String extName, String pyName) {
        this(Type.getType(jvmDesc), Type.getType(jvmUnboxedDesc), extName, pyName);
    }

    public PyBuiltinClass(Type jvmName, String extName, String pyName) {
        this(jvmName, jvmName, extName, pyName);
    }

    public PyBuiltinClass(Type jvmName, Type jvmUnboxed, String extName, String pyName) {
        this.jvmName = jvmName;
        this.jvmUnboxed = jvmUnboxed;
        this.extName = Type.getType(extName);
        this.pyName = pyName;
    }

    @Override
    public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        return null;
    }

    @Override
    public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
        if (jvmName.getSort() == Type.OBJECT) {
            compiler.writer.loadClass(jvmName);
            return;
        }
        throw new RuntimeException("Unknown JVM name: " + jvmName.getClassName());
    }

    @Override
    public int lineNo() {
        return 0;
    }

    @Override
    public String name() {
        return pyName;
    }

    @Override
    public Type type(PythonCompiler compiler) {
        return jvmUnboxed;
    }

    @Override
    public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit) {
        throw new CompilerException("Can't set class: " + pyName);
    }

    @Override
    public @Nullable JvmField field(PythonCompiler compiler, String name) {
        boolean load = PythonCompiler.classCache.load(compiler, extName);
        if (!load)
            throw new CompilerException(E_CLASS_NOT_IN_CP.formatted(extName.getClassName(), compiler.getLocation(this)));
        JvmClass extClass = PythonCompiler.classCache.get(extName);
        if (extClass == null)
            throw new CompilerException(E_CLASS_NOT_IN_CP.formatted(extName.getClassName(), compiler.getLocation(this)));
        if (!(extClass instanceof JClass jClass))
            throw new RuntimeException("Unknown JVM name: " + extName.getClassName());
        JvmField field = jClass.field(compiler, name);
        if (field != null) return field;
        boolean load1 = PythonCompiler.classCache.load(compiler, jvmName);
        if (!load1)
            throw new CompilerException(E_CLASS_NOT_IN_CP.formatted(jvmName.getClassName(), compiler.getLocation(this)));
        JvmClass jvmClass = PythonCompiler.classCache.get(jvmName);
        if (jvmClass == null)
            throw new CompilerException(E_CLASS_NOT_IN_CP.formatted(jvmName.getClassName(), compiler.getLocation(this)));
        if (!(jvmClass instanceof JClass jClass1))
            throw new RuntimeException("Unknown JVM name: " + jvmName.getClassName());
        field = jClass1.field(compiler, name);
        if (field == null)
            throw new CompilerException("Field '" + name + "' does not exist in class '" + className() + "' (" + compiler.getLocation(this) + ")");
        return field;
    }

    @Override
    public String className() {
        return jvmName.getClassName();
    }

    @Override
    public boolean isInterface() {
        return false;
    }

    @Override
    public boolean isAbstract() {
        return isAbstract;
    }

    @Override
    public boolean isEnum() {
        return false;
    }

    @Override
    public boolean doesInherit(PythonCompiler compiler, Type type) {
        if (jvmName.getSort() == Type.OBJECT) {
            boolean equals = jvmName.getClassName().equals(type.getClassName());
            if (!equals) {
                if (!PythonCompiler.classCache.load(compiler, type))
                    throw new CompilerException(E_CLASS_NOT_IN_CP.formatted(type.getClassName(), compiler.getLocation(this)));
                JvmClass jvmClass = PythonCompiler.classCache.get(type);
                if (jvmClass == null)
                    throw new CompilerException(E_CLASS_NOT_IN_CP.formatted(type.getClassName(), compiler.getLocation(this)));
                if (jvmClass instanceof JClass jClass) {
                    Class<?> type1 = jClass.getType();
                    if (type1 == null)
                        throw new CompilerException(E_CLASS_NOT_IN_CP.formatted(type.getClassName(), compiler.getLocation(this)));
                    equals = type1.isAssignableFrom(getClass());
                }
            }
            return equals;
        }

        throw new RuntimeException("Unknown JVM name: " + jvmName.getClassName());
    }

    @Override
    public @Nullable JvmFunction function(PythonCompiler compiler, String name, Type[] paramTypes) {
        boolean load = PythonCompiler.classCache.load(compiler, extName);
        if (!load)
            throw new CompilerException(E_CLASS_NOT_IN_CP.formatted(extName.getClassName(), compiler.getLocation(this)));
        JvmClass extClass = PythonCompiler.classCache.get(extName);
        if (extClass == null)
            throw new CompilerException(E_CLASS_NOT_IN_CP.formatted(extName.getClassName(), compiler.getLocation(this)));
        if (!(extClass instanceof JClass extJClass))
            throw new RuntimeException("Unknown JVM name: " + extName.getClassName());
        JvmFunction function = extJClass.function(compiler, name, paramTypes);
        if (function != null) return function;
        boolean load1 = PythonCompiler.classCache.load(compiler, jvmName);
        if (!load1)
            throw new CompilerException(E_CLASS_NOT_IN_CP.formatted(jvmName.getClassName(), compiler.getLocation(this)));
        JvmClass jvmClass = PythonCompiler.classCache.get(jvmName);
        if (jvmClass == null)
            throw new CompilerException(E_CLASS_NOT_IN_CP.formatted(jvmName.getClassName(), compiler.getLocation(this)));
        if (!(jvmClass instanceof JClass jvmJClass))
            throw new RuntimeException("Unknown JVM name: " + jvmName.getClassName());
        function = jvmJClass.function(compiler, name, paramTypes);
        if (function == null)
            throw new CompilerException("Function '" + name + "' does not exist in class '" + className() + "' (" + compiler.getLocation(this) + ")");
        return function;
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public JvmFunction constructor(PythonCompiler compiler, Type[] paramTypes) {
        throw new AssertionError("DEBUG");
    }

    public PyBuiltinClass setAbstract() {
        isAbstract = true;
        return this;
    }
}
