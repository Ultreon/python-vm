package dev.ultreon.pyvm.compiler.ast;

public class PyArgumentsNode implements PyNode {
    private final PyExprNode[] positionalArguments;
    private final PyExprNode starredArgument;
    private final PyKeywordArgNode[] keywordArguments;
    private final PyExprNode doubleStarredArguments;

    public PyArgumentsNode(PyExprNode[] positionalArguments, PyExprNode starredArgument, PyKeywordArgNode[] keywordArguments, PyExprNode doubleStarredArguments) {
        this.positionalArguments = positionalArguments;
        this.starredArgument = starredArgument;
        this.keywordArguments = keywordArguments;
        this.doubleStarredArguments = doubleStarredArguments;
    }

    public PyExprNode[] getPositionalArguments() {
        return this.positionalArguments;
    }

    public PyExprNode getStarredArgument() {
        return this.starredArgument;
    }

    public PyKeywordArgNode[] getKeywordArguments() {
        return this.keywordArguments;
    }

    public PyExprNode getDoubleStarredArgument() {
        return this.doubleStarredArguments;
    }
}
