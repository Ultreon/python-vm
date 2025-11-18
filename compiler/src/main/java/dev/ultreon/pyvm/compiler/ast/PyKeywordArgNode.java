package dev.ultreon.pyvm.compiler.ast;

public class PyKeywordArgNode implements PyNode {
    private final String name;
    private final PyExprNode value;

    public PyKeywordArgNode(String name, PyExprNode value) {
        if (name == null || value == null) {
            throw new IllegalArgumentException("Arguments cannot be null");
        }

        this.name = name;
        this.value = value;
    }

    public String getName() {
        return this.name;
    }

    public PyExprNode getValue() {
        return this.value;
    }
}
