package dev.ultreon.pythonc;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.Objects;

import static org.objectweb.asm.Opcodes.ALOAD;

final class Self implements PyExpr {
    private final Type type;
    private final Location location;

    Self(Type type, Location location) {
        this.type = type;
        this.location = location;
    }

    @Override
    public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        return null;
    }

    @Override
    public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
        // Use "this"
        compiler.writer.loadThis(compiler, PythonCompiler.classCache.require(compiler, type));
    }

    @Override
    public void expectReturnType(PythonCompiler compiler, JvmClass returnType, Location location) {
        typeClass(compiler).expectReturnType(compiler, returnType, location);
    }

    @Override
    public Type type(PythonCompiler compiler) {
        return type;
    }

    @Override
    public Location location() {
        return location;
    }

    public PyClass typeClass(PythonCompiler compiler) {
        return compiler.definingInstance;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Self) obj;
        return this.location.equals(that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location);
    }

    @Override
    public String toString() {
        return "Self[" +
               "location=" + location + ']';
    }

    public JvmClass cls(PythonCompiler pythonCompiler) {
        return typeClass(pythonCompiler);
    }
}
