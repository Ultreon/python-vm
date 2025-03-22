package dev.ultreon.pythonc;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;

public class WhileConditionContext extends AbstractContext implements ConditionContext {
    public final Label loopEnd;

    public WhileConditionContext(Label loopEnd) {
        this.loopEnd = loopEnd;
    }

    @Override
    public @Nullable Label ifTrue() {
        return null;
    }

    @Override
    public @Nullable Label ifFalse() {
        return loopEnd;
    }
}
