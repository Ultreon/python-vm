package dev.ultreon.pythonc;

import dev.ultreon.pythonc.classes.JavaClass;
import dev.ultreon.pythonc.classes.PyBuiltinClass;
import org.objectweb.asm.Type;

import dev.ultreon.pythonc.classes.JvmClass;

import java.util.HashMap;
import java.util.Map;

public class JvmClassCache {
    private final Map<Type, JvmClass> byType = new HashMap<>();

    public JvmClassCache() {

    }

    public void init(PythonCompiler compiler) {
        for (PyBuiltinClass pyBuiltinClass : compiler.builtins.getClasses()) {
            byType.put(compiler.writer.unboxType(pyBuiltinClass.type()), pyBuiltinClass);
            byType.put(compiler.writer.boxType(pyBuiltinClass.type()), pyBuiltinClass);

            if (pyBuiltinClass.pyName.equals("int")) {
                byType.put(Type.INT_TYPE, pyBuiltinClass);
                byType.put(Type.getType(Integer.class), pyBuiltinClass);
            } else if (pyBuiltinClass.pyName.equals("float")) {
                byType.put(Type.FLOAT_TYPE, pyBuiltinClass);
                byType.put(Type.getType(Float.class), pyBuiltinClass);
            }
        }
    }

    public JvmClass get(Type type) {
        return byType.get(type);
    }

    public void add(PythonCompiler compiler, JvmClass jvmClass) {
        byType.put(jvmClass.type(), jvmClass);
    }

    public boolean load(PythonCompiler compiler, Type type) {
        if (byType.containsKey(type)) {
            return true;
        }
        if (type.getSort() == Type.ARRAY) throw new AssertionError("DEBUG");
        if (type.getSort() != Type.OBJECT) return true;
        String className = type.getClassName();
        try {
            Class<?> aClass = Class.forName(className, false, getClass().getClassLoader());
            JvmClass jvmClass = new JavaClass(className, aClass, new Location());
            byType.put(jvmClass.type(), jvmClass);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public void replace(PythonCompiler compiler, JvmClass jvmClass) {
        byType.replace(jvmClass.type(), jvmClass);
    }

    public void remove(PythonCompiler compiler, JvmClass jvmClass) {
        byType.remove(jvmClass.type());
    }

    public void clear() {
        byType.clear();
    }

    public boolean load(PythonCompiler compiler, Class<?> loadingClass) {
        JvmClass jvmClass = new JavaClass(loadingClass.getName(), loadingClass, new Location());
        byType.put(jvmClass.type(), jvmClass);
        return true;
    }

    public JvmClass get(Class<?> declaringClass) {
        JvmClass jvmClass = byType.get(Type.getType(declaringClass));
        if (jvmClass == null) {
            throw new RuntimeException("Class '" + declaringClass.getName() + "' not found");
        }
        return jvmClass;
    }

    public JvmClass require(PythonCompiler compiler, Type type) {
        if (!(load(compiler, type))) {
            ClassPath classPath = ClassPath.of(type);
            return PythonCompiler.expectations.expectClass(compiler, classPath.path(), classPath.name());
        }
        return get(type);
    }

    public JvmClass require(PythonCompiler compiler, Class<?> type, Location location) {
        if (!(load(compiler, type))) {
            throw new CompilerException("Class '" + type.getName() + "' not found", location);
        }
        return get(type);
    }

    public JvmClass object(PythonCompiler compiler) {
        return require(compiler, Type.getObjectType("java/lang/Object"));
    }

    public JvmClass void_(PythonCompiler compiler) {
        return require(compiler, Type.VOID_TYPE);
    }

    public JvmClass require(Type type) {
        return require(PythonCompiler.current(), type);
    }

    public JvmClass require(Class<?> type, Location location) {
        return require(PythonCompiler.current(), type, location);
    }
}
