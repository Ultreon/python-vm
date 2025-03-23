package dev.ultreon.pythonc;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class MemberFuncCall implements PyExpr {
    private final PyExpr parent;
    private final JvmFunction memberFunc;

    public MemberFuncCall(PyExpr parent, JvmFunction memberFunc) {
        this.parent = parent;
        this.memberFunc = memberFunc;
        if (memberFunc instanceof JvmConstructor) {
            throw new AssertionError("Invalid function, this shouldn't be a constructor");
        }
    }

    @Override
    public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        return null;
    }

    @Override
    public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
        parent.load(mv, compiler, parent.preload(mv, compiler, boxed), boxed);
        if (memberFunc != null) {
            memberFunc.write(mv, compiler);
        }
    }

    @Override
    public int lineNo() {
        return 0;
    }

    @Override
    public Type type(PythonCompiler compiler) {
        return memberFunc.type(compiler);
    }

    public Type owner(PythonCompiler compiler) {
        return memberFunc.owner(compiler).type(compiler);
    }

    public JvmClass ownerClass(PythonCompiler compiler) {
        Type owner = owner(compiler);
        if (!PythonCompiler.classCache.load(compiler, owner)) {
            throw new CompilerException("Class '" + owner.getClassName() + "' not found (" + compiler.getLocation(this) + ")");
        }
        return PythonCompiler.classCache.get(owner);
    }
}
