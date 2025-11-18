package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import dev.ultreon.pyvm.compiler.util.PyType;
import org.objectweb.asm.Type;

public class PyTypeAnnotationNode implements PyNode {
    private final PyType type;

    public PyTypeAnnotationNode(PyType type) {
        this.type = type;
    }

    public String getName() {
        return this.type.getName();
    }

    public Type getResolvedType(PyCompileContext context) {
        return this.type.getAsmType(context);
    }
}
