package dev.ultreon.pythonc.expr;

import dev.ultreon.pythonc.*;
import dev.ultreon.pythonc.lang.PyAST;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public abstract class PyExpression implements PyAST {
    private final Location location;

    public PyExpression(Location location) {
        this.location = location;
    }

    @Deprecated(forRemoval = true)
    public final Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        this.prepare(compiler, compiler.writer);
        return null;
    }

    @Deprecated(forRemoval = true)
    public final void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
        // do nothing
        this.write(compiler, compiler.writer);
    }

    public void prepare(PythonCompiler compiler, JvmWriter writer) {

    }

    public Type type() {
        return Type.getType(Object.class);
    }

    public abstract void writeCode(PythonCompiler compiler, JvmWriter writer);

    public final Location location() {
        return location;
    }

    protected void writeFull(PythonCompiler compiler, JvmWriter writer) {
        this.prepare(compiler, writer);
        this.write(compiler, writer);
    }
}
