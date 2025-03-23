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

    public PyClass byName(String name) {
        return byName.get(name);
    }

    public PyClass byClassName(String className) {
        return byClassName.get(className);
    }

    public JvmClass byType(Type type) {
        return byType.get(type);
    }

    public boolean hasName(String name) {
        return byName.containsKey(name);
    }

    public boolean hasClassName(String className) {
        return byClassName.containsKey(className);
    }

    public boolean hasType(Type type) {
        return byType.containsKey(type);
    }
}
