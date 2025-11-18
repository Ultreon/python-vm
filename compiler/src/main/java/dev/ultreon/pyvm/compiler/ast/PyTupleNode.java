package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public class PyTupleNode implements PyExprNode {
    private final PyExprNode expr;
    private final PyExprNode[] exprNodes;

    public PyTupleNode(PyExprNode expr, PyExprNode[] exprNodes) {
        this.expr = expr;
        this.exprNodes = exprNodes;
    }

    @Override
    public Type getResolvedType(PyCompileContext context) {
        return Type.getType(Object.class);
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        method.instructions.add(new IntInsnNode(Opcodes.BIPUSH, this.exprNodes.length));
        method.instructions.add(new TypeInsnNode(Opcodes.ANEWARRAY, "python/types/PyObject"));
        method.instructions.add(new InsnNode(Opcodes.DUP));
        this.expr.compile(method, context);
        for (PyExprNode exprNode : this.exprNodes) {
            method.instructions.add(new InsnNode(Opcodes.DUP));
            exprNode.compile(method, context);
        }
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "createTuple", "([Lpython/types/PyObject;)Lpython/types/PyObject;", false));
    }
}
