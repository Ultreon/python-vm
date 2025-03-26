package dev.ultreon.pythonc;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class MemberField implements PyExpr {
    private final PyExpr parent;
    private final Location location;
    private final String name;

    public MemberField(PyExpr parent, String name, Location location) {
        this.parent = parent;
        this.location = location;
        this.name = name;
    }

    @Override
    public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        return null;
    }

    @Override
    public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
        parent.load(mv, compiler, parent.preload(mv, compiler, boxed), boxed);
        compiler.writer.dynamicGetAttr(name);
    }

    public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr expr) {
        compiler.writer.putField(parent.type(compiler).getInternalName(), name, "Ljava/lang/Object;", parent, expr);
    }

    @Override
    public void expectReturnType(PythonCompiler compiler, JvmClass returnType, Location location) {
        // Ignored
    }

    @Override
    public Type type(PythonCompiler compiler) {
        return Type.getType(Object.class);
    }

    @Override
    public Location location() {
        return location;
    }
}
