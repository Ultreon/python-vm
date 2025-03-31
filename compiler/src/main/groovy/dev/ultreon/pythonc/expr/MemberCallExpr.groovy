package dev.ultreon.pythonc.expr


import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler
import dev.ultreon.pythonc.SymbolReferenceExpr
import org.jetbrains.annotations.Nullable

class MemberCallExpr extends MemberExpression {
    private final List<PyExpression> arguments

    MemberCallExpr(PyExpression parent, List<PyExpression> arguments, Location location) {
        super(parent, location)
        this.arguments = arguments
    }

    static Builder builder(PyExpression parent, Location location) {
        return new Builder(parent, location)
    }

    List<PyExpression> getArguments() {
        return arguments
    }

    @Override
    @Nullable String getName() {
        return null
    }

    @Override
    void writeCode(PythonCompiler compiler, JvmWriter writer) {
        if (parent instanceof SymbolReferenceExpr) {
            def expr = parent
            def symbol = expr.symbol()
            symbol.writeCall compiler, writer, arguments, [:]
            compiler.checkNoPop location
            return
        }
        writer.dynamicCall parent, arguments
        compiler.checkNoPop location
    }

    @Override
    void writeAttrOnly(PythonCompiler compiler, JvmWriter writer) {
        super.writeCode(compiler, writer)
    }

    static class Builder {
        private final PyExpression parent
        private final Location location
        private final List<PyExpression> arguments = new ArrayList<>()

        Builder(PyExpression parent, Location location) {
            this.parent = parent
            this.location = location
        }

        Builder argument(PyExpression argument) {
            arguments.add(argument)
            return this
        }

        MemberCallExpr build() {
            return new MemberCallExpr(parent, arguments, location)
        }
    }

    String toString() {
        StringBuilder builder = new StringBuilder()

        builder.append(Location.ANSI_RED)
        builder.append("MemberCall ")
        builder.append(Location.ANSI_RESET)
        builder.append("[")
        builder.append(Location.ANSI_PURPLE)
        builder.append(parent.toString())
        builder.append(Location.ANSI_RESET)
        builder.append("] (")
        for (PyExpression argument : arguments) {
            builder.append(argument.toString())
            builder.append(Location.ANSI_RESET)
            builder.append(", ")
        }
        builder.append(Location.ANSI_RESET)
        builder.append(")")
    }
}
