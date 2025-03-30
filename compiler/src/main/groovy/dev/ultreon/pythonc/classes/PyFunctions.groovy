package dev.ultreon.pythonc.classes

import dev.ultreon.pythonc.functions.PyFunction
import org.jetbrains.annotations.NotNull

class PyFunctions implements Iterable<PyFunction> {
    private final Map<String, List<PyFunction>> functions = new HashMap<>()

    List<PyFunction> get(String name) {
        return functions.get(name)
    }

    Map<String, List<PyFunction>> functions() {
        return functions
    }

    @Override
    @NotNull Iterator<PyFunction> iterator() {
        return functions.values()
                .stream()
                .flatMap(Collection::stream)
                .iterator() as Iterator<PyFunction>
    }

    void add(PyFunction function) {
        functions.computeIfAbsent(function.name, k -> new ArrayList<>()).add function
    }
}
