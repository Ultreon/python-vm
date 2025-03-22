package dev.ultreon.pythonc;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;

public class WhileConditionContext extends AbstractContext implements ConditionContext {
    public final Label loopEnd;
    public final Label elseBlock;

    public WhileConditionContext(Label loopEnd, Label elseBlock) {
        this.loopEnd = loopEnd;
        this.elseBlock = elseBlock;
    }

    @Override
    public @Nullable Label ifTrue() {
        return null;
    }

    @Override
    public @Nullable Label ifFalse() {
        return elseBlock != null ? elseBlock : loopEnd;
    }
}
