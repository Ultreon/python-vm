package dev.ultreon.pythonc;

import org.jetbrains.annotations.Nullable
import org.objectweb.asm.Label;

interface ConditionContext extends Context {
    @Nullable Label ifTrue()
    @Nullable Label ifFalse()
}
