package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

public class PyStarExpressionsNode implements PyExprNode {
    private final List<PyStarExpressionNode> starExpressions;

    public PyStarExpressionsNode(List<PyStarExpressionNode> starExpressions) {
        this.starExpressions = starExpressions;
    }

    public List<PyStarExpressionNode> getStarExpressions() {
        return this.starExpressions;
    }

    @Override
    public Type getResolvedType(PyCompileContext context) {
        return Type.getType(Object.class);
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        for (PyStarExpressionNode expr : this.starExpressions) {
            expr.compile(method, context);
        }
    }
}
