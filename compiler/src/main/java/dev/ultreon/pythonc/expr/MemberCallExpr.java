package dev.ultreon.pythonc.expr;

import dev.ultreon.pythonc.*;
import dev.ultreon.pythonc.classes.PyBuiltin;
import dev.ultreon.pythonc.statement.PyStatement;
import org.jetbrains.annotations.Nullable;
import org.python._internal.Py;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MemberCallExpr extends MemberExpression {
    private final List<PyExpression> arguments;

    public MemberCallExpr(PyExpression parent, List<PyExpression> arguments, Location location) {
        super(parent, location);
        this.arguments = arguments;
    }

    public static MemberCallExpr.Builder builder(PyExpression parent, Location location) {
        return new MemberCallExpr.Builder(parent, location);
    }

    public List<PyExpression> arguments() {
        return arguments;
    }

    @Override
    public @Nullable String name() {
        return null;
    }

    @Override
    public void writeCode(PythonCompiler compiler, JvmWriter writer) {
        if (parent() instanceof SymbolReferenceExpr expr) {
            PySymbol symbol = expr.symbol();
            symbol.writeCall(compiler, writer, arguments, Map.of());
            compiler.checkNoPop(location());
            return;
        }
        writer.dynamicCall(parent(), arguments);
        compiler.checkNoPop(location());
    }

    @Override
    public void writeAttrOnly(PythonCompiler compiler, JvmWriter writer) {
        super.writeCode(compiler, writer);
    }

    public static class Builder {
        private final PyExpression parent;
        private final Location location;
        private final List<PyExpression> arguments = new ArrayList<>();

        public Builder(PyExpression parent, Location location) {
            this.parent = parent;
            this.location = location;
        }

        public Builder argument(PyExpression argument) {
            arguments.add(argument);
            return this;
        }

        public MemberCallExpr build() {
            return new MemberCallExpr(parent, arguments, location);
        }
    }
}
