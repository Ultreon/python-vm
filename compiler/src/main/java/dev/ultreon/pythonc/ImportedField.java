package dev.ultreon.pythonc;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

record ImportedField(String name, Type type, String owner, Location location) implements JvmClassMember {
    @Override
    public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        return null;
    }

    @Override
    public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
        compiler.writer.loadClass(ownerClass(compiler));
        compiler.writer.dynamicGetAttr(name);
    }

    @Override
    public Type type(PythonCompiler compiler) {
        return type;
    }

    @Override
    public void expectReturnType(PythonCompiler compiler, JvmClass returnType, Location location) {
        if (!returnType.doesInherit(compiler, PythonCompiler.classCache.require(compiler, type))) {
            throw new CompilerException("Expected " + returnType + " but got " + type + " at ", location);
        }
    }

    @Override
    public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit) {
        compiler.writer.loadClass(ownerClass(compiler));
        if (type.getSort() == Type.OBJECT) {
            visit.load(mv, compiler, visit.preload(mv, compiler, true), true);
            compiler.writer.dynamicSetAttr(name);
        } else {
            visit.load(mv, compiler, visit.preload(mv, compiler, false), false);
            compiler.writer.dynamicSetAttr(name);
        }
    }

    @Override
    public JvmClass ownerClass(PythonCompiler compiler) {
        boolean load = PythonCompiler.classCache.load(compiler, Type.getObjectType(owner));
        if (!load) {
            throw new RuntimeException("Inherited class from " + owner + " not found: " + owner);
        }
        return PythonCompiler.classCache.get(Type.getObjectType(owner));
    }

    public JvmClass typeClass(PythonCompiler compiler) {
        boolean load = PythonCompiler.classCache.load(compiler, type);
        if (!load) {
            throw new RuntimeException("Inherited class from " + type + " not found: " + type);
        }
        return PythonCompiler.classCache.get(type);
    }
}
