package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.tree.MethodNode;

public class PyClassDefNode implements PyStatementNode {
    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
