package dev.ultreon.pyvm.compiler.ast;

public class PyImportFromAsNameNode implements PyNode {
    private final String name;
    private final String asName;

    public PyImportFromAsNameNode(String name, String asName) {
        this.name = name;
        this.asName = asName;
    }

    public String getName() {
        return this.name;
    }

    public String getAsName() {
        return this.asName;
    }
}
