package dev.ultreon.pythonc;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.GETSTATIC;

record ImportedField(String name, Type type, String owner, int lineNo) implements Symbol {
    @Override
    public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        return null;
    }

    @Override
    public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
        mv.visitFieldInsn(GETSTATIC, owner, name, type.getDescriptor());
    }

    @Override
    public Type type(PythonCompiler compiler) {
        return type;
    }

    @Override
    public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit) {

    }
}
