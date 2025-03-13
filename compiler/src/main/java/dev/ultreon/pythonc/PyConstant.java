package dev.ultreon.pythonc;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.Objects;

final class PyConstant implements PyExpr {
    private final Object value;
    private final int lineNo;
    public final Type type;

    PyConstant(Object value, int lineNo) {
        this.value = value;
        this.lineNo = lineNo;
        type = switch (this.value) {
            case String s -> Type.STRING;
            case Integer i -> Type.INTEGER;
            case Float f -> Type.FLOAT;
            case Double d -> Type.DOUBLE;
            case Boolean b -> Type.BOOLEAN;
            case Byte b -> Type.BYTE;
            case Short s -> Type.SHORT;
            case Long l -> Type.LONG;
            case Character c -> Type.CHARACTER;
            default -> throw new RuntimeException("No supported matching type found for:\n" + this.value);
        };
    }

    enum Type {
        STRING(org.objectweb.asm.Type.getType(String.class)),
        INTEGER(org.objectweb.asm.Type.INT_TYPE),
        FLOAT(org.objectweb.asm.Type.FLOAT_TYPE),
        DOUBLE(org.objectweb.asm.Type.DOUBLE_TYPE),
        BOOLEAN(org.objectweb.asm.Type.BOOLEAN_TYPE),
        BYTE(org.objectweb.asm.Type.BYTE_TYPE),
        SHORT(org.objectweb.asm.Type.SHORT_TYPE),
        LONG(org.objectweb.asm.Type.LONG_TYPE),
        CHARACTER(org.objectweb.asm.Type.CHAR_TYPE);

        public final org.objectweb.asm.Type type;

        Type(org.objectweb.asm.Type type) {
            this.type = type;
        }
    }

    @Override
    public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        return null;
    }

    @Override
    public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
        mv.visitLdcInsn(value);
        Context context = compiler.getContext(Context.class);
        context.push(type.type);
    }

    public Object value() {
        return value;
    }

    @Override
    public int lineNo() {
        return lineNo;
    }

    @Override
    public org.objectweb.asm.Type type(PythonCompiler compiler) {
        return type.type;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (PyConstant) obj;
        return Objects.equals(this.value, that.value) && this.lineNo == that.lineNo;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, lineNo);
    }

    @Override
    public String toString() {
        return "PyConstant[" + "value=" + value + ", " + "lineNo=" + lineNo + ']';
    }
}
