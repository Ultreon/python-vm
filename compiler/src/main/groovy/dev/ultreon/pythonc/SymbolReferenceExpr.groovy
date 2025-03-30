package dev.ultreon.pythonc

import dev.ultreon.pythonc.expr.PyExpression
import dev.ultreon.pythonc.expr.PySymbol

class SymbolReferenceExpr extends PyExpression {
    private final String name

    SymbolReferenceExpr(String name, Location location) {
        super(location)
        this.name = name
    }

    @Override
    void writeCode(PythonCompiler compiler, JvmWriter writer) {
        PySymbol symbol = compiler.getSymbol(name)
        if (symbol == null) {
            throw new CompilerException("Undefined symbol: " + name, location)
        }

        symbol.write(compiler, writer)
    }

    void writeCall(PythonCompiler compiler, JvmWriter writer, List<PyExpression> args, Map<String, PyExpression> kwargs) {
        PySymbol symbol = compiler.getSymbol(name)
        if (symbol == null) {
            throw new CompilerException("Undefined symbol: " + name, location)
        }

        symbol.writeCall(compiler, writer, args, kwargs)
    }

    PySymbol symbol() {
        PySymbol symbol = PythonCompiler.current.getSymbol(name)
        if (symbol == null) {
            throw new CompilerException("Undefined symbol: " + name, location)
        } else {
            return symbol
        }
    }
}
