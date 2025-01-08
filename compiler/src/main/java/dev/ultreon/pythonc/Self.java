package dev.ultreon.pythonc;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.Objects;

final class Self implements PyExpr {
    private final int lineNo;
    private Type type;

    Self(int lineNo, Type type) {
        this.lineNo = lineNo;
        this.type = type;
    }

    @Override
    public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        return null;
    }

    @Override
    public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
        // Use "this"
//            mv.visitVarInsn(ALOAD, 0);
        throw new RuntimeException("No supported matching Self found for:\n" + this.lineNo);
    }

    @Override
    public int lineNo() {
        return lineNo;
    }

    @Override
    public Type type(PythonCompiler compiler) {
        return type;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Self) obj;
        return this.lineNo == that.lineNo;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lineNo);
    }

    @Override
    public String toString() {
        return "Self[" +
               "lineNo=" + lineNo + ']';
    }

}
