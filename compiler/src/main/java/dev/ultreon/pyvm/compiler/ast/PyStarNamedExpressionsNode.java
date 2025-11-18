package dev.ultreon.pyvm.compiler.ast;

import java.util.List;

public class PyStarNamedExpressionsNode implements PyNode {
    private final List<PyExprNode> starNamedExpressionNodes;

    public PyStarNamedExpressionsNode(List<PyExprNode> starNamedExpressionNodes) {
        this.starNamedExpressionNodes = starNamedExpressionNodes;
    }

    public List<PyExprNode> getStarNamedExpressionNodes() {
        return this.starNamedExpressionNodes;
    }
}
