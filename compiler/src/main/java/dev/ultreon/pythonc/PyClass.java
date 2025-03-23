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
    private final int lineNo;
    private final int columnNo;
    private final List<? extends JvmClass> superClasses = new ArrayList<>();
    private final Path path;

    public PyClass(Path path, String name, int lineNo, int columnNo) {
        this.name = name;
        String internalNamePrefix = path.toString().replace(File.separatorChar, '/');
        Type owner = Type.getObjectType(internalNamePrefix + '/' + name);
        this.path = path;
        this.className = owner.getClassName();
        this.owner = owner;
        this.lineNo = lineNo;
        this.columnNo = columnNo;
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
    public int lineNo() {
        return lineNo;
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

        for (JvmClass jvmClass : superClasses) {
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
    public @Nullable JvmFunction function(PythonCompiler compiler, String name, Type[] paramTypes) {
        for (JvmFunction function : methods.get(name)) {
            if (Arrays.equals(function.parameterTypes(compiler), paramTypes)) {
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
    public JvmClass superClass(PythonCompiler compiler) {
        for (JvmClass superClass : superClasses) {
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
    public int columnNo() {
        return columnNo;
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
    public Path getOutputPath() {
        return path;
    }
}
