package dev.ultreon.pythonc;

import org.jetbrains.annotations.Nullable;

public interface ConditionContext extends Context {
    @Nullable org.objectweb.asm.Label ifTrue();
    @Nullable org.objectweb.asm.Label ifFalse();
}
