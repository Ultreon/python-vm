package dev.ultreon.pyvm.compiler.util;

import dev.ultreon.pyvm.compiler.ast.PyExprNode;
import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

public class PyRefType implements PyExprNode, PyType {
    private String text;

    public PyRefType(String text) {
        this.text = text;
    }

    @Override
    public String getName() {
        return text;
    }

    @Override
    public Type getAsmType(PyCompileContext context) {
        return context.resolveType(text);
    }

    @Override
    public String getAsmSignature() {
        return "";
    }

    @Override
    public Type getResolvedType(PyCompileContext context) {
        return null;
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        context.getLocal(this.text);
    }
}
