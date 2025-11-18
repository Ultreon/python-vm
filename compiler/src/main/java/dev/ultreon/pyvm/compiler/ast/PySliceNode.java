package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class PySliceNode implements PyExprNode {
    private final PyExprNode first;
    @Nullable
    private final PyExprNode second;
    @Nullable
    private final PyExprNode third;

    public PySliceNode(PyExprNode first, @Nullable PyExprNode second, @Nullable PyExprNode third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    @Override
    public Type getResolvedType(PyCompileContext context) {
        return Type.getType(Object.class);
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        this.first.compile(method, context);
        if(this.second != null) {
            this.second.compile(method, context);
        }
        if(this.third != null) {
            this.third.compile(method, context);
        }
        if (second == null && third == null) {
            method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/builtins/Slice", "slice", "(Ljava/lang/Object;)Lpython/builtins/Slice;", false));
        } else if (second != null && third != null) {
            method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/builtins/Slice", "slice", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Lpython/builtins/Slice;", false));
        } else if (second != null) {
            method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "python/builtins/Slice", "slice", "(Ljava/lang/Object;Ljava/lang/Object;)Lpython/builtins/Slice;", false));
        } else {
            throw new IllegalStateException("Invalid slice expression");
        }
    }
}
