package dev.ultreon.pythonc

import dev.ultreon.pythonc.classes.LangClass
import org.jetbrains.annotations.NotNull
import org.objectweb.asm.Type

import static org.objectweb.asm.Type.getObjectType as objectType

class PyClasses implements Iterable<LangClass> {
    private final Map<String, LangClass> byName = new HashMap<>()
    private final Map<String, LangClass> byClassName = new HashMap<>()
    private final Map<Type, LangClass> byType = new HashMap<>()

    def add(LangClass pyClass) {
        byName[pyClass.name] = pyClass
        byClassName[pyClass.className()] = pyClass
        byType[objectType(pyClass.className())] = pyClass
    }

    LangClass byName(String name) {
        return byName.get(name)
    }

    LangClass byClassName(String className) {
        return byClassName.get(className)
    }

    LangClass byType(Type type) {
        return byType.get(type)
    }

    boolean hasName(String name) {
        return byName.containsKey(name)
    }

    boolean hasClassName(String className) {
        return byClassName.containsKey(className)
    }

    boolean hasType(Type type) {
        return byType.containsKey(type)
    }

    @Override
    @NotNull Iterator<LangClass> iterator() {
        return byName.values().iterator()
    }
}
