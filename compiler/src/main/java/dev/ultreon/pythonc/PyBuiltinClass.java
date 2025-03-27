package dev.ultreon.pythonc;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.stream.Collectors;

public class PyBuiltinClass implements JvmClass {
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
        throw new RuntimeException("Unknown JVM typedName: " + jvmName.getClassName());
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
    public Location location() {
        return new Location("<builtin>", 0, 0, 0, 0);
    }

    @Override
    public void expectReturnType(PythonCompiler compiler, JvmClass returnType, Location location) {
        if (!returnType.doesInherit(compiler, this))
            throw new CompilerException("Expected " + this + " but got " + returnType + " at ", location);
    }

    @Override
    public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit) {
        throw new CompilerException("Can't set class: " + pyName);
    }

    @Override
    public @Nullable JvmField field(PythonCompiler compiler, String name) {
        boolean load = PythonCompiler.classCache.load(compiler, extName);
        if (!load)
            throw new CompilerException("Can't find class: " + extName.getClassName());
        JvmClass extClass = PythonCompiler.classCache.get(extName);
        if (extClass == null)
            throw new CompilerException("Can't find class: " + extName.getClassName());
        if (!(extClass instanceof JClass jClass))
            throw new RuntimeException("Unknown JVM typedName: " + extName.getClassName());
        JvmField field = jClass.field(compiler, name);
        if (field != null) return field;
        boolean load1 = PythonCompiler.classCache.load(compiler, jvmName);
        if (!load1)
            throw new CompilerException("Can't find class: " + jvmName.getClassName());
        JvmClass jvmClass = PythonCompiler.classCache.get(jvmName);
        if (jvmClass == null)
            throw new CompilerException("Can't find class: " + jvmName.getClassName());
        if (!(jvmClass instanceof JClass jClass1))
            throw new RuntimeException("Unknown JVM typedName: " + jvmName.getClassName());
        field = jClass1.field(compiler, name);
        if (field == null)
            throw new CompilerException("Field '" + name + "' does not exist in class '" + className(), compiler.getLocation(this));
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
        if (type(compiler).equals(type)) return true;
        if (jvmName.getSort() == Type.OBJECT) {
            boolean equals = jvmName.getClassName().equals(type.getClassName());
            if (!equals) {
                if (!PythonCompiler.classCache.load(compiler, type))
                    throw new CompilerException("Can't find class: " + type.getClassName());
                if (type.getSort() == Type.INT || type.getSort() == Type.LONG) {
                    if (this.pyName.equals("int")) return true;
                } else if (type.getSort() == Type.FLOAT || type.getSort() == Type.DOUBLE) {
                    if (this.pyName.equals("float")) return true;
                }
                JvmClass jvmClass = PythonCompiler.classCache.get(type);
                if (jvmClass == null)
                    throw new CompilerException("Can't find class: " + type.getClassName());
                if (jvmClass instanceof JClass jClass) {
                    Class<?> type1 = jClass.getType();
                    if (type1 == null)
                        throw new CompilerException("Can't find class: " + type.getClassName());
                    equals = this.doesInherit(compiler, jClass);
                }
            }
            return equals;
        }

        throw new RuntimeException("Unknown JVM typedName: " + jvmName.getClassName());
    }

    @Override
    public @Nullable JvmFunction function(PythonCompiler compiler, String name, Type[] paramTypes) {
        boolean load = PythonCompiler.classCache.load(compiler, extName);
        if (!load)
            throw new CompilerException("Extension class not found: " + extName.getClassName());
        JvmClass extClass = PythonCompiler.classCache.get(extName);
        if (extClass == null)
            throw new CompilerException("Extension class not found: " + extName.getClassName());
        if (!(extClass instanceof JClass extJClass))
            throw new RuntimeException("Unknown JVM typedName: " + extName.getClassName());
        JvmFunction function = extJClass.function(compiler, name, paramTypes);
        if (function != null) return function;
        boolean load1 = PythonCompiler.classCache.load(compiler, jvmName);
        if (!load1)
            throw new CompilerException("JVM class not found: " + extName.getClassName());
        JvmClass jvmClass = PythonCompiler.classCache.get(jvmName);
        if (jvmClass == null)
            throw new CompilerException("JVM class not found: " + extName.getClassName());
        if (!(jvmClass instanceof JClass jvmJClass))
            throw new RuntimeException("Unknown JVM typedName: " + jvmName.getClassName());
        function = jvmJClass.function(compiler, name, paramTypes);
        if (function == null)
            throw new CompilerException("Function '" + name + "' does not exist in class '" + className(), compiler.getLocation(this));
        return function;
    }

    @Override
    public boolean isPrimitive() {
        return pyName.equals("int") || pyName.equals("float") || pyName.equals("bool");
    }

    @Override
    public @Nullable JvmConstructor constructor(PythonCompiler compiler, Type[] paramTypes) {
        boolean load = PythonCompiler.classCache.load(compiler, extName);
        if (!load)
            throw new CompilerException("Extension class not found: " + extName.getClassName());
        JvmClass extClass = PythonCompiler.classCache.get(extName);
        if (extClass == null)
            throw new CompilerException("Extension class not found: " + extName.getClassName());
        if (!(extClass instanceof JClass extJClass))
            throw new RuntimeException("Unknown JVM typedName: " + extName.getClassName());
        JvmConstructor constructor = extJClass.constructor(compiler, paramTypes);
        if (constructor != null) return constructor;
        boolean load1 = PythonCompiler.classCache.load(compiler, jvmName);
        if (!load1)
            throw new CompilerException("JVM class not found: " + extName.getClassName());
        JvmClass jvmClass = PythonCompiler.classCache.get(jvmName);
        if (jvmClass == null)
            throw new CompilerException("JVM class not found: " + extName.getClassName());
        if (!(jvmClass instanceof JClass jvmJClass))
            throw new RuntimeException("Unknown JVM typedName: " + jvmName.getClassName());
        constructor = jvmJClass.constructor(compiler, paramTypes);
        if (constructor == null)
            throw new CompilerException("Constructor with paramTypes " + Arrays.stream(paramTypes).map(Type::getClassName).collect(Collectors.joining(", ")) + "C does not exist in class '" + className(), compiler.getLocation(this));
        return constructor;
    }

    @Override
    public JvmClass firstSuperClass(PythonCompiler compiler) {
        if (!PythonCompiler.classCache.load(compiler, jvmName)) {
            throw new CompilerException("Can't find class: " + jvmName.getClassName());
        }

        JvmClass jvmClass = PythonCompiler.classCache.get(jvmName);
        if (jvmClass == null)
            throw new CompilerException("Can't find class: " + jvmName.getClassName());
        if (jvmClass instanceof PyBuiltinClass builtinClass) {
            if (jvmClass == this) {
                try {
                    Class<?> type = Class.forName(jvmName.getClassName());
                    return type.getSuperclass() == null ? null : PythonCompiler.classCache.require(compiler, type.getSuperclass());
                } catch (ClassNotFoundException e) {
                    throw new CompilerException("Can't find class: " + jvmName.getClassName());
                }
            }
        }
        if (!(jvmClass instanceof JClass jvmJClass))
            throw new RuntimeException("Unknown JVM typedName: " + jvmName.getClassName());
        return jvmJClass.firstSuperClass(compiler);
    }

    @Override
    public JvmClass[] dynamicSuperClasses(PythonCompiler compiler) {
        return new JvmClass[0];
    }

    @Override
    public JvmClass[] interfaces(PythonCompiler compiler) {
        if (!PythonCompiler.classCache.load(compiler, jvmName)) {
            throw new CompilerException("Can't find class: " + jvmName.getClassName());
        }

        JvmClass jvmClass = PythonCompiler.classCache.get(jvmName);
        if (jvmClass == null)
            throw new CompilerException("Can't find class: " + jvmName.getClassName());
        if (jvmClass instanceof PyBuiltinClass builtinClass) {
            if (jvmClass == this) {
                try {
                    Class<?> type = Class.forName(jvmName.getClassName());
                    List<JvmClass> interfaces = new ArrayList<>();
                    for (Class<?> anInterface : type.getInterfaces()) {
                        interfaces.add(PythonCompiler.classCache.require(compiler, anInterface));
                    }
                    return interfaces.toArray(new JvmClass[0]);
                } catch (ClassNotFoundException e) {
                    throw new CompilerException("Can't find class: " + jvmName.getClassName());
                }
            }
        }
        if (!(jvmClass instanceof JClass jvmJClass))
            throw new RuntimeException("Unknown JVM typedName: " + jvmName.getClassName());
        return jvmJClass.interfaces(compiler);
    }

    @Override
    public Map<String, List<JvmFunction>> methods(PythonCompiler compiler) {
        for (JvmClass jvmClass : interfaces(compiler)) {
            Map<String, List<JvmFunction>> methods = jvmClass.methods(compiler);
            if (methods != null) {
                return methods;
            }
        }
        JvmClass superClass = firstSuperClass(compiler);
        if (superClass != null) {
            return superClass.methods(compiler);
        }

        JvmClass jvmClass = extClass(compiler);
        if (jvmClass != null) {
            return jvmClass.methods(compiler);
        }

        throw new RuntimeException("Unknown JVM builtin class: " + jvmName.getClassName());
    }

    private JvmClass extClass(PythonCompiler compiler) {
        boolean load = PythonCompiler.classCache.load(compiler, extName);
        if (!load)
            throw new CompilerException("Extension class not found: " + extName.getClassName());
        JvmClass extClass = PythonCompiler.classCache.get(extName);
        if (extClass == null)
            throw new CompilerException("Extension class not found: " + extName.getClassName());
        if (!(extClass instanceof JClass jvmJClass))
            throw new RuntimeException("Unknown JVM typedName: " + extName.getClassName());
        return jvmJClass;
    }

    @Override
    public JvmConstructor[] constructors(PythonCompiler compiler) {
        JvmClass extClass = extClass(compiler);
        List<JvmFunction> jvmFunctions = extClass.methods(compiler).get("<init>");
        if (jvmFunctions == null)
            return new JvmConstructor[0];
        List<JvmConstructor> constructors = jvmFunctions.stream().map(method -> {
            if (method instanceof JvmConstructor constructor) {
                return (JvmConstructor) null;
            }
            return new PyBuiltinConstructor(this, method);
        }).filter(Objects::nonNull).toList();
        return constructors.toArray(new JvmConstructor[0]);
    }

    @Override
    public boolean isArray() {
        return false;
    }

    @Override
    public JvmFunction requireFunction(PythonCompiler pythonCompiler, String name, Type[] types) {
        JvmFunction function = function(pythonCompiler, name, types);
        if (function == null)
            throw new CompilerException("Function " + name + " not found in class '" + pyName + "'");
        return function;
    }

    public PyBuiltinClass setAbstract() {
        isAbstract = true;
        return this;
    }
}
