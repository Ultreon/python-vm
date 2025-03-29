package dev.ultreon.pythonc;

import dev.ultreon.pythonc.classes.PyBuiltinClass;
import dev.ultreon.pythonc.expr.PySymbol;
import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.Map;

public class PyImports {
    private final Map<String, PySymbol> byName = new HashMap<>();
    private final Map<String, PySymbol> byAlias = new HashMap<>();
    private final PythonCompiler compiler;

    public PyImports(PythonCompiler compiler) {
        this.compiler = compiler;

        add("object", new PyImportSymbol("object", new PyBuiltinClass(Type.getType(Object.class), "Lorg/pythonPythonObject;", "object"), new Location()));
    }

    public void add(String alias, PySymbol symbol) {
        byName.put(alias, symbol);
        byAlias.put(alias, symbol);
        compiler.setSymbol(alias, symbol);
    }

    public PySymbol get(String name) {
        if (byAlias.containsKey(name)) {
            return byAlias.get(name);
        }

        return byName.get(name);
    }
}
