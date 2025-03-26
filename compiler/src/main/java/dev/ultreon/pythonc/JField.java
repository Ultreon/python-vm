package dev.ultreon.pythonc;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class JField implements JvmField {
    private final JClass jClass;
    private final Field field;
    private final String name;
    private final Type type1;

    public JField(JClass jClass, Field field, String name, Type type1) {
        this.jClass = jClass;
        this.field = field;
        this.name = name;
        this.type1 = type1;
    }

    @Override
    public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        return null;
    }

    @Override
    public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
        if (Modifier.isStatic(field.getModifiers())) {
            compiler.writer.loadClass(jClass);
            compiler.writer.dynamicGetAttr(name);
            return;
        }
        compiler.writer.dynamicGetAttr(name);
    }

    @Override
    public String name() {
        return "";
    }

    @Override
    public Type type(PythonCompiler compiler) {
        return Type.getType(field.getType());
    }

    @Override
    public Location location() {
        return new Location("<Java>", 0, 0, 0, 0);
    }

    @Override
    public void expectReturnType(PythonCompiler compiler, JvmClass returnType, Location location) {
        if (!typeClass(compiler).doesInherit(compiler, returnType)) {
            throw new CompilerException("Expected " + typeClass(compiler).type(compiler) + " but got " + returnType.type(compiler), location);
        }
    }

    @Override
    public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit) {
        visit.load(mv, compiler, visit.preload(mv, compiler, false), false);
        compiler.writer.cast(compiler.writer.boxType(type(compiler)));
        compiler.writer.smartCast(type(compiler));
        compiler.writer.dynamicSetAttr(name);
    }

    @Override
    public Type owner(PythonCompiler compiler) {
        PythonCompiler.classCache.load(compiler, field.getDeclaringClass());
        return PythonCompiler.classCache.get(field.getDeclaringClass()).type(compiler);
    }

    @Override
    public JvmClass ownerClass(PythonCompiler compiler) {
        PythonCompiler.classCache.load(compiler, field.getDeclaringClass());
        return PythonCompiler.classCache.get(field.getDeclaringClass());
    }

    @Override
    public JvmClass typeClass(PythonCompiler compiler) {
        PythonCompiler.classCache.load(compiler, type1);
        return PythonCompiler.classCache.get(type1);
    }

    @Override
    public JvmClass cls(PythonCompiler pythonCompiler) {
        return typeClass(pythonCompiler);
    }
}
