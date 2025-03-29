package dev.ultreon.pythonc.statement;

import dev.ultreon.pythonc.JvmWriter;
import dev.ultreon.pythonc.Location;
import dev.ultreon.pythonc.PythonCompiler;
import dev.ultreon.pythonc.expr.PyExpression;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

public class IfStatement extends PyStatement {
    private final PyExpression condition;
    private final PyBlock trueBlock;
    private final @Nullable PyBlock falseBlock;

    public IfStatement(PyExpression condition, PyBlock trueBlock, @Nullable PyBlock falseBlock) {
        this.condition = condition;
        this.trueBlock = trueBlock;
        this.falseBlock = falseBlock;
    }

    public PyExpression condition() {
        return condition;
    }

    public PyBlock trueBlock() {
        return trueBlock;
    }

    public PyBlock falseBlock() {
        return falseBlock;
    }

    @Override
    public void writeStatement(PythonCompiler compiler, JvmWriter writer) {
//        Label endLabel = new Label();
//
//        Type conditionType = condition.write(compiler, writer);
//        if (!conditionType.equals(Type.BOOLEAN_TYPE)) {
//            writer.cast(Type.BOOLEAN_TYPE);
//        }
//
//        writer.pushTrue();
//
//        if (falseBlock == null) {
//            writer.jumpIfNotEqual(endLabel);
//            compiler.checkPop(condition.location());
//            trueBlock.write(compiler, writer);
//        } else {
//            Label elseLabel = new Label();
//            writer.jumpIfNotEqual(elseLabel);
//            compiler.checkPop(condition.location());
//            trueBlock.write(compiler, writer);
//            writer.jump(endLabel);
//            writer.label(elseLabel);
//            falseBlock.write(compiler, writer);
//        }
//
//        writer.label(endLabel);
    }

    @Override
    public Location location() {
        return null;
    }
}
