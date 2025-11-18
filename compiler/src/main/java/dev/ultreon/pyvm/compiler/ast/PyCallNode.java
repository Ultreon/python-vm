package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

public class PyCallNode implements PyExprNode {
    private final PyExprNode expr;
    private final PyArgumentsNode arguments;

    public PyCallNode(PyExprNode expr, PyArgumentsNode arguments) {
        this.expr = expr;
        this.arguments = arguments;
    }

    @Override
    public Type getResolvedType(PyCompileContext context) {
        return Type.getType(Object.class);
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        PyExprNode[] positionalArguments = arguments.getPositionalArguments();
        PyKeywordArgNode[] keywordArguments = arguments.getKeywordArguments();
        PyExprNode starredArgument = arguments.getStarredArgument();
        PyExprNode doubleStarredArgument = arguments.getDoubleStarredArgument();

        if (starredArgument != null) {
            throw new UnsupportedOperationException("*args expressions are not supported yet");
        }

        if (doubleStarredArgument != null) {
            throw new UnsupportedOperationException("**kwargs expressions are not supported yet");
        }

        this.expr.compile(method, context);
        context.call(positionalArguments, keywordArguments);
    }
}
