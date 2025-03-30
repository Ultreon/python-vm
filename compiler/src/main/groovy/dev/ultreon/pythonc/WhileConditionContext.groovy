package dev.ultreon.pythonc;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;

class WhileConditionContext extends AbstractContext implements ConditionContext {
    final Label loopEnd;
    final Label elseBlock;

    WhileConditionContext(Label loopEnd, Label elseBlock) {
        this.loopEnd = loopEnd;
        this.elseBlock = elseBlock;
    }

    @Override
    @Nullable Label ifTrue() {
        return null;
    }

    @Override
    @Nullable Label ifFalse() {
        return elseBlock != null ? elseBlock : loopEnd;
    }
}
