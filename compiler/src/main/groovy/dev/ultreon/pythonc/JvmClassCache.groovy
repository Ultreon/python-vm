package dev.ultreon.pythonc

import dev.ultreon.pythonc.classes.JavaClass
import dev.ultreon.pythonc.classes.JvmClass
import dev.ultreon.pythonc.classes.PyBuiltinClass
import org.objectweb.asm.Type 

class JvmClassCache {
    private final Map<Type, JvmClass> byType = new HashMap<>()

    JvmClassCache() {

    }

    void init(PythonCompiler compiler) {
        for (PyBuiltinClass pyBuiltinClass : (compiler.builtins.classes)) {
            byType.put(compiler.writer.unboxType(pyBuiltinClass.type), pyBuiltinClass)
            byType.put(compiler.writer.boxType(pyBuiltinClass.type), pyBuiltinClass)

            if (pyBuiltinClass.pyName == "int") {
                byType.put(Type.INT_TYPE, pyBuiltinClass)
                byType.put(Type.getType(Integer.class), pyBuiltinClass)
            } else if (pyBuiltinClass.pyName == "float") {
                byType.put(Type.FLOAT_TYPE, pyBuiltinClass)
                byType.put(Type.getType(Float.class), pyBuiltinClass)
            }
        }
    }

    JvmClass get(Type type) {
        return byType.get(type)
    }

    void add(PythonCompiler compiler, JvmClass jvmClass) {
        byType.put(jvmClass.type, jvmClass)
    }

    boolean load(PythonCompiler compiler, Type type) {
        if (byType.containsKey(type)) {
            return true
        }
        if (type.sort == Type.ARRAY) throw new IllegalStateException("DEBUG")
        if (type.sort != Type.OBJECT) return true
        String className = type.className
        try {
            Class<?> aClass = Class.forName(className, false, getClass().classLoader)
            JvmClass jvmClass = new JavaClass(className, aClass, new Location())
            byType.put(jvmClass.type, jvmClass)
            return true
        } catch (ClassNotFoundException ignored) {
            return false
        }
    }

    void replace(PythonCompiler compiler, JvmClass jvmClass) {
        byType.replace(jvmClass.type, jvmClass)
    }

    void remove(PythonCompiler compiler, JvmClass jvmClass) {
        byType.remove(jvmClass.type)
    }

    void clear() {
        byType.clear()
    }

    boolean load(PythonCompiler compiler, Class<?> loadingClass) {
        JvmClass jvmClass = new JavaClass(loadingClass.name, loadingClass, new Location())
        byType.put(jvmClass.type, jvmClass)
        return true
    }

    JvmClass get(Class<?> declaringClass) {
        JvmClass jvmClass = byType[Type.getType(declaringClass)]
        if (jvmClass == null) {
            throw new RuntimeException("Class '" + declaringClass.name + "' not found")
        }
        return jvmClass
    }

    JvmClass require(PythonCompiler compiler, Type type) {
        if (!(load(compiler, type))) {
            ClassPath classPath = ClassPath.of(type)
            return PythonCompiler.expectations.expectClass(compiler, classPath.path(), classPath.name())
        }
        return get(type)
    }

    JvmClass require(PythonCompiler compiler, Class<?> type, Location location) {
        if (type == null) {
            throw new NullPointerException("Class is null")
        }
        if (!(load(compiler, type))) {
            throw new CompilerException("Class '" + type.name + "' not found", location)
        }
        return get(type)
    }

    JvmClass object(PythonCompiler compiler) {
        return require(compiler, Type.getObjectType("java/lang/Object"))
    }

    JvmClass void_(PythonCompiler compiler) {
        return require(compiler, Type.VOID_TYPE)
    }

    JvmClass require(Type type) {
        return require(PythonCompiler.current, type)
    }

    JvmClass require(Class<?> type, Location location) {
        return require(PythonCompiler.current, type, location)
    }
}
