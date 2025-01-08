package dev.ultreon.pythonc;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.objectweb.asm.Opcodes.*;

final class PyVariable implements Symbol {
    private final String name;
    private final Type type;
    private final int index;
    private final int lineNo;
    private final Label label;

    PyVariable(String name, Type type, int index, int lineNo, Label label) {
        this.name = name;
        this.type = type;
        this.index = index;
        this.lineNo = lineNo;
        this.label = label;
    }

    @Override
    public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        return null;
    }

    @Override
    public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
        int opcode;
        if (type.equals(Type.getType(String.class))) {
            opcode = ALOAD;
        } else if (type.equals(Type.LONG_TYPE)) {
            opcode = LLOAD;
        } else if (type.equals(Type.DOUBLE_TYPE)) {
            opcode = DLOAD;
        } else if (type.equals(Type.FLOAT_TYPE)) {
            opcode = FLOAD;
        } else if (type.equals(Type.INT_TYPE)) {
            opcode = ILOAD;
        } else if (type.equals(Type.BOOLEAN_TYPE)) {
            opcode = ILOAD;
        } else if (type.equals(Type.CHAR_TYPE)) {
            opcode = ILOAD;
        } else if (type.equals(Type.BYTE_TYPE)) {
            opcode = ILOAD;
        } else if (type.equals(Type.SHORT_TYPE)) {
            opcode = ILOAD;
        } else {
            if (compiler.imports.get(type.getClassName().substring(type.getClassName().lastIndexOf('.') + 1)) == null) {
                throw compiler.typeNotFound(type.getClassName().substring(type.getClassName().lastIndexOf('.') + 1), this);
            }

            opcode = ALOAD;
        }
        mv.visitVarInsn(opcode, index);

        if (boxed) {
            if (type.equals(Type.LONG_TYPE)) {
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
            } else if (type.equals(Type.DOUBLE_TYPE)) {
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
            } else if (type.equals(Type.INT_TYPE)) {
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
            } else if (type.equals(Type.FLOAT_TYPE)) {
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
            } else if (type.equals(Type.BOOLEAN_TYPE)) {
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
            } else if (type.equals(Type.BYTE_TYPE)) {
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
            } else if (type.equals(Type.SHORT_TYPE)) {
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
            } else if (type.equals(Type.CHAR_TYPE)) {
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
            }
        }
    }

    @Override
    public Type type(PythonCompiler compiler) {
        if (type.equals(Type.LONG_TYPE)) {
            return Type.LONG_TYPE;
        } else if (type.equals(Type.DOUBLE_TYPE)) {
            return Type.DOUBLE_TYPE;
        } else if (type.equals(Type.INT_TYPE)) {
            return Type.INT_TYPE;
        } else if (type.equals(Type.FLOAT_TYPE)) {
            return Type.FLOAT_TYPE;
        } else if (type.equals(Type.BOOLEAN_TYPE)) {
            return Type.BOOLEAN_TYPE;
        } else if (type.equals(Type.CHAR_TYPE)) {
            return Type.CHAR_TYPE;
        } else if (type.equals(Type.BYTE_TYPE)) {
            return Type.BYTE_TYPE;
        } else if (type.equals(Type.SHORT_TYPE)) {
            return Type.SHORT_TYPE;
        } else if (type.equals(Type.getType(String.class))) {
            return Type.getType(String.class);
        } else if (type.equals(Type.getType(byte[].class))) {
            return Type.getType(byte[].class);
        } else if (type.equals(Type.getType(List.class))) {
            return Type.getType(List.class);
        } else if (type.equals(Type.getType(Map.class))) {
            return Type.getType(Map.class);
        } else if (type.equals(Type.getType(Set.class))) {
            return Type.getType(Set.class);
        } else if (type.equals(Type.getType(Object[].class))) {
            return Type.getType(Object[].class);
        } else if (type.equals(Type.getType(Object.class))) {
            return Type.getType(Object.class);
        } else if (type.equals(Type.getType(Class.class))) {
            return Type.getType(Class.class);
        }
        if (compiler.symbols.get(type.getClassName().substring(type.getClassName().lastIndexOf('.') + 1)) == null) {
            throw compiler.typeNotFound(type.getClassName().substring(type.getClassName().lastIndexOf('.') + 1), this);
        }

        return compiler.symbols.get(type.getClassName().substring(type.getClassName().lastIndexOf('.') + 1)).type(compiler);
    }

    @Override
    public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit) {
        mv.visitVarInsn(ISTORE, index);
    }

    @Override
    public String name() {
        return name;
    }

    public Type type() {
        return type;
    }

    public int index() {
        return index;
    }

    @Override
    public int lineNo() {
        return lineNo;
    }

    public Label label() {
        return label;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (PyVariable) obj;
        return Objects.equals(this.name, that.name) &&
               Objects.equals(this.type, that.type) &&
               this.index == that.index &&
               this.lineNo == that.lineNo &&
               Objects.equals(this.label, that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, index, lineNo, label);
    }

    @Override
    public String toString() {
        return "PyVariable[" +
               "name=" + name + ", " +
               "type=" + type + ", " +
               "index=" + index + ", " +
               "lineNo=" + lineNo + ", " +
               "label=" + label + ']';
    }

}
