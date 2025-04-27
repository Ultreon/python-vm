package dev.ultreon.pythonc.classes

import dev.ultreon.pythonc.functions.PyFunction
import org.jetbrains.annotations.NotNull

class PyFunctions implements Iterable<PyFunction> {
    private final Map<String, SequencedSet<PyFunction>> functions = new HashMap<>()

    List<PyFunction> get(String name) {
        def get = functions.get(name)
        if (get == null) {
            return List.of()
        }
        return List.copyOf(get)
    }

    Map<String, SequencedSet<PyFunction>> functions() {
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
        functions.computeIfAbsent(function.name, k -> new LinkedHashSet<>()).add function
    }
}
