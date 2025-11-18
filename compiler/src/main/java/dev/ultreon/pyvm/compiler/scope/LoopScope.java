package dev.ultreon.pyvm.compiler.scope;

import org.objectweb.asm.tree.LabelNode;

public interface LoopScope extends Scope {
    LabelNode getContinueLabel();
}
