package dev.ultreon.pythonc

class PyFromImportStatement extends PyImportStatementLike {
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
