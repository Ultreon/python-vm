package dev.ultreon.pythonc;

import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.Map;

public class PyImports {
    private final Map<String, Symbol> byName = new HashMap<>();
    private final Map<String, Symbol> byAlias = new HashMap<>();
    private final Map<Type, Symbol> byType = new HashMap<>();
    private final PythonCompiler compiler;

    public PyImports(PythonCompiler compiler) {
        this.compiler = compiler;

        add("object", new PyImport("object", new PyBuiltinClass(Type.getType(Object.class), "Lorg/pythonPythonObject;", "object")));
    }

    public void add(String alias, Symbol symbol) {
        byName.put(alias, symbol);
        byAlias.put(alias, symbol);
        byType.put(symbol.type(compiler), symbol);

        compiler.symbols.put(alias, new PyImport(alias, symbol));
    }

    public Symbol get(String name) {
        if (byAlias.containsKey(name)) {
            return byAlias.get(name);
        }

        return byName.get(name);
    }

    public Symbol get(Type type) {
        return byType.get(type);
    }

    public void remove(String s) {
        Symbol remove = byAlias.remove(s);
        if (remove != null) {
            byName.remove(s);
            byType.remove(remove.type(compiler));
            return;
        }
        remove = byName.remove(s);
        if (remove == null) {
            return;
        }
        byType.remove(remove.type(compiler));
    }
}
