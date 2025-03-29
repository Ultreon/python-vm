package dev.ultreon.pythonc;

import java.util.ArrayList;
import java.util.List;

public class PyFromImportStatement extends PyImportStatementLike {
    private final ModulePath path;
    private final List<PyAlias> names;

    public PyFromImportStatement(ModulePath path, List<PyAlias> names) {
        this.path = path;
        this.names = names;
    }

    public static PyFromImportStatement.Builder builder(ModulePath path) {
        return new PyFromImportStatement.Builder(path);
    }

    @Override
    public void writeStatement(PythonCompiler compiler, JvmWriter writer) {
        for (PyAlias name : names) {
            importAlias(compiler, name);
        }
    }

    private void importAlias(PythonCompiler compiler, PyAlias name) {
        compiler.imports.add(name.alias() == null ? name.name() : name.alias(), PythonCompiler.classCache.require(name.asType(path)));
    }

    @Override
    public Location location() {
        return null;
    }

    public static class Builder {
        private final ModulePath path;
        private final List<PyAlias> names = new ArrayList<>();

        public Builder(ModulePath path) {
            this.path = path;
        }

        public Builder alias(PyAlias name) {
            names.add(name);
            return this;
        }

        public Builder name(String name) {
            names.add(new PyAlias(name));
            return this;
        }

        public Builder alias(String name, String alias) {
            names.add(new PyAlias(name, alias));
            return this;
        }

        public PyFromImportStatement build() {
            return new PyFromImportStatement(path, names);
        }
    }
}
