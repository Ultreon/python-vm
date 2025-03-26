package dev.ultreon.pythonc;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExpectedClass implements JvmClass {
    private final String module;
    private final String name;
    private final Type type;

    private final List<Type> expectedInheritors = new ArrayList<>();
    private Location location = new Location("<unknown>", 0, 0, 0, 0);

    public ExpectedClass(String module, String name) {
        this.module = module;
        this.name = name;

        this.type = Type.getObjectType(module.replace(".", "/") + "/" + name);
    }

    @Override
    public @Nullable JvmField field(PythonCompiler compiler, String name) {
        return PythonCompiler.expectations.expectField(compiler, this, name, true);
    }

    @Override
    public @Nullable JvmFunction function(PythonCompiler compiler, String name, Type[] paramTypes) {
        return PythonCompiler.expectations.expectFunction(compiler, this, name, paramTypes, false, false, location);
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
        this.expectedInheritors.add(type);
        return true;
    }

    public List<Type> expectedInheritors() {
        return expectedInheritors;
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public @Nullable JvmConstructor constructor(PythonCompiler compiler, Type[] paramTypes) {
        return PythonCompiler.expectations.expectConstructor(compiler, this, paramTypes);
    }

    @Override
    public JvmClass firstSuperClass(PythonCompiler compiler) {
        return PythonCompiler.classCache.require(compiler, expectedInheritors.getFirst());
    }

    @Override
    public JvmClass[] dynamicSuperClasses(PythonCompiler compiler) {
        for (Type expectedInheritor : expectedInheritors) {
            JvmClass jvmClass = PythonCompiler.classCache.get(expectedInheritor);
            if (!jvmClass.isInterface()) {
                return new JvmClass[]{jvmClass};
            }
        }
        return new JvmClass[0];
    }

    @Override
    public JvmClass[] interfaces(PythonCompiler compiler) {
        for (Type expectedInheritor : expectedInheritors) {
            JvmClass jvmClass = PythonCompiler.classCache.get(expectedInheritor);
            if (jvmClass.isInterface()) {
                return new JvmClass[]{jvmClass};
            }
        }
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
        return null;
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
        return type;
    }

    @Override
    public Location location() {
        return location;
    }

    @Override
    public void expectReturnType(PythonCompiler compiler, JvmClass returnType, Location location) {
        if (!this.doesInherit(compiler, returnType)) {
            throw new CompilerException("Expected return type " + returnType + " but got " + this, location);
        }
    }

    @Override
    public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit) {

    }
}
