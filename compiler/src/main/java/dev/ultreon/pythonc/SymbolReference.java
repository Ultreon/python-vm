package dev.ultreon.pythonc;

import dev.ultreon.pythonc.expr.PySymbol;

public interface SymbolReference {
    PySymbol resolve(PythonCompiler compiler);
}
