package dev.ultreon.pythonc;

import dev.ultreon.pythonc.functions.PyFunction;
import dev.ultreon.pythonc.functions.FunctionDefiner;
import dev.ultreon.pythonc.functions.param.PyParameter;

public class FunctionContext extends SymbolContext {
    private final FunctionDefiner owner;
    private final PyFunction function;
    private final String name;
    private final PyParameter[] parameters;

    protected FunctionContext(FunctionDefiner owner, PyFunction function, String name, PyParameter[] parameters, SymbolProvider symbolContext) {
        super(symbolContext);
        this.owner = owner;
        this.function = function;
        this.name = name;
        this.parameters = parameters;

        for (PyParameter parameter : parameters) {
            this.setSymbol(parameter.name(), parameter);
        }
    }

    public FunctionDefiner owner() {
        return owner;
    }

    public PyFunction function() {
        return function;
    }

    public String name() {
        return name;
    }

    public PyParameter[] parameters() {
        return parameters;
    }

    public static FunctionContext pushContext(FunctionDefiner owner, PyFunction function, String name, PyParameter[] parameters) {
        FunctionContext functionContext = new FunctionContext(owner, function, name, parameters, SymbolContext.current());
        PythonCompiler.current().pushContext(functionContext);
        return functionContext;
    }

    public Location location() {
        return function.location();
    }
}
