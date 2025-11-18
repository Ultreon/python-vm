package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.List;

public class PyFStringNode implements PyStringNode {
    private final List<PyFStringMiddleNode> nodes;

    public PyFStringNode(List<PyFStringMiddleNode> nodes) {
        this.nodes = nodes;
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        method.instructions.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
        method.instructions.add(new InsnNode(Opcodes.DUP));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false));
        for (PyFStringMiddleNode node : this.nodes) {
            node.compile(method, context);
        }
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "createStr", "(Ljava/lang/String;)Lpython/builtins/Str;", false));
    }
}
