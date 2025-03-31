package dev.ultreon.pythonc.functions

import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.expr.VariableExpr

class PyVariables {
    private final Map<String, VariableExpr> byName = new HashMap<>()

    void add(int idx, String name, Location location) {
        byName.put(name, new VariableExpr(idx, name, location))
    }

    VariableExpr get(String name) {
        return byName.get(name)
    }

    boolean contains(String name) {
        return byName.containsKey(name)
    }
}
