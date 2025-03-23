package dev.ultreon.pythonc;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public record PyField(Type owner, String name, Type type, int lineNo) implements JvmField, Symbol, JvmOwnable {
    public String toString() {
        return name + ": '" + type + "'";
    }

    @Override
    public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        return null;
    }

    @Override
    public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
        compiler.writer.getField(owner(compiler).getInternalName(), name, type.getDescriptor());
    }

    @Override
    public int lineNo() {
        return 0;
    }

    @Override
    public String name() {
        return "";
    }

    @Override
    public Type type(PythonCompiler compiler) {
        return type;
    }

    public JvmClass ownerClass(PythonCompiler compiler) {
        Symbol symbol = compiler.symbols.get(owner.getClassName());
        if (symbol instanceof JvmClass jvmClass) {
            return jvmClass;
        }
        throw new IllegalStateException("Invalid owner: " + owner);
    }

    public JvmClass typeClass(PythonCompiler compiler) {
        Symbol symbol = compiler.symbols.get(type.getClassName());
        if (symbol instanceof JvmClass jvmClass) {
            return jvmClass;
        }
        throw new IllegalStateException("Invalid type: " + type);
    }

    @Override
    public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit) {
        if (type.getSort() == Type.OBJECT) {
            visit.load(mv, compiler, visit.preload(mv, compiler, true), true);
            compiler.writer.getContext().pop();

            mv.visitFieldInsn(Opcodes.PUTFIELD, owner.getDescriptor(), name, type.getDescriptor());
        } else {
            visit.load(mv, compiler, visit.preload(mv, compiler, false), false);
            if (compiler.writer.getContext().pop() != type) {
                int sort = visit.type(compiler).getSort();
                if (sort == Type.OBJECT) {
                    throw new RuntimeException("Cannot smart cast " + visit.type(compiler) + " to " + type);
                } else if (sort == Type.ARRAY) {
                    throw new RuntimeException("Cannot smart cast " + visit.type(compiler) + " to " + type);
                } else if (sort == Type.VOID) {
                    throw new RuntimeException("Cannot smart cast " + visit.type(compiler) + " to " + type);
                } else if (sort == Type.LONG) {
                    if (type.getSort() == Type.DOUBLE) {
                        mv.visitInsn(Opcodes.D2L);
                    } else if (type.getSort() == Type.INT) {
                        mv.visitInsn(Opcodes.L2I);
                    } else {
                        throw new RuntimeException("Cannot smart cast " + visit.type(compiler) + " to " + type);
                    }
                } else if (sort == Type.DOUBLE) {
                    if (type.getSort() == Type.LONG) {
                        mv.visitInsn(Opcodes.L2D);
                    } else if (type.getSort() == Type.INT) {
                        mv.visitInsn(Opcodes.I2D);
                    } else {
                        throw new RuntimeException("Cannot smart cast " + visit.type(compiler) + " to " + type);
                    }
                } else if (sort == Type.INT) {
                    if (type.getSort() == Type.LONG) {
                        mv.visitInsn(Opcodes.I2L);
                    } else if (type.getSort() == Type.DOUBLE) {
                        mv.visitInsn(Opcodes.I2D);
                    } else {
                        throw new RuntimeException("Cannot smart cast " + visit.type(compiler) + " to " + type);
                    }
                } else if (sort == Type.FLOAT) {
                    if (type.getSort() == Type.DOUBLE) {
                        mv.visitInsn(Opcodes.F2D);
                    } else if (type.getSort() == Type.INT) {
                        mv.visitInsn(Opcodes.F2I);
                    } else {
                        throw new RuntimeException("Cannot smart cast " + visit.type(compiler) + " to " + type);
                    }
                } else {
                    throw new RuntimeException("Cannot smart cast " + visit.type(compiler) + " to " + type);
                }
            }

            mv.visitFieldInsn(Opcodes.PUTFIELD, owner.getDescriptor(), name, type.getDescriptor());
        }
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
