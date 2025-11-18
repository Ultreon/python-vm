package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Type;

public class PyNamedParamNode implements PyParamNode {
    private final String name;

    public PyNamedParamNode(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Type getResolvedType(PyCompileContext context) {
        return Type.getObjectType("python/_core/VMObject");
    }
}
