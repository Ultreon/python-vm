package dev.ultreon.pythonc.classes

import dev.ultreon.pythonc.fields.PyField
import dev.ultreon.pythonc.functions.PyFunction
import org.jetbrains.annotations.NotNull

class PyFields implements Iterable<PyFunction> {
    private final Map<String, PyField> functions = new HashMap<>()

    PyField getAt(String name) {
        return functions.get(name)
    }

    Map<String, PyField> functions() {
        return functions
    }

    @Override
    @NotNull Iterator<PyField> iterator() {
        return functions.values()
                .iterator() as Iterator<PyField>
    }

    void plusEquals(PyField field) {
        functions[field.name] = field
    }
}
