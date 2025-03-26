package dev.ultreon.pythonc;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ExpectedField implements JvmField {
    private final JvmClass owner;
    private final String name;
    private final boolean isStatic;
    private JvmClass expectedType;
    private final List<ExpectedFieldType> returnTypeExpectations = new ArrayList<>();

    public ExpectedField(JvmClass owner, String name, boolean isStatic) {
        this.owner = owner;
        this.name = name;
        this.isStatic = isStatic;
    }

    @Override
    public JvmClass cls(PythonCompiler pythonCompiler) {
        return owner;
    }

    @Override
    public JvmClass ownerClass(PythonCompiler compiler) {
        return owner;
    }

    @Override
    public JvmClass typeClass(PythonCompiler compiler) {
        return expectedType;
    }

    @Override
    public Type owner(PythonCompiler compiler) {
        return owner.type(compiler);
    }

    @Override
    public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        return null;
    }

    @Override
    public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
        if (isStatic) {
            compiler.writer.getStatic(owner.type(compiler).getInternalName(), name, expectedType.type(compiler).getDescriptor());
        } else {
            compiler.writer.getField(name, expectedType.type(compiler).getDescriptor());
        }
    }

    @Override
    public Type type(PythonCompiler compiler) {
        return null;
    }

    @Override
    public Location location() {
        return new Location("<unknown>", 0, 0, 0, 0);
    }

    @Override
    public void expectReturnType(PythonCompiler compiler, JvmClass returnType, Location location) {
        if (!expectedType.doesInherit(compiler, returnType)) {
            throw new CompilerException("Expected " + expectedType + " but got " + returnType + " at ", location);
        }
    }

    @Override
    public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit) {

    }

    public JvmClass owner() {
        return owner;
    }

    @Override
    public String name() {
        return name;
    }

    public boolean isStatic() {
        return isStatic;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ExpectedField) obj;
        return Objects.equals(this.owner, that.owner) &&
                Objects.equals(this.name, that.name) &&
                this.isStatic == that.isStatic;
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, name, isStatic);
    }

    @Override
    public String toString() {
        return "ExpectedField[" +
                "owner=" + owner + ", " +
                "name=" + name + ", " +
                "isStatic=" + isStatic + ']';
    }

}
