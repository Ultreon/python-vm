package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.List;

public class PyStringConcatNode implements PyStringNode {
    private final PyStringNode string;
    private final List<PyStringNode> parts;

    public PyStringConcatNode(PyStringNode string, List<PyStringNode> parts) {
        this.string = string;
        this.parts = parts;
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        method.instructions.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
        method.instructions.add(new InsnNode(Opcodes.DUP));
        string.compile(method, context);
        method.instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, "python/builtins/Str"));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "python/builtins/Str", "getValue", "()Ljava/lang/String;", false));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false));
        for (PyStringNode part : parts) {
            part.compile(method, context);
            method.instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, "python/builtins/Str"));
            method.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "python/builtins/Str", "getValue", "()Ljava/lang/String;", false));
            method.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false));
        }
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/_core/Py", "createStr", "(Ljava/lang/String;)Lpython/builtins/Str;", false));
    }
}
