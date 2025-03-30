package dev.ultreon.pythonc

import dev.ultreon.pythonc.expr.PySymbol

class SymbolContext extends AbstractContext implements SymbolProvider {
    private final Map<String, PySymbol> symbols = new HashMap<>()
    private final SymbolProvider parent

    protected SymbolContext(SymbolProvider symbolContext) {
        parent = symbolContext
    }

    protected static SymbolProvider current() {
        return PythonCompiler.current.lastContext(SymbolProvider.class)
    }

    @Override
    PySymbol getSymbol(String name) {
        PySymbol pySymbol = symbols.get(name)
        if (pySymbol == null && parent != null)
            pySymbol = parent.getSymbol(name)
        return pySymbol
    }

    @Override
    void setSymbol(String name, PySymbol symbol) {
        if (PythonCompiler.current.unlocked.contains(name)) {
            throw new TODO()
        }
        symbols.put(name, symbol)
    }

    static SymbolContext pushContext(PythonCompiler compiler) {
        SymbolContext symbolContext = compiler.lastContext(SymbolContext.class)
        SymbolContext context = new SymbolContext(symbolContext)
        compiler.pushContext(context)
        return context
    }

    @Override
    boolean hasSymbol(String name) {
        return symbols.containsKey(name)
    }

    @Override
    Map<String, PySymbol> symbols() {
        return symbols
    }

    @Override
    PySymbol getSymbolToSet(String name) {
        PySymbol pySymbol = symbols.get(name)
        if (pySymbol == null && parent != null) {
            if (!PythonCompiler.current.unlocked.contains(name)) return null
            else pySymbol = parent.getSymbol(name)
        }
        return pySymbol
    }
}
