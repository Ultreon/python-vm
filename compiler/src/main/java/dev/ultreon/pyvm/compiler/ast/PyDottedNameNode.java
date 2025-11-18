package dev.ultreon.pyvm.compiler.ast;

public class PyDottedNameNode implements PyNode {
    private final String name;

    public PyDottedNameNode(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
