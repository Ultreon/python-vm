package dev.ultreon.pythonc;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class PyVariable implements Symbol {
    private final String name;
    private Type type;
    private final int index;
    private final int lineNo;
    private final boolean typeConstant;
    private final Label label;

    PyVariable(String name, Type type, int index, int lineNo, boolean typeConstant, Label label) {
        this.name = name;
        this.type = type;
        this.index = index;
        this.lineNo = lineNo;
        this.typeConstant = typeConstant;
        this.label = label;
    }

    @Override
    public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        return null;
    }

    @Override
    public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
        int opcode;
        if (type.getSort() == Type.OBJECT) {
            if (type.equals(Type.getType(String.class)) || type.equals(Type.BYTE_TYPE) || type.equals(Type.CHAR_TYPE) || type.equals(Type.SHORT_TYPE) || type.equals(Type.INT_TYPE) || type.equals(Type.LONG_TYPE) || type.equals(Type.FLOAT_TYPE) || type.equals(Type.DOUBLE_TYPE) || type.equals(Type.BOOLEAN_TYPE)
            || type.equals(Type.getType(byte[].class)) || type.equals(Type.getType(Object[].class)) || type.equals(Type.getType(Object.class)) || type.equals(Type.getType(Class.class))
            || type.equals(Type.getType(Byte.class)) || type.equals(Type.getType(Character.class)) || type.equals(Type.getType(Short.class)) || type.equals(Type.getType(Integer.class)) || type.equals(Type.getType(Long.class)) || type.equals(Type.getType(Float.class)) || type.equals(Type.getType(Double.class)) || type.equals(Type.getType(Boolean.class))) {

            } else if (compiler.imports.get(compiler.writer.boxType(type).getClassName().substring(compiler.writer.boxType(type).getClassName().lastIndexOf('.') + 1)) == null) {
                throw compiler.typeNotFound(compiler.writer.boxType(type).getClassName().substring(compiler.writer.boxType(type).getClassName().lastIndexOf('.') + 1), this);
            }
        }

        compiler.writer.loadObject(index, compiler.writer.boxType(type));

        if (!boxed) {
            compiler.writer.unbox(type);
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
        if (type.getSort() == Type.OBJECT) {
            visit.load(mv, compiler, visit.preload(mv, compiler, true), true);
            Type newType = visit.type(compiler);
            compiler.writer.getContext().pop();
            if (!type.equals(newType)) {
                if (typeConstant) {
                    throw new RuntimeException("Cannot assign " + newType + " to " + type);
                }
                type = newType;
            }
            mv.visitVarInsn(Opcodes.ASTORE, index);
        } else if (type.getSort() == Type.ARRAY) {
            visit.load(mv, compiler, visit.preload(mv, compiler, true), true);
            Type newType = visit.type(compiler);
            compiler.writer.getContext().pop();
            if (!type.equals(newType)) {
                if (typeConstant) {
                    throw new RuntimeException("Cannot assign " + newType + " to " + type);
                }
                type = newType;
            }
            mv.visitVarInsn(Opcodes.ASTORE, index);
        } else {
            visit.load(mv, compiler, visit.preload(mv, compiler, false), false);
            Type newType = visit.type(compiler);
            if (!type.equals(newType)) {
                if (typeConstant) {
                    throw new RuntimeException("Cannot assign " + newType + " to " + type);
                }
                type = newType;
            }
            compiler.writer.box(type);
            compiler.writer.getContext().pop();
            mv.visitVarInsn(Opcodes.ASTORE, index);
        }
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
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
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
