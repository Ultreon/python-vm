package dev.ultreon.pyvm.compiler.ast;

import dev.ultreon.pyvm.compiler.context.PyCompileContext;
import dev.ultreon.pyvm.compiler.util.Variable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class PyVarAccessNode implements PyExprNode, PyTargetNode {
    private final Variable var;
    private final Type type;

    public PyVarAccessNode(Variable var, Type type) {
        this.var = var;
        this.type = type;
    }

    public Variable getVar() {
        return this.var;
    }

    @Override
    public void compile(MethodNode method, PyCompileContext context) {
        context.loadVar(var);
        method.instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, this.type.getInternalName()));
    }

    @Override
    public Type getResolvedType(PyCompileContext context) {
        return this.type;
    }

    @Override
    public void compileSet(MethodNode method, PyCompileContext context, PyExprNode value) {
        value.compile(method, context);
        method.instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, this.type.getInternalName()));

        switch (this.type.getSort()) {
            case Type.OBJECT:
            case Type.ARRAY:
                method.instructions.add(new VarInsnNode(Opcodes.ASTORE, this.var.getSlot()));
                break;
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                method.instructions.add(new VarInsnNode(Opcodes.ISTORE, this.var.getSlot()));
                break;
            case Type.FLOAT:
                method.instructions.add(new VarInsnNode(Opcodes.FSTORE, this.var.getSlot()));
                break;
            case Type.LONG:
                method.instructions.add(new VarInsnNode(Opcodes.LSTORE, this.var.getSlot()));
                break;
            case Type.DOUBLE:
                method.instructions.add(new VarInsnNode(Opcodes.DSTORE, this.var.getSlot()));
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + this.type.getSort());
        }
    }
}
