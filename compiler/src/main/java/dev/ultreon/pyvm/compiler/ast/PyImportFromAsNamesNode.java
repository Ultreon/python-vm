package dev.ultreon.pyvm.compiler.ast;

public class PyImportFromAsNamesNode implements PyNode {
    private final PyImportFromAsNameNode[] asNames;

    public PyImportFromAsNamesNode(PyImportFromAsNameNode[] asNames) {
        this.asNames = asNames;
    }

    public PyImportFromAsNameNode[] getAsNames() {
        return this.asNames;
    }
}
