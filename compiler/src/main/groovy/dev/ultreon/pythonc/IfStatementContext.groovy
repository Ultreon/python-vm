package dev.ultreon.pythonc

import org.jetbrains.annotations.Nullable
import org.objectweb.asm.Label
import org.objectweb.asm.Type

class IfStatementContext extends AbstractContext {
    public @Nullable Label elifLabel
    public final Label endLabel

    IfStatementContext(@Nullable Label elifLabel, Label endLabel) {
        this.elifLabel = elifLabel
        this.endLabel = endLabel
    }
}
