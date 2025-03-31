package dev.ultreon.pythonc.statement

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.ModulePath
import dev.ultreon.pythonc.PyAlias
import dev.ultreon.pythonc.PythonCompiler

class PyFromImportStatement implements PyImportStatementLike {
    private final ModulePath path
    private final List<PyAlias> names

    PyFromImportStatement(ModulePath path, List<PyAlias> names) {
        this.path = path
        this.names = names
    }

    static Builder builder(ModulePath path) {
        return new Builder(path)
    }

    @Override
    void writeStatement(PythonCompiler compiler, JvmWriter writer) {
        for (PyAlias name : names) {
            importAlias(compiler, name)
        }
    }

    private void importAlias(PythonCompiler compiler, PyAlias name) {
        compiler.imports.add(name.alias() == null ? name.name() : name.alias(), PythonCompiler.classCache.require(name.asType(path)))
    }

    @Override
    Location getLocation() {
        return null
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

    static class Builder {
        private final ModulePath path
        private final List<PyAlias> names = new ArrayList<>()

        Builder(ModulePath path) {
            this.path = path
        }

        Builder alias(PyAlias name) {
            names.add(name)
            return this
        }

        Builder name(String name) {
            names.add(new PyAlias(name))
            return this
        }

        Builder alias(String name, String alias) {
            names.add(new PyAlias(name, alias))
            return this
        }

        PyFromImportStatement build() {
            return new PyFromImportStatement(path, names)
        }
    }
}
