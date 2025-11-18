package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

public class PyRefNode implements PyStarAtomNode {
    private final String text;

    public PyRefNode(String text) {
        this.text = text;
    }

    @Override
    public Type getResolvedType(PyCompileContext context) {
        return Type.getType(Object.class);
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        context.getLocal(text);
    }

    @Override
    public void compileSet(MethodNode method, PyCompileContext context, PyExprNode value) {
        context.setLocal(text, value);
    }
}
