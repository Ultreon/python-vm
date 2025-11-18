package dev.ultreon.pyvm.compiler.scope;

import org.objectweb.asm.tree.LabelNode;

public interface Scope {

    Scope getParent();

    LabelNode getExitLabel();
}
