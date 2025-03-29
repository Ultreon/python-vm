package dev.ultreon.pythonc;

import dev.ultreon.pythonc.expr.MemberAttrExpr;
import dev.ultreon.pythonc.expr.PySymbol;

import java.util.HashMap;
import java.util.Map;

public class SymbolContext extends AbstractContext implements SymbolProvider {
    private final Map<String, PySymbol> symbols = new HashMap<>();
    private final SymbolProvider parent;

    protected SymbolContext(SymbolProvider symbolContext) {
        parent = symbolContext;
    }

    protected static SymbolProvider current() {
        return PythonCompiler.current().lastContext(SymbolProvider.class);
    }

    @Override
    public PySymbol getSymbol(String name) {
        PySymbol pySymbol = symbols.get(name);
        if (pySymbol == null && parent != null)
            pySymbol = parent.getSymbol(name);
        return pySymbol;
    }

    @Override
    public void setSymbol(String name, PySymbol symbol) {
        if (PythonCompiler.current().unlocked.contains(name)) {
            throw new TODO();
        }
        symbols.put(name, symbol);
    }

    public static SymbolContext pushContext(PythonCompiler compiler) {
        SymbolContext symbolContext = compiler.lastContext(SymbolContext.class);
        SymbolContext context = new SymbolContext(symbolContext);
        compiler.pushContext(context);
        return context;
    }

    @Override
    public boolean hasSymbol(String name) {
        return symbols.containsKey(name);
    }

    @Override
    public Map<String, PySymbol> symbols() {
        return symbols;
    }

    @Override
    public PySymbol getSymbolToSet(String name) {
        PySymbol pySymbol = symbols.get(name);
        if (pySymbol == null && parent != null) {
            if (!PythonCompiler.current().unlocked.contains(name)) return null;
            else pySymbol = parent.getSymbol(name);
        }
        return pySymbol;
    }
}
