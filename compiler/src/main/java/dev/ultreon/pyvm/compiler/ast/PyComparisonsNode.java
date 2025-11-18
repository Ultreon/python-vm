package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class PyComparisonsNode implements PyExprNode {
    private final PyExprNode comparisons;

    public PyComparisonsNode(PyExprNode comparisons) {
        this.comparisons = comparisons;
    }

    @Override
    public Type getResolvedType(PyCompileContext context) {
        return Type.BOOLEAN_TYPE;
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        this.comparisons.compile(method, context);
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "createBool", "(Z)Lpython/builtins/Bool;", false));
    }
}
