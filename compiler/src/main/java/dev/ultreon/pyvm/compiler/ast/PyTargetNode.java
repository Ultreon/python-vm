package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.tree.MethodNode;

public interface PyTargetNode extends PyNode {
    void compileSet(MethodNode method, PyCompileContext context, PyExprNode value);
}
