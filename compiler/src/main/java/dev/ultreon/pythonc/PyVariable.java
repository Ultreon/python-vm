package dev.ultreon.pythonc;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Objects;

public final class PyVariable implements Symbol {
    private final String name;
    private final Type type;
    private final int index;
    private final Label label;
    private final Location location;

    PyVariable(String name, int index, Label label, Location location) {
        this(name, Type.getType(Object.class), index, label, location);
    }

    PyVariable(String name, Type type, int index, Label label, Location location) {
        if (type == null) {
            type = Type.getType(Object.class);
        }
        this.name = name;
        this.type = type;
        this.index = index;
        this.label = label;
        this.location = location;
    }

    @Override
    public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        return null;
    }

    @Override
    public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
        switch (type.getSort()) {
            case Type.ARRAY, Type.OBJECT -> compiler.writer.loadObject(index, type);
            case Type.BOOLEAN -> {
                compiler.writer.loadBoolean(index);
                compiler.writer.box(type);
            }
            case Type.CHAR -> {
                compiler.writer.loadChar(index);
                compiler.writer.box(type);
            }
            case Type.DOUBLE -> {
                compiler.writer.loadDouble(index);
                compiler.writer.box(type);
            }
            case Type.FLOAT -> {
                compiler.writer.loadFloat(index);
                compiler.writer.box(type);
            }
            case Type.INT -> {
                compiler.writer.loadInt(index);
                compiler.writer.box(type);
            }
            case Type.LONG -> {
                compiler.writer.loadLong(index);
                compiler.writer.box(type);
            }
            case Type.SHORT -> {
                compiler.writer.loadShort(index);
                compiler.writer.box(type);
            }
            case Type.BYTE -> {
                compiler.writer.loadByte(index);
                compiler.writer.box(type);
            }
            default -> {
                throw new RuntimeException("Unsupported type: " + type);
            }
        }
    }

    @Override
    public Type type(PythonCompiler compiler) {
        return Type.getType(Object.class);
    }

    @Override
    public Location location() {
        return location;
    }

    @Override
    public void expectReturnType(PythonCompiler compiler, JvmClass returnType, Location location) {
        // Nothing
    }

    @Override
    public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit) {
        Type newType = visit.type(compiler);
        visit.load(mv, compiler, visit.preload(mv, compiler, true), true);
        compiler.writer.box(newType);
        compiler.writer.storeObject(index, newType);
    }

    @Override
    public String name() {
        return name;
    }

    public Type type() {
        return Type.getType(Object.class);
    }

    public int index() {
        return index;
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
                this.index == that.index &&
                Objects.equals(this.label, that.label) &&
                Objects.equals(this.location, that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, index, label, location);
    }

    @Override
    public String toString() {
        return "PyVariable[" +
                "typedName=" + name + ", " +
                "index=" + index + ", " +
                "label=" + label + ", " +
                "location=" + location + ']';
    }

    public JvmClass cls(PythonCompiler compiler) {
        if (!PythonCompiler.classCache.load(compiler, Type.getType(Object.class))) {
            throw new RuntimeException("Class " + Type.getType(Object.class).getClassName() + " not found");
        }
        return PythonCompiler.classCache.get(Type.getType(Object.class));
    }
}
