package dev.ultreon.pyvm.compiler.util;

import dev.ultreon.pyvm.compiler.ast.PyNode;
import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.tree.MethodNode;

public interface PyImport extends PyNode {
    void compile(MethodNode method, PyCompileContext context);
}
