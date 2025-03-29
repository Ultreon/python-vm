package dev.ultreon.pythonc.statement;

import dev.ultreon.pythonc.*;
import dev.ultreon.pythonc.expr.PyExpression;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

public class WhileStatement extends PyStatement {
    private final PyExpression condition;
    private final PyBlock content;
    private final @Nullable PyBlock elseBlock;

    public WhileStatement(PyExpression condition, PyBlock content) {
        this(condition, content, null);
    }

    public WhileStatement(PyExpression condition, PyBlock content, @Nullable PyBlock elseBlock) {
        this.condition = condition;
        this.content = content;
        this.elseBlock = elseBlock;
    }

    public PyExpression condition() {
        return condition;
    }

    public PyBlock content() {
        return content;
    }

    public PyBlock elseBlock() {
        return elseBlock;
    }

    @Override
    public void writeStatement(PythonCompiler compiler, JvmWriter writer) {
        Label startLabel = new Label();
        Label endLabel = new Label();

        compiler.pushContext(new WhileLoopContext(startLabel, endLabel));

        writer.label(startLabel);

        Type conditionType = condition.write(compiler, writer);
        if (!conditionType.equals(Type.BOOLEAN_TYPE)) {
            writer.cast(Type.BOOLEAN_TYPE);
        }

        writer.pushTrue();

        if (elseBlock == null) {
            writer.jumpIfNotEqual(endLabel);
            content.write(compiler, writer);
            compiler.checkPop(location());
        } else {
            Label elseLabel = new Label();
            writer.jumpIfNotEqual(elseLabel);
            content.write(compiler, writer);
            compiler.checkPop(location());
            writer.jump(endLabel);
            writer.label(elseLabel);
            elseBlock.write(compiler, writer);
            compiler.checkPop(location());
            writer.jump(endLabel);
        }

        writer.jump(startLabel);
        writer.label(endLabel);
    }

    @Override
    public Location location() {
        return null;
    }
}
