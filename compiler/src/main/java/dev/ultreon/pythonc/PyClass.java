package dev.ultreon.pythonc;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

public class PyClass implements JvmClass, PyCompileClass {
    public static final String CANNOT_SET = "Cannot set the class '%s' (%s)";

    public final String name;
    public final String className;
    public final Type owner;
    public final Map<String, PyField> fields;
    public final Map<String, List<JvmFunction>> methods;
    private final Path path;
    private Type[] dynamicSuperTypes;
    private JvmClass[] dynamicSuperClasses;
    private final Location location;

    public PyClass(Path path, String name, Location location, Type... superClasses) {
        this.name = name;
        this.location = location;
        String internalNamePrefix = path.toString().replace(File.separatorChar, '/');
        Type owner = Type.getObjectType(internalNamePrefix + '/' + name);
        this.path = path;
        this.className = owner.getClassName();
        this.owner = owner;
        this.fields = new LinkedHashMap<>();
        this.methods = new LinkedHashMap<>();
        this.dynamicSuperTypes = superClasses;
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
        compiler.writer.loadClass(this);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public @Nullable JvmField field(PythonCompiler compiler, String name) {
        return fields.get(name);
    }

    @Override
    public String className() {
        return className;
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
        throw new CompilerException(CANNOT_SET.formatted(className, compiler.getLocation(visit)));
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

        if (type.equals(Type.getType(Object.class))) {
            return true;
        }

        for (JvmClass jvmClass : dynamicSuperClasses(compiler)) {
            if (jvmClass.doesInherit(compiler, type)) {
                return true;
            }

            if (jvmClass instanceof JClass jClass) {
                if (compiler.classes.byType(type).doesInherit(compiler, jClass.type(compiler))) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean doesInherit(PythonCompiler compiler, JvmClass checkAgainst) {
        if (this.type(compiler).equals(Type.getType(Object.class))) return true;
        if (this.equals(checkAgainst)) return true;
        JvmClass superClass = firstSuperClass(compiler);
        if (superClass == null) return false;
        if (superClass.equals(checkAgainst)) return true;
        for (JvmClass anInterface : dynamicSuperClasses(compiler)) {
            if (anInterface == null) continue;
            if (anInterface.equals(checkAgainst)) return true;
            if (anInterface.doesInherit(compiler, checkAgainst)) return true;
        }
        return superClass.doesInherit(compiler, checkAgainst);
    }

    @Override
    public @Nullable JvmFunction function(PythonCompiler compiler, String name, Type[] argTypes) {
        List<JvmFunction> jvmFunctions = methods.get(name);
        if (jvmFunctions == null) return null;
        for (JvmFunction function : jvmFunctions) {
            if (canAgreeWithParameters(compiler, function, argTypes)) {
                return function;
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
        for (JvmFunction function : methods.get("<init>")) {
            if (canAgreeWithParameters(compiler, function, paramTypes)) {
                return (JvmConstructor) function;
            }
        }
        return null;
    }

    @Override
    public JvmClass firstSuperClass(PythonCompiler compiler) {
        for (JvmClass superClass : dynamicSuperClasses(compiler)) {
            if (superClass instanceof JClass jClass) {
                if (!jClass.getType().isInterface()) {
                    return jClass;
                }
            } else {
                return superClass;
            }
        }
        return null;
    }

    @Override
    public JvmClass[] dynamicSuperClasses(PythonCompiler compiler) {
        if (dynamicSuperTypes == null) {
            return new JvmClass[0];
        }

        if (dynamicSuperClasses != null) {
            return dynamicSuperClasses;
        }

        List<JvmClass> superClasses = new ArrayList<>();
        for (Type superType : dynamicSuperTypes) {
            if (!(PythonCompiler.classCache.load(compiler, superType))) {
                throw new CompilerException("Class '" + superType.getClassName() + "' not found (" + compiler.getLocation(this) + ")");
            }

            superClasses.add(PythonCompiler.classCache.get(superType));
        }

        JvmClass[] array = superClasses.toArray(JvmClass[]::new);
        this.dynamicSuperClasses = array;
        return array;
    }

    @Override
    public JvmClass[] interfaces(PythonCompiler compiler) {
        List<JvmClass> superClasses = new ArrayList<>();
        for (JvmClass superClass : superClasses) {
            if (superClass instanceof JClass jClass) {
                if (jClass.getType().isInterface()) {
                    superClasses.add(jClass);
                }
            }
        }
        return superClasses.toArray(JvmClass[]::new);
    }

    @Override
    public Map<String, List<JvmFunction>> methods(PythonCompiler compiler) {
        return methods;
    }

    @Override
    public JvmConstructor[] constructors(PythonCompiler compiler) {
        List<JvmConstructor> list = new ArrayList<>();
        for (JvmFunction jvmFunction : methods.get("<init>")) {
            JvmConstructor function = (JvmConstructor) jvmFunction;
            list.add(function);
        }
        return list.toArray(JvmConstructor[]::new);
    }

    @Override
    public boolean isArray() {
        return owner.getSort() == Type.ARRAY;
    }

    @Override
    public JvmFunction requireFunction(PythonCompiler pythonCompiler, String name, Type[] types) {
        JvmFunction function = function(pythonCompiler, name, types);
        if (function == null) {
            return PythonCompiler.expectations.expectFunction(pythonCompiler, this, name, types, false, false, location);
        }
        return function;
    }

    @Override
    public Path getOutputPath() {
        return path;
    }

    public boolean isModule() {
        return false;
    }
}
