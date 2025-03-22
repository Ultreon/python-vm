package dev.ultreon.pythonc;

import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.Map;

public class PyClasses {
    private final Map<String, PyClass> byName = new HashMap<>();
    private final Map<String, PyClass> byClassName = new HashMap<>();
    private final Map<Type, PyClass> byType = new HashMap<>();

    private final PythonCompiler pythonCompiler;

    public PyClasses(PythonCompiler pythonCompiler) {
        this.pythonCompiler = pythonCompiler;
    }

    public void add(PyClass pyClass) {
        byName.put(pyClass.name(), pyClass);
        byClassName.put(pyClass.className(), pyClass);
        byType.put(Type.getObjectType(pyClass.className()), pyClass);
    }

    public PyClass get(String name) {
        return byName.get(name);
    }

    public JvmClass get(Type type) {
        return byType.get(type);
    }

    public boolean has(String className) {
        return byClassName.containsKey(className);
    }

    public boolean has(Type type) {
        return byType.containsKey(type);
    }
}
