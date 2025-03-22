package dev.ultreon.pythonc;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

final class JClass implements JvmClass {
    private final String className;
    private final Type asmType;
    private final Class<?> type;
    private String alias;
    private final HashMap<String, JvmField> fields = new HashMap<>();
    private final Map<String, List<JvmFunction>> functions = new HashMap<>();

    JClass(String className, Class<?> type) {
        this.className = className;
        this.asmType = Type.getObjectType(className.replace(".", "/"));
        this.alias = alias;
        this.type = type;
    }

    @Override
    public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        return null;
    }

    @Override
    public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
        // Do <name>.class
        compiler.writer.loadClass(Type.getType("L" + className + ";"));
    }

    public Type asmType() {
        return asmType;
    }

    public Class<?> getType() {
        return type;
    }

    @Override
    public int lineNo() {
        return 0;
    }

    @Override
    public String name() {
        return className;
    }

    @Override
    public Type type(PythonCompiler compiler) {
        return asmType;
    }

    @Override
    public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit) {

    }

    public boolean isInterface(PythonCompiler compiler) {
        try {
            Class<?> aClass = Class.forName(className.replace("/", "."), false, getClass().getClassLoader());
            return aClass.isInterface();
        } catch (ClassNotFoundException e) {
            throw new CompilerException("JVM Class '" + className + "' not found (" + compiler.getLocation(this) + ")");
        }
    }

    @Override
    public boolean isInterface() {
        return type.isInterface();
    }

    @Override
    public boolean isAbstract() {
        return Modifier.isAbstract(type.getModifiers());
    }

    @Override
    public boolean isEnum() {
        return type.isEnum();
    }

    @Override
    public boolean doesInherit(PythonCompiler compiler, Type type) {
        JvmClass jvmClass = PythonCompiler.classCache.get(type);
        if (jvmClass instanceof JClass jClass) {
            return jClass.type.isAssignableFrom(this.type);
        }
        return false;
    }

    @Override
    public JvmFunction constructor(PythonCompiler compiler, Type[] paramTypes) {
        Method method;
        Constructor<?>[] constructors = type.getConstructors();
        JvmClass[] ourParamTypes = new JvmClass[paramTypes.length];
        for (int i = 0, paramTypesLength = paramTypes.length; i < paramTypesLength; i++) {
            Type paramType = paramTypes[i];
            if (!(PythonCompiler.classCache.load(compiler, paramType)))
                throw new CompilerException("Class '" + paramType.getClassName() + "' not found (" + compiler.getLocation(this) + ")");
            ourParamTypes[i] = PythonCompiler.classCache.get(paramType);
        }

        Constructor<?> theConstructor;
        methodLoop:
        for (Constructor<?> constructor1 : constructors) {
            @NotNull Class<?>[] parameterTypes = constructor1.getParameterTypes();
            for (int i = 0, parameterTypesLength = parameterTypes.length; i < parameterTypesLength; i++) {
                Class<?> theParamType = parameterTypes[i];
                if (!(PythonCompiler.classCache.load(compiler, Type.getType(theParamType))))
                    throw new CompilerException("Class '" + theParamType.getName() + "' not found (" + compiler.getLocation(this) + ")");

                if (i > ourParamTypes.length - 1) {
                    continue methodLoop;
                }
                if (!ourParamTypes[i].doesInherit(compiler, Type.getType(theParamType))) {
                    continue methodLoop;
                }
            }

            if (ourParamTypes.length != parameterTypes.length) {
                continue;
            }

            JConstructor jFunction = new JConstructor("__init__", constructor1, 0, 0);
            this.functions.computeIfAbsent("__init__", k -> new ArrayList<>()).add(jFunction);
            return jFunction;
        }

        throw new CompilerException("Constructor not found (" + compiler.getLocation(this) + ")");
    }

    @Override
    public @Nullable JvmFunction function(PythonCompiler compiler, String name, Type[] paramTypes) {
        Method method;
        Method[] methodsByName = ClassUtils.getMethodsByName(type, name);
        JvmClass[] ourParamTypes = new JvmClass[paramTypes.length];
        for (int i = 0, paramTypesLength = paramTypes.length; i < paramTypesLength; i++) {
            Type paramType = paramTypes[i];
            if (!(PythonCompiler.classCache.load(compiler, paramType)))
                throw new CompilerException("Class '" + paramType.getClassName() + "' not found (" + compiler.getLocation(this) + ")");
            ourParamTypes[i] = PythonCompiler.classCache.get(paramType);
        }

        Method theMethod;
        methodLoop:
        for (Method method1 : methodsByName) {
            @NotNull Class<?>[] parameterTypes = method1.getParameterTypes();
            for (int i = 0, parameterTypesLength = parameterTypes.length; i < parameterTypesLength; i++) {
                Class<?> theParamType = parameterTypes[i];
                if (!(PythonCompiler.classCache.load(compiler, Type.getType(theParamType))))
                    throw new CompilerException("Class '" + theParamType.getName() + "' not found (" + compiler.getLocation(this) + ")");

                if (!ourParamTypes[i].doesInherit(compiler, Type.getType(theParamType))) {
                    continue methodLoop;
                }
            }

            JFunction jFunction = new JFunction(name, method1, 0, 0);
            this.functions.computeIfAbsent(name, k -> new ArrayList<>()).add(jFunction);
            return jFunction;
        }

        throw new CompilerException("Method '" + name + "' not found (" + compiler.getLocation(this) + ")");
    }

    @Override
    public boolean isPrimitive() {
        return type.isPrimitive();
    }

    @Override
    public @Nullable JvmField field(PythonCompiler compiler, String name) {
        Field field;
        try {
            field = this.type.getField(name);
        } catch (NoSuchFieldException e) {
            return null;
        }
        Class<?> type1 = field.getType();
        if (Modifier.isStatic(field.getModifiers())) {
            if (type1.isPrimitive()) {
                Type asmType = Type.getType(type1);
                Field finalField = field;
                return fields.computeIfAbsent(name, k -> new JField(this, finalField, name, asmType));
            }
            Field finalField = field;
            return fields.computeIfAbsent(name, k -> new JField(this, finalField, name, Type.getType(type1)));
        }
        if (type1.isPrimitive()) {
            Type asmType = Type.getType(type1);
            Field finalField1 = field;
            return fields.computeIfAbsent(name, k -> new JField(this, finalField1, name, asmType));
        } else {
            Field finalField2 = field;
            return fields.computeIfAbsent(name, k -> new JField(this, finalField2, name, Type.getType(type1)));
        }
    }

    public String className() {
        return className;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (JClass) obj;
        return Objects.equals(this.className, that.className);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className);
    }

    @Override
    public String toString() {
        return "JClass[" +
                "className=" + className + ']';
    }

}
