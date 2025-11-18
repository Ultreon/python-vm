package dev.ultreon.pyvm.compiler.ast;

public class PyImportFromTargetsNode implements PyNode {
    private final PyImportFromAsNameNode[] asNameNodes;

    public PyImportFromTargetsNode(PyImportFromAsNameNode[] asNameNodes) {
        this.asNameNodes = asNameNodes;
    }

    public PyImportFromAsNameNode[] getAsNameNodes() {
        return asNameNodes;
    }
}
