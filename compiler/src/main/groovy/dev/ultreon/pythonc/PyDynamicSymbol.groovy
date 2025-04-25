package dev.ultreon.pythonc

import dev.ultreon.pythonc.expr.PyExpression
import dev.ultreon.pythonc.expr.PySymbol
import dev.ultreon.pythonc.functions.FunctionDefiner

class PyDynamicSymbol extends PyExpression implements PySymbol {
    private final FunctionDefiner functionDefiner
    private final String name

    PyDynamicSymbol(FunctionDefiner functionDefiner, String name, Location location) {
        super(location)
        this.functionDefiner = functionDefiner
        this.name = name
    }

    @Override
    void writeCode(PythonCompiler compiler, JvmWriter writer) {
        functionDefiner.attr(name, location).writeCode(compiler, writer)
    }

    @Override
    String getName() {
        return null
    }

    @Override
    void writeCall(PythonCompiler compiler, JvmWriter writer, List<PyExpression> args, Map<String, PyExpression> kwargs) {
        functionDefiner.call(name, args, kwargs, location).writeCode(compiler, writer)
    }
}
