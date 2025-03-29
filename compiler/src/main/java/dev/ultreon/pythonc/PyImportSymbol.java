package dev.ultreon.pythonc;

import dev.ultreon.pythonc.expr.PyExpression;
import dev.ultreon.pythonc.expr.PySymbol;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.Map;

public class PyImportSymbol extends PyExpression implements PySymbol {
    private String alias;
    private final PySymbol symbol;
    private final Location location;

    public PyImportSymbol(String alias, PySymbol symbol, Location location) {
        super(location);
        this.alias = alias;
        this.symbol = symbol;
        this.location = location;
    }

    @Override
    public String name() {
        return alias;
    }

    @Override
    public void writeCall(PythonCompiler compiler, JvmWriter writer, List<PyExpression> args, Map<String, PyExpression> kwargs) {
        write(compiler, writer);
        writer.createArgs(args);
        writer.createKwargs(kwargs);
        writer.dynamicCall();
    }

    public PySymbol symbol() {
        return symbol;
    }

    @Override
    public void writeCode(PythonCompiler compiler, JvmWriter writer) {
        symbol.write(compiler, writer);
    }

    @Override
    public Type type() {
        if (symbol == null) {
            throw new CompilerException("Unknown symbol: " + alias, location);
        }
        if (symbol instanceof PyExpression expression) {
            return expression.type();
        }

        return Type.getType(Object.class);
    }
}
