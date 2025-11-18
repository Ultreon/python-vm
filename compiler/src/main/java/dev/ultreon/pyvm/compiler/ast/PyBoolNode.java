package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class PyBoolNode implements PyExprNode {
    private final boolean value;

    public PyBoolNode(boolean value) {
        this.value = value;
    }

    @Override
    public Type getResolvedType(PyCompileContext context) {
        return Type.getObjectType("python/lang/Bool");
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        method.instructions.add(new LdcInsnNode(value ? 1 : 0));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "createBool", "(Z)Lpython/builtins/Bool;", false));
    }
}
