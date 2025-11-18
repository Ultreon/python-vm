package dev.ultreon.pyvm.compiler.ast;

public class PyDottedAsNameNode implements PyNode {
    private final String dottedName;
    private final String asName;

    public PyDottedAsNameNode(String dottedName, String asName) {
        this.dottedName = dottedName;
        this.asName = asName;
    }

    public String getDottedName() {
        return this.dottedName;
    }

    public String getAsName() {
        return this.asName;
    }
}
