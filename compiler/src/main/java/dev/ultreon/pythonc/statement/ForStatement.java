package dev.ultreon.pythonc.statement;

import dev.ultreon.pythonc.JvmWriter;
import dev.ultreon.pythonc.Location;
import dev.ultreon.pythonc.PythonCompiler;
import dev.ultreon.pythonc.WhileLoopContext;
import dev.ultreon.pythonc.expr.PyExpression;
import dev.ultreon.pythonc.expr.VariableExpr;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class ForStatement extends PyStatement {
    private final PyExpression iterable;
    private final VariableExpr variable;
    private final PyBlock content;
    private final @Nullable PyBlock elseBlock;

    public ForStatement(PyExpression iterable, VariableExpr variable, PyBlock content) {
        this(iterable, variable, content, null);
    }

    public ForStatement(PyExpression iterable, VariableExpr variable, PyBlock content, @Nullable PyBlock elseBlock) {
        this.iterable = iterable;
        this.variable = variable;
        this.content = content;
        this.elseBlock = elseBlock;
    }

    public PyExpression iterable() {
        return iterable;
    }

    public VariableExpr variable() {
        return variable;
    }

    public PyBlock content() {
        return content;
    }

    public PyBlock elseBlock() {
        return elseBlock;
    }

    @Override
    public void writeStatement(PythonCompiler compiler, JvmWriter writer) {
//        Label startLabel = new Label();
//        Label endLabel = new Label();
//
//        compiler.pushContext(new WhileLoopContext(startLabel, endLabel));
//
//        iterable.write(compiler, writer);
//
//        // Secretly have an iterator in the code (don't leak pls)
//        writer.secretIter();
//
//        writer.label(startLabel);
//
//        // Magically add a boolean from some iterator!
//        writer.secretDup();
//        writer.magicHasNext();
//        writer.getContext().push(Type.BOOLEAN_TYPE);
//
//        writer.pushTrue();
//
//        writer.jumpIfNotEqual(endLabel);
//
//        // Magically add an object from some iterator!
//        writer.secretDup();
//        writer.magicNext();
//        variable.writeSet(compiler, writer);
//
//        if (elseBlock == null) {
//            content.write(compiler, writer);
//        } else {
//            Label elseLabel = new Label();
//            writer.jumpIfNotEqual(elseLabel);
//            content.write(compiler, writer);
//            writer.jump(endLabel);
//            writer.label(elseLabel);
//            elseBlock.write(compiler, writer);
//            writer.jump(endLabel);
//        }
//
//        writer.jump(startLabel);
//        writer.label(endLabel);
//
//        writer.secretPop();
    }

    @Override
    public Location location() {
        return null;
    }
}
