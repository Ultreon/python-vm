package dev.ultreon.pythonc;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public record PyField(Type owner, String name, Type type, Location location) implements JvmField, Symbol, JvmOwnable {
    public String toString() {
        return name + ": '" + type + "'";
    }

    @Override
    public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        return null;
    }

    @Override
    public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
        compiler.writer.getField(name, type.getDescriptor());
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Type type(PythonCompiler compiler) {
        return type;
    }

    @Override
    public void expectReturnType(PythonCompiler compiler, JvmClass returnType, Location location) {
        if (!PythonCompiler.classCache.require(compiler, owner).doesInherit(compiler, returnType)) {
            throw new RuntimeException("Invalid field type: " + returnType + " for " + compiler.getLocation(this));
        }
    }

    public JvmClass ownerClass(PythonCompiler compiler) {
        Symbol symbol = PythonCompiler.classCache.require(compiler, owner);
        if (symbol instanceof JvmClass jvmClass) {
            return jvmClass;
        }
        throw new CompilerException("Invalid owner: " + owner , compiler.getLocation(this));
    }

    public JvmClass typeClass(PythonCompiler compiler) {
        Symbol symbol = compiler.symbols.get(type.getClassName());
        if (symbol instanceof JvmClass jvmClass) {
            return jvmClass;
        }
        throw new IllegalStateException("Invalid owner: " + type);
    }

    @Override
    public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr expr) {
        expr.load(mv, compiler, expr.preload(mv, compiler, true), true);
        compiler.writer.dynamicSetAttr(name);
    }

    @Override
    public Type owner(PythonCompiler compiler) {
        return owner;
    }

    @Override
    public JvmClass cls(PythonCompiler pythonCompiler) {
        return ownerClass(pythonCompiler);
    }
}
