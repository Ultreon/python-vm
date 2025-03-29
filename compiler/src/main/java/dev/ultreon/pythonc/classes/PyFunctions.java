package dev.ultreon.pythonc.classes;

import dev.ultreon.pythonc.functions.PyFunction;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PyFunctions implements Iterable<PyFunction> {
    private final Map<String, List<PyFunction>> functions = new HashMap<>();

    public List<PyFunction> get(String name) {
        return functions.get(name);
    }

    public Map<String, List<PyFunction>> functions() {
        return functions;
    }

    @Override
    public @NotNull Iterator<PyFunction> iterator() {
        return functions.values().stream().flatMap(Collection::stream).iterator();
    }

    public void add(PyFunction function) {
        functions.computeIfAbsent(function.name(), k -> new ArrayList<>()).add(function);
    }
}
