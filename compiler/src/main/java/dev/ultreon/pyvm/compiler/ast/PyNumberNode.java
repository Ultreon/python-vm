package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class PyNumberNode implements PyExprNode {
    private final String text;

    public PyNumberNode(String text) {
        this.text = text;
    }

    @Override
    public Type getResolvedType(PyCompileContext context) {
        return Type.getType(Object.class);
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        if (this.text == null) {
            throw new NullPointerException("Text cannot be null!");
        }
        method.instructions.add(new LdcInsnNode(this.text));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "createNumber", "(Ljava/lang/String;)Lpython/_core/VMObject;", false));
    }
}
