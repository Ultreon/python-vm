package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.scope.ForScope;
import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import dev.ultreon.pyvm.compiler.util.Variable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public class PyForStmtNode implements PyCompoundStatementNode {
    private final PyTargetNode target;
    private final PyExprNode expr;
    private final PyBlockNode body;
    private final PyElseBlockNode elseBlock;

    public PyForStmtNode(PyTargetNode target, PyExprNode expr, PyBlockNode body, PyElseBlockNode elseBlock) {
        this.target = target;
        this.expr = expr;
        this.body = body;
        this.elseBlock = elseBlock;
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        context.pushScope(new ForScope(context.getScope()));
        LabelNode exitLabel = context.getScope(ForScope.class).getExitLabel();
        Variable iterVar = context.createVar("$forStmt_Iter_" + context.getCounter());
        Variable valueVar = context.createVar("$forStmt_Val_" + context.getCounter());
        this.expr.compile(method, context);
        context.getAttribute("__iter__");
        context.call();
        context.storeVar(iterVar);

        method.instructions.add(context.getScope(ForScope.class).getContinueLabel());
        this.target.compileSet(method, context, new ForSourceNode(valueVar, exitLabel, iterVar));
        this.body.compile(method, context);
        method.instructions.add(new JumpInsnNode(Opcodes.GOTO, context.getScope(ForScope.class).getContinueLabel()));

        if (this.elseBlock != null) {
            this.elseBlock.compile(method, context);
        }

        method.instructions.add(exitLabel);
        context.popScope();
    }

    private class ForSourceNode implements PyExprNode {
        private final Variable var;
        private final LabelNode exitLabel;
        private final Variable iterVar;

        public ForSourceNode(Variable var, LabelNode exitLabel, Variable iterVar) {
            this.var = var;
            this.exitLabel = exitLabel;
            this.iterVar = iterVar;
        }

        @Override
        public Type getResolvedType(PyCompileContext context) {
            return Type.getType(Object.class);
        }

        @Override
        public void compile(MethodNode method, PyCompileContext context) {
            LabelNode startLabel = new LabelNode();
            LabelNode catchLabel = new LabelNode();
            LabelNode tryExitLabel = new LabelNode();
            method.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
            method.instructions.add(new VarInsnNode(Opcodes.ASTORE, var.getSlot()));
            method.tryCatchBlocks.add(new TryCatchBlockNode(
                    startLabel, catchLabel, catchLabel, "python/builtins/StopIteration"
            ));
            method.instructions.add(startLabel);
            context.loadVar(iterVar);
            context.getAttribute("__next__");
            context.call();
            context.storeVar(var);
            method.instructions.add(new JumpInsnNode(Opcodes.GOTO, tryExitLabel));
            method.instructions.add(catchLabel);
            method.instructions.add(new InsnNode(Opcodes.POP));
            target.compileSet(method, context, new PyExprNode() {
                @Override
                public void compile(MethodNode method, PyCompileContext context) {
                    context.loadVar(var);
                }

                @Override
                public Type getResolvedType(PyCompileContext context) {
                    return Type.getType(Object.class);
                }
            });
            method.instructions.add(new JumpInsnNode(Opcodes.GOTO, exitLabel));
            method.instructions.add(tryExitLabel);
            context.loadVar(var);
        }
    }
}
