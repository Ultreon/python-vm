package dev.ultreon.pythonc;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

public class IfStatementContext extends AbstractContext {
    public @Nullable Label elifLabel;
    public final Label endLabel;

    public IfStatementContext(@Nullable Label elifLabel, Label endLabel) {
        this.elifLabel = elifLabel;
        this.endLabel = endLabel;
    }
}
