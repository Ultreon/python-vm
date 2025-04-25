package dev.ultreon.pythonc.statement

import dev.ultreon.pythonc.*
import dev.ultreon.pythonc.expr.PyExpression
import dev.ultreon.pythonc.expr.PySymbol
import dev.ultreon.pythonc.expr.Settable

class PyFromImportStatement implements PyImportStatementLike {
    private final ModulePath path
    private final List<PyAlias> names
    private final Location location

    PyFromImportStatement(ModulePath path, List<PyAlias> names, Location location) {
        this.path = path
        this.names = names
        this.location = location
    }

    static Builder builder(ModulePath path, Location location) {
        return new Builder(path, location)
    }

    @Override
    void writeStatement(PythonCompiler compiler, JvmWriter writer) {
        for (PyAlias name : names) {
            importAlias(compiler, writer, name)
        }
    }

    private void importAlias(PythonCompiler compiler, JvmWriter writer, PyAlias name) {
        Settable variable
        if (compiler.definingFunction != null) {
            variable = compiler.definingFunction.defineVariable(name.alias ?: name.name, location)
            SymbolContext.current().setSymbol(name.alias ?: name.name, new ImportedSymbol(name, path, variable, PythonCompiler.classCache.require(name.asType(path)), location))
        } else if (compiler.definingClass != null) {
            variable = compiler.definingClass.defineVariable(name.alias ?: name.name, location)
            SymbolContext.current().setSymbol(name.alias ?: name.name, new ImportedSymbol(name, path, variable, PythonCompiler.classCache.require(name.asType(path)), location))
        } else if (compiler.definingModule != null) {
            variable = compiler.definingModule.defineVariable(name.alias ?: name.name, location)
            SymbolContext.current().setSymbol(name.alias ?: name.name, new ImportedSymbol(name, path, variable, PythonCompiler.classCache.require(name.asType(path)), location))
        } else {
            throw new RuntimeException("Not defining!")
        }
        variable.set(compiler, writer, new DynamicExpression(name, path, location))
        compiler.checkPop(location)
    }

    static class DynamicExpression extends PyExpression {
        private final ModulePath path
        private final PyAlias name

        DynamicExpression(PyAlias name, ModulePath path, Location location) {
            super(location)
            this.path = path
            this.name = name
        }

        @Override
        void writeCode(PythonCompiler compiler, JvmWriter writer) {
            compiler.writer.dynamicImport(path, name)
        }
    }

    static class ImportedSymbol extends PyExpression implements PySymbol, Settable {
        private final PyAlias name
        private final Settable variable
        private final ModulePath path
        private final PySymbol value

        ImportedSymbol(PyAlias name, ModulePath path, Settable variable, PySymbol value = null, Location location) {
            super(location)
            this.name = name
            this.variable = variable
            this.path = path
            this.value = value
        }

        PySymbol getValue() {
            return value
        }

        ModulePath getPath() {
            return path
        }

        @Override
        String getName() {
            return name.getName()
        }

        @Override
        void writeCall(PythonCompiler compiler, JvmWriter writer, List<PyExpression> args, Map<String, PyExpression> kwargs) {
            variable.writeCode(compiler, writer)
            writer.createArgs(args)
            writer.createKwargs(kwargs)
            writer.dynamicCall()
        }

        @Override
        void writeCode(PythonCompiler compiler, JvmWriter writer) {
            variable.writeCode(compiler, writer)
        }

        @Override
        void set(PythonCompiler compiler, JvmWriter writer, PyExpression expr) {
            variable.set(compiler, writer, expr)
            compiler.checkPop(location)
        }
    }

    String toString() {
        StringBuilder builder = new StringBuilder()

        builder.append(Location.ANSI_RED).append("FromImport").append(Location.ANSI_RESET).append(" (").append(Location.ANSI_PURPLE).append(path).append(Location.ANSI_RESET).append(") :: [")
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) builder.append(Location.ANSI_WHITE).append(", ")
            builder.append(Location.ANSI_RESET).append(names.get(i))
        }
        builder.append(Location.ANSI_RESET).append("]").append(Location.ANSI_RESET)

        return builder.toString()
    }

    Location getLocation() {
        return location
    }

    static class Builder {
        private final ModulePath path
        private final List<PyAlias> names = new ArrayList<>()
        private final Location location

        Builder(ModulePath path, Location location) {
            this.path = path
            this.location = location
        }

        Builder alias(PyAlias name) {
            names.add(name)
            return this
        }

        Builder name(String name, Location location) {
            names.add(new PyAlias(name, location))
            return this
        }

        Builder alias(String name, String alias, Location location) {
            names.add(new PyAlias(name, alias, location))
            return this
        }

        PyFromImportStatement build() {
            return new PyFromImportStatement(path, names, location)
        }
    }
}
