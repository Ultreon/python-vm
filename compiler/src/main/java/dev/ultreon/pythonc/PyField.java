package dev.ultreon.pythonc;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.GETFIELD;

record PyField(Type owner, String name, Type type, int lineNo) implements JvmField, Symbol, Ownable {
    public String toString() {
        return name + ": '" + type + "'";
    }

    @Override
    public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        return null;
    }

    @Override
    public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
        mv.visitFieldInsn(GETFIELD, owner(compiler).getInternalName(), name, type.getDescriptor());
    }

    @Override
    public int lineNo() {
        return 0;
    }

    @Override
    public String name() {
        return "";
    }

    @Override
    public Type type(PythonCompiler compiler) {
        return type;
    }

    @Override
    public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit) {

    }

    @Override
    public Type owner(PythonCompiler compiler) {
        return owner;
    }
}
