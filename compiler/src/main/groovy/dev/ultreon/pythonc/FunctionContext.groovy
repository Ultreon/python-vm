package dev.ultreon.pythonc

import dev.ultreon.pythonc.expr.PySymbol
import dev.ultreon.pythonc.functions.PyFunction
import dev.ultreon.pythonc.functions.FunctionDefiner
import dev.ultreon.pythonc.functions.param.PyParameter
import org.objectweb.asm.Label

class FunctionContext extends SymbolContext {
    private final FunctionDefiner owner
    private final PyFunction function
    private final String name
    private final PyParameter[] parameters
    Label head

    protected FunctionContext(FunctionDefiner owner, PyFunction function, String name, PyParameter[] parameters, SymbolProvider symbolContext) {
        super(symbolContext)
        this.owner = owner
        this.function = function
        this.name = name
        this.parameters = parameters

        for (PyParameter parameter : parameters) {
            this.setSymbol(parameter.name, parameter)
        }
    }

    @Override
    PySymbol getSymbol(String name) {
        if (name == "self" && !function.static) {
            return function.selfSymbol
        }

        def symbol = super.getSymbol(name)
        if (symbol == null) {
            if (parent instanceof ModuleContext) {
                return new PyDynamicSymbol(owner, name, location())
            }
        }

        return symbol
    }

    FunctionDefiner owner() {
        return owner
    }

    PyFunction function() {
        return function
    }

    String name() {
        return name
    }

    PyParameter[] parameters() {
        return parameters
    }

    static FunctionContext pushContext(FunctionDefiner owner, PyFunction function, String name, PyParameter[] parameters) {
        FunctionContext functionContext = new FunctionContext(owner, function, name, parameters, current())
        PythonCompiler.current.pushContext(functionContext)
        return functionContext
    }

    Location location() {
        return function.location
    }
}
