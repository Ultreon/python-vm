package dev.ultreon.pyvm.compiler.ast;

public class PyDottedAsNamesNode implements PyNode {
    private final PyDottedAsNameNode[] dottedAsNameNodes;

    public PyDottedAsNamesNode(PyDottedAsNameNode[] dottedAsNameNodes) {
        this.dottedAsNameNodes = dottedAsNameNodes;
    }

    public PyDottedAsNameNode[] getDottedAsNameNodes() {
        return this.dottedAsNameNodes;
    }
}
