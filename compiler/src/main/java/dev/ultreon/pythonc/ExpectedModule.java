package dev.ultreon.pythonc;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.Map;

public class ExpectedModule implements JvmClass {
    private final String name;
    private final Type type;

    public ExpectedModule(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public @Nullable JvmField field(PythonCompiler compiler, String name) {
        return PythonCompiler.expectations.expectField(compiler, this, name, true);
    }

    @Override
    public @Nullable JvmFunction function(PythonCompiler compiler, String name, Type[] paramTypes) {
        return PythonCompiler.expectations.expectFunction(compiler, this, name, paramTypes, true, false, new Location());
    }

    @Override
    public String className() {
        return type.getClassName();
    }

    @Override
    public boolean isInterface() {
        return false;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }

    @Override
    public boolean isEnum() {
        return false;
    }

    @Override
    public boolean doesInherit(PythonCompiler compiler, Type type) {
        return false;
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public @Nullable JvmConstructor constructor(PythonCompiler compiler, Type[] paramTypes) {
        return null;
    }

    @Override
    public JvmClass firstSuperClass(PythonCompiler compiler) {
        return null;
    }

    @Override
    public JvmClass[] dynamicSuperClasses(PythonCompiler compiler) {
        return new JvmClass[0];
    }

    @Override
    public JvmClass[] interfaces(PythonCompiler compiler) {
        return new JvmClass[0];
    }

    @Override
    public Map<String, List<JvmFunction>> methods(PythonCompiler compiler) {
        return Map.of();
    }

    @Override
    public JvmConstructor[] constructors(PythonCompiler compiler) {
        return new JvmConstructor[0];
    }

    @Override
    public boolean isArray() {
        return false;
    }

    @Override
    public JvmFunction requireFunction(PythonCompiler pythonCompiler, String name, Type[] types) {
        return PythonCompiler.expectations.expectFunction(pythonCompiler, this, name, types, true, false, new Location());
    }

    @Override
    public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        return null;
    }

    @Override
    public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {

    }

    @Override
    public String name() {
        return "";
    }

    @Override
    public Type type(PythonCompiler compiler) {
        return null;
    }

    @Override
    public Location location() {
        return new Location("<unknown>", 0, 0, 0, 0);
    }

    @Override
    public void expectReturnType(PythonCompiler compiler, JvmClass returnType, Location location) {
        if (!returnType.doesInherit(compiler, this))
            throw new CompilerException("Expected " + this + " but got " + returnType + " at ", location);
    }

    @Override
    public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit) {
        throw new CompilerException("Cannot set a module");
    }
}
