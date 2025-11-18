package dev.ultreon.pyvm.compiler.util;

import dev.ultreon.pyvm.compiler.ast.PyNode;
import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Type;

public interface PyType extends PyNode {
    String getName();

    Type getAsmType(PyCompileContext context);
    String getAsmSignature();
}
