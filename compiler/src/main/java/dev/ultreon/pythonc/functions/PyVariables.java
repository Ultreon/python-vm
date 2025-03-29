package dev.ultreon.pythonc.functions;

import dev.ultreon.pythonc.Location;
import dev.ultreon.pythonc.expr.VariableExpr;

import java.util.HashMap;
import java.util.Map;

public class PyVariables {
    private final Map<String, VariableExpr> byName = new HashMap<>();

    public void add(int idx, String name, Location location) {
        byName.put(name, new VariableExpr(idx, name, location));
    }

    public VariableExpr get(String name) {
        return byName.get(name);
    }

    public boolean contains(String name) {
        return byName.containsKey(name);
    }
}
