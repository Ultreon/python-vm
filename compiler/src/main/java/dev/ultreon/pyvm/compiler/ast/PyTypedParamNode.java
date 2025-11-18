package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Type;

public class PyTypedParamNode implements PyParamNode {
    private final String name;
    private final PyTypeAnnotationNode node;

    public PyTypedParamNode(String name, PyTypeAnnotationNode node) {
        this.name = name;
        this.node = node;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public Type getResolvedType(PyCompileContext context) {
        return this.node.getResolvedType(context);
    }
}
