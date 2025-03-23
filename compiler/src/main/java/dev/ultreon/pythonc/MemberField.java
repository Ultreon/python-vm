package dev.ultreon.pythonc;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class MemberField implements PyExpr {
    private final JvmField parent;
    private final JvmField member;

    public MemberField(JvmField parent, JvmField member) {
        this.parent = parent;
        this.member = member;
    }

    @Override
    public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        return null;
    }

    @Override
    public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
        parent.load(mv, compiler, preloaded, boxed);
        compiler.writer.getField(member.owner(compiler).getInternalName(), member.name(), member.type(compiler).getDescriptor());
    }

    @Override
    public int lineNo() {
        return member.lineNo();
    }

    @Override
    public Type type(PythonCompiler compiler) {
        return member.type(compiler);
    }
}
