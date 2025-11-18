package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.tree.MethodNode;

public interface PyFStringMiddleNode extends PyNode {

    void compile(MethodNode method, PyCompileContext context);
}
