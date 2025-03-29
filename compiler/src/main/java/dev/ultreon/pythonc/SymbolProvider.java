package dev.ultreon.pythonc;

import dev.ultreon.pythonc.expr.PySymbol;

import java.util.Map;

public interface SymbolProvider {
    PySymbol getSymbol(String name);

    default PySymbol getSymbol(String name, Location location) {
        return getSymbol(name);
    }

    void setSymbol(String name, PySymbol symbol);

    default void setSymbol(String name, PySymbol symbol, Location location) {
        setSymbol(name, symbol);
    }

    boolean hasSymbol(String name);

    Map<String, PySymbol> symbols();

    PySymbol getSymbolToSet(String name);
}
