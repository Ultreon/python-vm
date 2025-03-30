package dev.ultreon.pythonc

import dev.ultreon.pythonc.expr.PyExpression
import dev.ultreon.pythonc.expr.PySymbol
import org.objectweb.asm.Type

class PyImportSymbol extends PyExpression implements PySymbol {
    private String alias
    private final PySymbol symbol
    private final Location location

    PyImportSymbol(String alias, PySymbol symbol, Location location) {
        super(location)
        this.alias = alias
        this.symbol = symbol
        this.location = location
    }

    @Override
    String getName() {
        return alias
    }

    @Override
    void writeCall(PythonCompiler compiler, JvmWriter writer, List<PyExpression> args, Map<String, PyExpression> kwargs) {
        write(compiler, writer)
        writer.createArgs(args)
        writer.createKwargs(kwargs)
        writer.dynamicCall()
    }

    PySymbol symbol() {
        return symbol
    }

    @Override
    void writeCode(PythonCompiler compiler, JvmWriter writer) {
        symbol.write(compiler, writer)
    }

    @Override
    Type getType() {
        if (symbol == null) {
            throw new CompilerException("Unknown symbol: " + alias, location)
        }
        if (symbol instanceof PyExpression) {
            PyExpression expression = (PyExpression) symbol
            return expression.type
        }

        return Type.getType(Object.class)
    }
}
