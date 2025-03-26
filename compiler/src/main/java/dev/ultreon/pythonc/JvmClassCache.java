package dev.ultreon.pythonc;

import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.Map;

public class JvmClassCache {
    private final Map<Type, JvmClass> byType = new HashMap<>();

    public JvmClassCache() {

    }

    public void init(PythonCompiler compiler) {
        for (PyBuiltinClass pyBuiltinClass : compiler.builtins.getClasses()) {
            byType.put(compiler.writer.unboxType(pyBuiltinClass.type(compiler)), pyBuiltinClass);
            byType.put(compiler.writer.boxType(pyBuiltinClass.type(compiler)), pyBuiltinClass);

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
        JvmClass jvmClass = byType.get(type);
        if (jvmClass == null) {
            throw new RuntimeException("Class '" + type.getClassName() + "' not found");
        }
        return jvmClass;
    }

    public void add(PythonCompiler compiler, JvmClass jvmClass) {
        byType.put(jvmClass.type(compiler), jvmClass);
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
            JvmClass jvmClass = new JClass(className, aClass);
            byType.put(jvmClass.type(compiler), jvmClass);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public void replace(PythonCompiler compiler, JvmClass jvmClass) {
        byType.replace(jvmClass.type(compiler), jvmClass);
    }

    public void remove(PythonCompiler compiler, JvmClass jvmClass) {
        byType.remove(jvmClass.type(compiler));
    }

    public void clear() {
        byType.clear();
    }

    public boolean load(PythonCompiler compiler, Class<?> declaringClass) {
        JvmClass jvmClass = new JClass(declaringClass.getName(), declaringClass);
        byType.put(jvmClass.type(compiler), jvmClass);
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
            return PythonCompiler.expectations.expectClass(compiler, type.getClassName().substring(0, type.getClassName().lastIndexOf('.')), type.getClassName().substring(type.getClassName().lastIndexOf('.') + 1));
        }
        return get(type);
    }

    public JvmClass require(PythonCompiler compiler, Class<?> type) {
        if (!(load(compiler, type))) {
//            throw new CompilerException("Class '" + type.getName() + "' not found");
            return PythonCompiler.expectations.expectClass(compiler, type.getPackageName(), type.getSimpleName());
        }
        return get(type);
    }
}
