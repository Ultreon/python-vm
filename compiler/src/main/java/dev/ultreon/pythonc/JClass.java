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
import java.util.stream.Collectors;

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
        // Do <typedName>.class
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
        if (type(compiler).equals(type)) return true;
        if (type.getSort() != Type.OBJECT && type.getSort() != Type.ARRAY) {
            return type.getSort() == type(compiler).getSort();
        }
        JvmClass jvmClass = PythonCompiler.classCache.get(type);
        if (jvmClass instanceof JClass jClass) {
            return jClass.type.isAssignableFrom(this.type);
        }
        return false;
    }

    @Override
    public @Nullable JvmConstructor constructor(PythonCompiler compiler, Type[] paramTypes) {
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
    public JvmClass superClass(PythonCompiler compiler) {
        Class<?> superclass = type.getSuperclass();
        if (superclass == null) {
            return null;
        }
        if (!(PythonCompiler.classCache.load(compiler, superclass))) {
            throw new CompilerException("Class '" + type.getSuperclass().getName() + "' not found (" + compiler.getLocation(this) + ")");
        }

        return PythonCompiler.classCache.get(type.getSuperclass());
    }

    @Override
    public JvmClass[] interfaces(PythonCompiler compiler) {
        JvmClass[] interfaces = new JvmClass[type.getInterfaces().length];
        for (int i = 0, interfacesLength = interfaces.length; i < interfacesLength; i++) {
            Class<?> anInterface = type.getInterfaces()[i];
            if (!(PythonCompiler.classCache.load(compiler, anInterface))) {
                throw new CompilerException("Class '" + type.getInterfaces()[i].getName() + "' not found (" + compiler.getLocation(this) + ")");
            }
            interfaces[i] = PythonCompiler.classCache.get(type.getInterfaces()[i]);
        }
        return interfaces;
    }

    @Override
    public Map<String, List<JvmFunction>> methods(PythonCompiler compiler) {
        this.functions.clear();
        for (Method method : type.getMethods()) {
            JvmFunction jvmFunction = new JFunction(method.getName(), method, 0, 0);
            if (Modifier.isStatic(method.getModifiers()) && method.getName().equals("__init__")) {
                this.functions.computeIfAbsent("<init>", k -> new ArrayList<>()).add(jvmFunction);
            } else {
                this.functions.computeIfAbsent(method.getName(), k -> new ArrayList<>()).add(jvmFunction);
            }
        }
        for (Constructor<?> constructor : type.getConstructors()) {
            JvmFunction jvmFunction = new JConstructor("__init__", constructor, 0, 0);
            this.functions.computeIfAbsent("<init>", k -> new ArrayList<>()).add(jvmFunction);
        }
        return functions;
    }

    @Override
    public JvmConstructor[] constructors(PythonCompiler compiler) {
        Map<String, List<JvmFunction>> methods = methods(compiler);
        List<JvmConstructor> constructors = new ArrayList<>();
        List<JvmFunction> jvmFunctions = methods.get("<init>");
        if (jvmFunctions == null)
            return new JvmConstructor[0];
        jvmFunctions.forEach(jvmFunction -> constructors.add((JConstructor) jvmFunction));
        return constructors.toArray(new JvmConstructor[0]);
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
            if (parameterTypes.length != paramTypes.length) {
                continue;
            }
            for (int i = 0, parameterTypesLength = parameterTypes.length; i < parameterTypesLength; i++) {
                Class<?> theParamType = parameterTypes[i];
                if (!(PythonCompiler.classCache.load(compiler, Type.getType(theParamType))))
                    throw new CompilerException("Class '" + theParamType.getName() + "' not found (" + compiler.getLocation(this) + ")");

                if (!ourParamTypes[i].doesInherit(compiler, PythonCompiler.classCache.require(compiler, Type.getType(theParamType)))) {
                    continue methodLoop;
                }
            }

            JFunction jFunction = new JFunction(name, method1, 0, 0);
            this.functions.computeIfAbsent(name, k -> new ArrayList<>()).add(jFunction);
            return jFunction;
        }

        throw new CompilerException("Method '" + name + "' not found with parameters (" + Arrays.stream(paramTypes).map(Type::getClassName).collect(Collectors.joining(", ")) + ") in '" + type.getName() + "' (" + compiler.getLocation(this) + ")");
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

    @Override
    public boolean isArray() {
        return type.isArray();
    }
}
