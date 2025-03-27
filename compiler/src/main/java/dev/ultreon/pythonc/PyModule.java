package dev.ultreon.pythonc;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PyModule implements JvmClass, PyCompileClass {
    public static final String CANNOT_SET = "Cannot set the class '%s' (%s)";

    public final Path path;
    public final Type owner;
    public final Map<String, PyField> fields;
    public final Map<String, List<JvmFunction>> methods;
    private Location location;

    public PyModule(Path path, String name, Location location) {
        this.path = path;
        this.location = location;
        this.owner = Type.getObjectType(name);
        this.fields = new LinkedHashMap<>();
        this.methods = new LinkedHashMap<>();
    }

    public String owner(PythonCompiler compiler) {
        return PythonCompiler.FMT_CLASS.formatted(owner);
    }

    @Override
    public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        return null;
    }

    @Override
    public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
        throw new UnsupportedOperationException(PythonCompiler.E_NOT_ALLOWED);
    }

    @Override
    public String name() {
        String className = owner.getClassName();
        return className.substring(className.lastIndexOf('.') + 1);
    }

    @Override
    public @Nullable JvmField field(PythonCompiler compiler, String name) {
        return fields.get(name);
    }

    @Override
    public String className() {
        return owner.getClassName();
    }

    @Override
    public Type type(PythonCompiler compiler) {
        return owner;
    }

    @Override
    public Location location() {
        return location;
    }

    @Override
    public void expectReturnType(PythonCompiler compiler, JvmClass returnType, Location location) {
        if (!returnType.doesInherit(compiler, this))
            throw new CompilerException("Expected " + this + " but got " + returnType + " at ", location);
    }

    @Override
    public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit) {
        throw new CompilerException(CANNOT_SET.formatted(path, compiler.getLocation(visit)));
    }

    @Override
    public boolean isInterface() {
        // TODO add interface support
        return false;
    }

    @Override
    public boolean isAbstract() {
        // TODO add abstract class support
        return false;
    }

    @Override
    public boolean isEnum() {
        // TODO add enum support
        return false;
    }

    public boolean doesInherit(PythonCompiler compiler, Type type) {
        if (type == null) {
            return false;
        }

        return type.equals(Type.getType(Object.class));
    }

    @Override
    public @Nullable JvmFunction function(PythonCompiler compiler, String name, Type[] paramTypes) {
        for (List<JvmFunction> functions : methods.values()) {
            for (JvmFunction function : functions) {
                if (canAgreeWithParameters(compiler, function, paramTypes)) {
                    return function;
                }
            }
        }
        return null;
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public @Nullable JvmConstructor constructor(PythonCompiler compiler, Type[] paramTypes) {
        throw new AssertionError("Illegal compiler object call");
    }

    @Override
    public JvmClass firstSuperClass(PythonCompiler compiler) {
        if (!PythonCompiler.classCache.load(compiler, Type.getType(Object.class))) {
            throw new AssertionError("Illegal compiler object call");
        }

        return PythonCompiler.classCache.get(Type.getType(Object.class));
    }

    @Override
    public JvmClass[] dynamicSuperClasses(PythonCompiler compiler) {
        return new JvmClass[0];
    }

    @Override
    public JvmClass[] interfaces(PythonCompiler compiler) {
        if (!PythonCompiler.classCache.load(compiler, Type.getType("Lorg/python/_internal/PyModule;"))) {
            throw new NoClassDefFoundError("org/python/_internal/PyModule");
        }

        JvmClass jvmClass = PythonCompiler.classCache.get(Type.getType("Lorg/python/_internal/PyModule;"));
        if (!jvmClass.isInterface()) {
            throw new AssertionError("org/python/_internal/PyModule is not an interface");
        }
        return new JvmClass[] {jvmClass};
    }

    @Override
    public Map<String, List<JvmFunction>> methods(PythonCompiler compiler) {
        return methods;
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
        JvmFunction function = function(pythonCompiler, name, types);
        if (function == null) {
            return PythonCompiler.expectations.expectFunction(pythonCompiler, this, name, types, true, false, location);
        } else {
            return function;
        }
    }

    @Override
    public Path getOutputPath() {
        return path;
    }

    @Override
    public boolean isModule() {
        return true;
    }
}
