package dev.ultreon.pythonc;

import dev.ultreon.pythonc.statement.PyStatement;
import org.objectweb.asm.Label;

public class JumpStatement extends PyStatement {
    private final Label toLabel;

    public JumpStatement(Label toLabel) {
        super();
        this.toLabel = toLabel;
    }

    public Label getToLabel() {
        return toLabel;
    }

    @Override
    public void writeStatement(PythonCompiler compiler, JvmWriter writer) {
        writer.jump(toLabel);
    }

    @Override
    public Location location() {
        return null;
    }
}
