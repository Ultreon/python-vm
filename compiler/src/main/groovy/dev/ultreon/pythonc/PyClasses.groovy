package dev.ultreon.pythonc

import dev.ultreon.pythonc.classes.PyClass
import org.jetbrains.annotations.NotNull
import org.objectweb.asm.Type

import static org.objectweb.asm.Type.getObjectType as objectType

class PyClasses implements Iterable<PyClass> {
    private final Map<String, PyClass> byName = new HashMap<>()
    private final Map<String, PyClass> byClassName = new HashMap<>()
    private final Map<Type, PyClass> byType = new HashMap<>()

    def add(PyClass pyClass) {
        byName[pyClass.name] = pyClass
        byClassName[pyClass.className()] = pyClass
        byType[objectType(pyClass.className())] = pyClass
    }

    PyClass byName(String name) {
        return byName.get(name)
    }

    PyClass byClassName(String className) {
        return byClassName.get(className)
    }

    PyClass byType(Type type) {
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
    @NotNull Iterator<PyClass> iterator() {
        return byName.values().iterator()
    }
}
