package dev.ultreon.pythonc;

import dev.ultreon.pythonc.classes.LangClass;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PyClasses implements Iterable<LangClass> {
    private final Map<String, LangClass> byName = new HashMap<>();
    private final Map<String, LangClass> byClassName = new HashMap<>();
    private final Map<Type, LangClass> byType = new HashMap<>();

    public void add(LangClass pyClass) {
        byName.put(pyClass.name(), pyClass);
        byClassName.put(pyClass.className(), pyClass);
        byType.put(Type.getObjectType(pyClass.className()), pyClass);
    }

    public LangClass byName(String name) {
        return byName.get(name);
    }

    public LangClass byClassName(String className) {
        return byClassName.get(className);
    }

    public LangClass byType(Type type) {
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

    @Override
    public @NotNull Iterator<LangClass> iterator() {
        return byName.values().iterator();
    }
}
