package dev.ultreon.pythonc;

import dev.ultreon.pythonc.expr.PyExpression;
import dev.ultreon.pythonc.expr.PySymbol;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.Map;

public class SymbolReferenceExpr extends PyExpression {
    private final String name;

    public SymbolReferenceExpr(String name, Location location) {
        super(location);
        this.name = name;
    }

    @Override
    public void writeCode(PythonCompiler compiler, JvmWriter writer) {
        PySymbol symbol = compiler.getSymbol(name);
        if (symbol == null) {
            throw new CompilerException("Undefined symbol: " + name, location());
        }

        symbol.write(compiler, writer);
    }

    public void writeCall(PythonCompiler compiler, JvmWriter writer, List<PyExpression> args, Map<String, PyExpression> kwargs) {
        PySymbol symbol = compiler.getSymbol(name);
        if (symbol == null) {
            throw new CompilerException("Undefined symbol: " + name, location());
        }

        symbol.writeCall(compiler, writer, args, kwargs);
    }

    public PySymbol symbol() {
        PySymbol symbol = PythonCompiler.current().getSymbol(name);
        if (symbol == null) {
            throw new CompilerException("Undefined symbol: " + name, location());
        } else {
            return symbol;
        }
    }
}
