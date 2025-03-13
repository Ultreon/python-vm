package dev.ultreon.pythonc;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

public class JvmWriter {
    public final PythonCompiler pc;

    public JvmWriter(PythonCompiler pythonCompiler) {
        this.pc = pythonCompiler;
    }


    public Context getContext() {
        Context context = pc.getContext(Context.class);
        if (context == null) {
            throw new RuntimeException("No context found");
        }
        return context;
    }

    public JvmClass getClassSymbol(String className) {
        return pc.getClassSymbol(className);
    }

    public void invokeStatic(String owner, String name, String signature, boolean isInterface) {
        Context context = getContext();
        Type methodType = Type.getMethodType(signature);
        Type[] argumentTypes = methodType.getArgumentTypes();
        for (Type arg : argumentTypes) {
            Type pop = context.pop();
            getClassSymbol(pop.getClassName());
        }

        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitMethodInsn(INVOKESTATIC, owner, name, signature, isInterface);
        Type returnType = methodType.getReturnType();
        if (returnType.equals(Type.VOID_TYPE)) return;
        context.push(returnType);
    }

    public void invokeSpecial(String owner, String name, String signature, boolean isInterface) {
        Context context = getContext();
        Type methodType = Type.getMethodType(signature);
        Type[] argumentTypes = methodType.getArgumentTypes();
        context.pop();
        for (Type arg : argumentTypes) {
            Type pop = context.pop();
            getClassSymbol(pop.getClassName());
        }
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitMethodInsn(INVOKESPECIAL, owner, name, signature, isInterface);
        Type returnType = methodType.getReturnType();
        if (returnType.equals(Type.VOID_TYPE)) return;
        context.push(returnType);
    }

    public void newInstance(String owner, String name, String signature, boolean isInterface, Runnable paramInit) {
        Context context = getContext();
        Type methodType = Type.getMethodType(signature);
        Type[] argumentTypes = methodType.getArgumentTypes();
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        newObject(Type.getObjectType(owner));
        dup();
        paramInit.run();
        invokeSpecial(owner, "<init>", signature, isInterface);
        Type returnType = methodType.getReturnType();
        if (returnType.equals(Type.VOID_TYPE)) return;
        context.push(returnType);
    }

    public void newObject(Type objectType) {
        Context context = getContext();
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitTypeInsn(NEW, objectType.getInternalName());
        context.push(objectType);
    }

    public void throwObject() {
        Context context = getContext();
        Type pop = context.pop();
        getClassSymbol(pop.getClassName());
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitInsn(ATHROW);
    }

    public void returnObject() {
        Context context = getContext();
        Type pop = context.pop();
        getClassSymbol(pop.getClassName());
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitInsn(ARETURN);
    }

    public void invokeVirtual(String owner, String name, String signature, boolean isInterface) {
        Context context = getContext();
        Type methodType = Type.getMethodType(signature);
        Type[] argumentTypes = methodType.getArgumentTypes();
        context.pop();
        for (Type arg : argumentTypes) {
            Type pop = context.pop();
            getClassSymbol(pop.getClassName());
        }
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitMethodInsn(INVOKEVIRTUAL, owner, name, signature, isInterface);
        Type returnType = methodType.getReturnType();
        if (returnType.equals(Type.VOID_TYPE)) return;
        context.push(returnType);
    }

    public void invokeInterface(String owner, String name, String signature, boolean isInterface) {
        Context context = getContext();
        Type methodType = Type.getMethodType(signature);
        Type[] argumentTypes = methodType.getArgumentTypes();
        context.pop();
        for (Type arg : argumentTypes) {
            Type pop = context.pop();
            getClassSymbol(pop.getClassName());
        }

        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitMethodInsn(INVOKEINTERFACE, owner, name, signature, isInterface);
        Type returnType = methodType.getReturnType();
        if (returnType.equals(Type.VOID_TYPE)) return;
        context.push(returnType);
    }

    public void invokeDynamic(String owner, String name, String signature, boolean isInterface) {
        Context context = getContext();
        Type methodType = Type.getMethodType(signature);
        Type[] argumentTypes = methodType.getArgumentTypes();
        context.pop();
        for (Type arg : argumentTypes) {
            Type pop = context.pop();
            getClassSymbol(pop.getClassName());
        }

        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitMethodInsn(INVOKEDYNAMIC, owner, name, signature, isInterface);
        Type returnType = methodType.getReturnType();
        if (returnType.equals(Type.VOID_TYPE)) return;
        context.push(returnType);
    }

    public void getStatic(String owner, String name, String descriptor) {
        Context context = getContext();
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        Type type = Type.getType(descriptor);
        mv.visitFieldInsn(GETSTATIC, owner, name, type.getDescriptor());
        context.push(type);
    }

    public void putStatic(String owner, String name, String descriptor) {
        Context context = getContext();
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        Type type = Type.getType(descriptor);
        cast(type);
        context.pop();
        mv.visitFieldInsn(PUTSTATIC, owner, name, type.getDescriptor());
    }

    public void getField(String owner, String name, String descriptor) {
        Context context = getContext();
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        Type type = Type.getType(descriptor);
        context.pop();
        mv.visitFieldInsn(GETFIELD, owner, name, type.getDescriptor());
        context.push(type);
    }

    public void putField(String owner, String name, String descriptor) {
        Context context = getContext();
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        Type type = Type.getType(descriptor);
        context.pop();
        context.pop();
        mv.visitFieldInsn(PUTFIELD, owner, name, type.getDescriptor());
    }

    public void loadConstant(String name) {
        Context context = getContext();
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitLdcInsn(name);
        context.push(Type.getType("Ljava/lang/String;"));
    }

    public void loadConstant(int value) {
        Context context = getContext();
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitLdcInsn(value);
        context.push(Type.INT_TYPE);
    }

    public void loadConstant(long value) {
        Context context = getContext();
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitLdcInsn(value);
        context.push(Type.LONG_TYPE);
    }

    public void loadConstant(float value) {
        Context context = getContext();
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitLdcInsn(value);
        context.push(Type.FLOAT_TYPE);
    }

    public void loadConstant(double value) {
        Context context = getContext();
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitLdcInsn(value);
        context.push(Type.DOUBLE_TYPE);
    }

    public void loadConstant(char value) {
        Context context = getContext();
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitLdcInsn(value);
        context.push(Type.CHAR_TYPE);
    }

    public void loadConstant(boolean value) {
        Context context = getContext();
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitLdcInsn(value);
        context.push(Type.BOOLEAN_TYPE);
    }

    public void loadConstant(byte value) {
        Context context = getContext();
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitLdcInsn(value);
        context.push(Type.BYTE_TYPE);
    }

    public void loadConstant(short value) {
        Context context = getContext();
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitLdcInsn(value);
        context.push(Type.SHORT_TYPE);
    }

    public void loadClass(Type type) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitLdcInsn(type);
    }

    public void storeInt(int index) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitVarInsn(ISTORE, index);
        Type pop = getContext().pop();
        if (!pop.equals(Type.INT_TYPE)) {
            throw new RuntimeException("Expected int, got " + pop);
        }
    }

    public void loadInt(int index) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitVarInsn(ILOAD, index);
        getContext().push(Type.INT_TYPE);
    }

    public void storeLong(int index) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitVarInsn(LSTORE, index);
        Type pop = getContext().pop();
        if (!pop.equals(Type.LONG_TYPE)) {
            throw new RuntimeException("Expected long, got " + pop);
        }
    }

    public void loadLong(int index) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitVarInsn(LLOAD, index);
        getContext().push(Type.LONG_TYPE);
    }

    public void storeFloat(int index) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitVarInsn(FSTORE, index);
        Type pop = getContext().pop();
        if (!pop.equals(Type.FLOAT_TYPE)) {
            throw new RuntimeException("Expected float, got " + pop);
        }
    }

    public void loadFloat(int index) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitVarInsn(FLOAD, index);
        getContext().push(Type.FLOAT_TYPE);
    }

    public void storeDouble(int index) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitVarInsn(DSTORE, index);
        Type pop = getContext().pop();
        if (!pop.equals(Type.DOUBLE_TYPE)) {
            throw new RuntimeException("Expected double, got " + pop);
        }
    }

    public void loadDouble(int index) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitVarInsn(DLOAD, index);
        getContext().push(Type.DOUBLE_TYPE);
    }

    public void storeChar(int index) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitVarInsn(ISTORE, index);
        Type pop = getContext().pop();
        if (!pop.equals(Type.CHAR_TYPE)) {
            throw new RuntimeException("Expected char, got " + pop);
        }
    }

    public void loadChar(int index) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitVarInsn(ILOAD, index);
        getContext().push(Type.CHAR_TYPE);
    }

    public void storeBoolean(int index) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        cast(Type.BOOLEAN_TYPE);
        mv.visitVarInsn(ISTORE, index);
        Type pop = getContext().pop();
        if (!pop.equals(Type.BOOLEAN_TYPE)) {
            throw new RuntimeException("Expected boolean, got " + pop);
        }
    }

    public void loadBoolean(int index) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitVarInsn(ILOAD, index);
        cast(Type.BOOLEAN_TYPE);
        getContext().push(Type.BOOLEAN_TYPE);
    }

    public void storeByte(int index) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        cast(Type.BYTE_TYPE);
        mv.visitVarInsn(ISTORE, index);
        Type pop = getContext().pop();
        if (!pop.equals(Type.BYTE_TYPE)) {
            throw new RuntimeException("Expected byte, got " + pop);
        }
    }

    public void loadByte(int index) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitVarInsn(ILOAD, index);
        cast(Type.BYTE_TYPE);
        getContext().push(Type.BYTE_TYPE);
    }

    public void storeShort(int index) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        cast(Type.SHORT_TYPE);
        mv.visitVarInsn(ISTORE, index);
        Type pop = getContext().pop();
        if (!pop.equals(Type.SHORT_TYPE)) {
            throw new RuntimeException("Expected short, got " + pop);
        }
    }

    public void loadShort(int index) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitVarInsn(ILOAD, index);
        cast(Type.SHORT_TYPE);
        getContext().push(Type.SHORT_TYPE);
    }

    public void storeObject(int index, Type type) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitVarInsn(ASTORE, index);
        getContext().pop();
    }

    public void loadObject(int index, Type type) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitVarInsn(ALOAD, index);
        getContext().push(Type.getType(Object.class));
        cast(type);
    }

    public void addValues() {
        Context context = getContext();
        Type left = context.pop();
        Type right = context.pop();

        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        if (left == Type.INT_TYPE && right == Type.INT_TYPE) {
            mv.visitInsn(IADD);
            context.push(Type.INT_TYPE);
        } else if (left == Type.LONG_TYPE && right == Type.INT_TYPE) {
            mv.visitInsn(LADD);
            context.push(Type.LONG_TYPE);
        } else if (left == Type.INT_TYPE && right == Type.LONG_TYPE) {
            mv.visitInsn(LADD);
            context.push(Type.LONG_TYPE);
        } else if (left == Type.LONG_TYPE && right == Type.LONG_TYPE) {
            mv.visitInsn(LADD);
            context.push(Type.LONG_TYPE);
        } else if (left == Type.DOUBLE_TYPE && right == Type.DOUBLE_TYPE) {
            mv.visitInsn(DADD);
            context.push(Type.DOUBLE_TYPE);
        } else if (left == Type.DOUBLE_TYPE && right == Type.INT_TYPE) {
            mv.visitInsn(DADD);
            context.push(Type.DOUBLE_TYPE);
        } else if (left == Type.INT_TYPE && right == Type.DOUBLE_TYPE) {
            mv.visitInsn(DADD);
            context.push(Type.DOUBLE_TYPE);
        } else if (left == Type.DOUBLE_TYPE && right == Type.LONG_TYPE) {
            mv.visitInsn(DADD);
            context.push(Type.DOUBLE_TYPE);
        } else if (left == Type.LONG_TYPE && right == Type.DOUBLE_TYPE) {
            mv.visitInsn(DADD);
            context.push(Type.DOUBLE_TYPE);
        } else if (left == Type.FLOAT_TYPE && right == Type.FLOAT_TYPE) {
            mv.visitInsn(FADD);
            context.push(Type.FLOAT_TYPE);
        } else if (left == Type.FLOAT_TYPE && right == Type.INT_TYPE) {
            mv.visitInsn(FADD);
            context.push(Type.FLOAT_TYPE);
        } else if (left == Type.INT_TYPE && right == Type.FLOAT_TYPE) {
            mv.visitInsn(FADD);
            context.push(Type.FLOAT_TYPE);
        } else if (left == Type.FLOAT_TYPE && right == Type.LONG_TYPE) {
            mv.visitInsn(FADD);
            context.push(Type.FLOAT_TYPE);
        } else if (left == Type.LONG_TYPE && right == Type.FLOAT_TYPE) {
            mv.visitInsn(FADD);
            context.push(Type.FLOAT_TYPE);
        } else {
            throw new RuntimeException("Unsupported addition between " + left + " and " + right);
        }
    }

    public void subtractValues() {
        Context context = getContext();
        Type left = context.pop();
        Type right = context.pop();

        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        if (left == Type.INT_TYPE && right == Type.INT_TYPE) {
            mv.visitInsn(ISUB);
            context.push(Type.INT_TYPE);
        } else if (left == Type.LONG_TYPE && right == Type.INT_TYPE) {
            mv.visitInsn(LSUB);
            context.push(Type.LONG_TYPE);
        } else if (left == Type.INT_TYPE && right == Type.LONG_TYPE) {
            mv.visitInsn(LSUB);
            context.push(Type.LONG_TYPE);
        } else if (left == Type.LONG_TYPE && right == Type.LONG_TYPE) {
            mv.visitInsn(LSUB);
            context.push(Type.LONG_TYPE);
        } else if (left == Type.DOUBLE_TYPE && right == Type.DOUBLE_TYPE) {
            mv.visitInsn(DSUB);
            context.push(Type.DOUBLE_TYPE);
        } else if (left == Type.DOUBLE_TYPE && right == Type.INT_TYPE) {
            mv.visitInsn(DSUB);
            context.push(Type.DOUBLE_TYPE);
        } else if (left == Type.INT_TYPE && right == Type.DOUBLE_TYPE) {
            mv.visitInsn(DSUB);
            context.push(Type.DOUBLE_TYPE);
        } else if (left == Type.DOUBLE_TYPE && right == Type.LONG_TYPE) {
            mv.visitInsn(DSUB);
            context.push(Type.DOUBLE_TYPE);
        } else if (left == Type.LONG_TYPE && right == Type.DOUBLE_TYPE) {
            mv.visitInsn(DSUB);
            context.push(Type.DOUBLE_TYPE);
        } else if (left == Type.FLOAT_TYPE && right == Type.FLOAT_TYPE) {
            mv.visitInsn(FSUB);
            context.push(Type.FLOAT_TYPE);
        } else if (left == Type.FLOAT_TYPE && right == Type.INT_TYPE) {
            mv.visitInsn(FSUB);
            context.push(Type.FLOAT_TYPE);
        } else if (left == Type.INT_TYPE && right == Type.FLOAT_TYPE) {
            mv.visitInsn(FSUB);
            context.push(Type.FLOAT_TYPE);
        } else if (left == Type.FLOAT_TYPE && right == Type.LONG_TYPE) {
            mv.visitInsn(FSUB);
            context.push(Type.FLOAT_TYPE);
        } else if (left == Type.LONG_TYPE && right == Type.FLOAT_TYPE) {
            mv.visitInsn(FSUB);
            context.push(Type.FLOAT_TYPE);
        } else {
            throw new RuntimeException("Unsupported subtraction between " + left + " and " + right);
        }
    }

    public void multiplyValues() {
        Context context = getContext();
        Type left = context.pop();
        Type right = context.pop();

        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        if (left == Type.INT_TYPE && right == Type.INT_TYPE) {
            mv.visitInsn(IMUL);
            context.push(Type.INT_TYPE);
        } else if (left == Type.LONG_TYPE && right == Type.INT_TYPE) {
            mv.visitInsn(LMUL);
            context.push(Type.LONG_TYPE);
        } else if (left == Type.INT_TYPE && right == Type.LONG_TYPE) {
            mv.visitInsn(LMUL);
            context.push(Type.LONG_TYPE);
        } else if (left == Type.LONG_TYPE && right == Type.LONG_TYPE) {
            mv.visitInsn(LMUL);
            context.push(Type.LONG_TYPE);
        } else if (left == Type.DOUBLE_TYPE && right == Type.DOUBLE_TYPE) {
            mv.visitInsn(DMUL);
            context.push(Type.DOUBLE_TYPE);
        } else if (left == Type.DOUBLE_TYPE && right == Type.INT_TYPE) {
            mv.visitInsn(DMUL);
            context.push(Type.DOUBLE_TYPE);
        } else if (left == Type.INT_TYPE && right == Type.DOUBLE_TYPE) {
            mv.visitInsn(DMUL);
            context.push(Type.DOUBLE_TYPE);
        } else if (left == Type.DOUBLE_TYPE && right == Type.LONG_TYPE) {
            mv.visitInsn(DMUL);
            context.push(Type.DOUBLE_TYPE);
        } else if (left == Type.LONG_TYPE && right == Type.DOUBLE_TYPE) {
            mv.visitInsn(DMUL);
            context.push(Type.DOUBLE_TYPE);
        } else if (left == Type.FLOAT_TYPE && right == Type.FLOAT_TYPE) {
            mv.visitInsn(FMUL);
            context.push(Type.FLOAT_TYPE);
        } else if (left == Type.FLOAT_TYPE && right == Type.INT_TYPE) {
            mv.visitInsn(FMUL);
            context.push(Type.FLOAT_TYPE);
        } else if (left == Type.INT_TYPE && right == Type.FLOAT_TYPE) {
            mv.visitInsn(FMUL);
            context.push(Type.FLOAT_TYPE);
        } else if (left == Type.FLOAT_TYPE && right == Type.LONG_TYPE) {
            mv.visitInsn(FMUL);
            context.push(Type.FLOAT_TYPE);
        } else if (left == Type.LONG_TYPE && right == Type.FLOAT_TYPE) {
            mv.visitInsn(FMUL);
            context.push(Type.FLOAT_TYPE);
        } else {
            throw new RuntimeException("Unsupported multiplication between " + left + " and " + right);
        }
    }

    public void divideValues() {
        Context context = getContext();
        Type left = context.pop();
        Type right = context.pop();

        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        if (left == Type.INT_TYPE && right == Type.INT_TYPE) {
            mv.visitInsn(IDIV);
            context.push(Type.INT_TYPE);
        } else if (left == Type.LONG_TYPE && right == Type.INT_TYPE) {
            mv.visitInsn(LDIV);
            context.push(Type.LONG_TYPE);
        } else if (left == Type.INT_TYPE && right == Type.LONG_TYPE) {
            mv.visitInsn(LDIV);
            context.push(Type.LONG_TYPE);
        } else if (left == Type.LONG_TYPE && right == Type.LONG_TYPE) {
            mv.visitInsn(LDIV);
            context.push(Type.LONG_TYPE);
        } else if (left == Type.DOUBLE_TYPE && right == Type.DOUBLE_TYPE) {
            mv.visitInsn(DDIV);
            context.push(Type.DOUBLE_TYPE);
        } else if (left == Type.DOUBLE_TYPE && right == Type.INT_TYPE) {
            mv.visitInsn(DDIV);
            context.push(Type.DOUBLE_TYPE);
        } else if (left == Type.INT_TYPE && right == Type.DOUBLE_TYPE) {
            mv.visitInsn(DDIV);
            context.push(Type.DOUBLE_TYPE);
        } else if (left == Type.DOUBLE_TYPE && right == Type.LONG_TYPE) {
            mv.visitInsn(DDIV);
            context.push(Type.DOUBLE_TYPE);
        } else if (left == Type.LONG_TYPE && right == Type.DOUBLE_TYPE) {
            mv.visitInsn(DDIV);
            context.push(Type.DOUBLE_TYPE);
        } else if (left == Type.FLOAT_TYPE && right == Type.FLOAT_TYPE) {
            mv.visitInsn(FDIV);
            context.push(Type.FLOAT_TYPE);
        } else if (left == Type.FLOAT_TYPE && right == Type.INT_TYPE) {
            mv.visitInsn(FDIV);
            context.push(Type.FLOAT_TYPE);
        } else if (left == Type.INT_TYPE && right == Type.FLOAT_TYPE) {
            mv.visitInsn(FDIV);
            context.push(Type.FLOAT_TYPE);
        } else if (left == Type.FLOAT_TYPE && right == Type.LONG_TYPE) {
            mv.visitInsn(FDIV);
            context.push(Type.FLOAT_TYPE);
        } else if (left == Type.LONG_TYPE && right == Type.FLOAT_TYPE) {
            mv.visitInsn(FDIV);
            context.push(Type.FLOAT_TYPE);
        } else {
            throw new RuntimeException("Unsupported division between " + left + " and " + right);
        }
    }

    public void modValues() {
        Context context = getContext();
        Type left = context.pop();
        Type right = context.pop();

        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        if (left == Type.INT_TYPE && right == Type.INT_TYPE) {
            mv.visitInsn(IREM);
            context.push(Type.INT_TYPE);
        } else if (left == Type.LONG_TYPE && right == Type.INT_TYPE) {
            mv.visitInsn(LREM);
            context.push(Type.LONG_TYPE);
        } else if (left == Type.INT_TYPE && right == Type.LONG_TYPE) {
            mv.visitInsn(LREM);
            context.push(Type.LONG_TYPE);
        } else if (left == Type.LONG_TYPE && right == Type.LONG_TYPE) {
            mv.visitInsn(LREM);
            context.push(Type.LONG_TYPE);
        } else {
            throw new RuntimeException("Unsupported modulus between " + left + " and " + right);
        }
    }

    public void andValues() {
        Context context = getContext();
        Type left = context.pop();
        Type right = context.pop();

        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        if (left == Type.INT_TYPE && right == Type.INT_TYPE) {
            mv.visitInsn(IAND);
            context.push(Type.INT_TYPE);
        } else if (left == Type.LONG_TYPE && right == Type.INT_TYPE) {
            mv.visitInsn(LAND);
            context.push(Type.LONG_TYPE);
        } else if (left == Type.INT_TYPE && right == Type.LONG_TYPE) {
            mv.visitInsn(LAND);
            context.push(Type.LONG_TYPE);
        } else if (left == Type.LONG_TYPE && right == Type.LONG_TYPE) {
            mv.visitInsn(LAND);
            context.push(Type.LONG_TYPE);
        } else {
            throw new RuntimeException("Unsupported and between " + left + " and " + right);
        }
    }

    public void orValues() {
        Context context = getContext();
        Type left = context.pop();
        Type right = context.pop();

        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        if (left == Type.INT_TYPE && right == Type.INT_TYPE) {
            mv.visitInsn(IOR);
            context.push(Type.INT_TYPE);
        } else if (left == Type.LONG_TYPE && right == Type.INT_TYPE) {
            mv.visitInsn(LOR);
            context.push(Type.LONG_TYPE);
        } else if (left == Type.INT_TYPE && right == Type.LONG_TYPE) {
            mv.visitInsn(LOR);
            context.push(Type.LONG_TYPE);
        } else if (left == Type.LONG_TYPE && right == Type.LONG_TYPE) {
            mv.visitInsn(LOR);
            context.push(Type.LONG_TYPE);
        } else {
            throw new RuntimeException("Unsupported or between " + left + " and " + right);
        }
    }

    public void xorValues() {
        Context context = getContext();
        Type left = context.pop();
        Type right = context.pop();

        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        if (left == Type.INT_TYPE && right == Type.INT_TYPE) {
            mv.visitInsn(IXOR);
            context.push(Type.INT_TYPE);
        } else if (left == Type.LONG_TYPE && right == Type.INT_TYPE) {
            mv.visitInsn(LXOR);
            context.push(Type.LONG_TYPE);
        } else if (left == Type.INT_TYPE && right == Type.LONG_TYPE) {
            mv.visitInsn(LXOR);
            context.push(Type.LONG_TYPE);
        } else if (left == Type.LONG_TYPE && right == Type.LONG_TYPE) {
            mv.visitInsn(LXOR);
            context.push(Type.LONG_TYPE);
        } else {
            throw new RuntimeException("Unsupported xor between " + left + " and " + right);
        }
    }

    public void jump(Label endLabel) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitJumpInsn(Opcodes.GOTO, endLabel);
    }

    public void cast(Type type) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        Context context = getContext();
        if (!context.peek().equals(type)) {
            Type pop = context.pop();
            if (pop.equals(Type.BYTE_TYPE)) invokeStatic("java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
            if (pop.equals(Type.SHORT_TYPE)) invokeStatic("java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
            if (pop.equals(Type.CHAR_TYPE))
                invokeStatic("java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
            if (pop.equals(Type.INT_TYPE))
                invokeStatic("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
            if (pop.equals(Type.LONG_TYPE)) invokeStatic("java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
            if (pop.equals(Type.FLOAT_TYPE)) invokeStatic("java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
            if (pop.equals(Type.DOUBLE_TYPE))
                invokeStatic("java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
            if (pop.equals(Type.BOOLEAN_TYPE))
                invokeStatic("java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
            mv.visitTypeInsn(CHECKCAST, type.getInternalName());
            context.push(type);
        }
    }

    public void dup() {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitInsn(DUP);

        Context context = getContext();
        context.push(context.peek());
    }

    public void pop() {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitInsn(POP);

        Context context = getContext();
        context.pop();
    }

    public void swap() {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitInsn(SWAP);

        Context context = getContext();
        Type t1 = context.pop();
        Type t2 = context.pop();
        context.push(t2);
        context.push(t1);
    }

    public void dup2() {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitInsn(DUP2);

        Context context = getContext();
        context.push(context.peek());
        context.push(context.peek());
    }

    public void dup2_x1() {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitInsn(DUP2_X1);

        Context context = getContext();
        Type t1 = context.pop();
        Type t2 = context.pop();
        context.push(t2);
        context.push(t1);
        context.push(t2);
    }

    public void dup2_x2() {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitInsn(DUP2_X2);

        Context context = getContext();
        Type t1 = context.pop();
        Type t2 = context.pop();
        context.push(t2);
        context.push(t1);
        context.push(t2);
        context.push(t1);
    }

    public void pushStackByte(int i) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitIntInsn(BIPUSH, i);

        getContext().push(Type.BYTE_TYPE);
    }

    public void pushStackShort(int i) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitIntInsn(SIPUSH, i);

        getContext().push(Type.SHORT_TYPE);
    }

    public void lineNumber(int line, Label label) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitLineNumber(line, label);
    }

    public void jumpIfEqual(Label label) {
        Context context = getContext();
        Type pop = context.pop();
        if (!pop.equals(Type.BOOLEAN_TYPE)) throw new RuntimeException("Expected boolean, got " + pop);

        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitJumpInsn(Opcodes.IFEQ, label);
    }

    public void jumpIfNotEqual(Label label) {
        Context context = getContext();
        Type pop = context.pop();
        if (!pop.equals(Type.BOOLEAN_TYPE)) throw new RuntimeException("Expected boolean, got " + pop);

        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitJumpInsn(Opcodes.IFNE, label);
    }

    public void jumpIfLessThan(Label label) {
        Context context = getContext();
        Type pop = context.pop();
        if (!pop.equals(Type.BOOLEAN_TYPE)) throw new RuntimeException("Expected boolean, got " + pop);

        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitJumpInsn(Opcodes.IFLT, label);
    }

    public void jumpIfGreaterThan(Label label) {
        Context context = getContext();
        Type pop = context.pop();
        if (!pop.equals(Type.BOOLEAN_TYPE)) throw new RuntimeException("Expected boolean, got " + pop);

        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitJumpInsn(Opcodes.IFGT, label);
    }

    public void localVariable(String name, String desc, String signature, Label start, Label end, int index) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitLocalVariable(name, desc, signature, start, end, index);
    }

    public void end() {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    public void newArray(Type type) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        Context context = getContext();
        Type pop = context.pop();
        if (pop != Type.INT_TYPE && pop != Type.SHORT_TYPE && pop != Type.BYTE_TYPE)
            throw new RuntimeException("Expected stack int, got " + pop);
        mv.visitTypeInsn(ANEWARRAY, type.getInternalName());

        context.push(Type.getType("[" + type.getDescriptor()));
    }

    public void arrayStoreObject(Type type) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        cast(type);
        mv.visitInsn(AASTORE);

        Context context = getContext();
        context.pop();
        context.pop();
        context.pop();
    }

    public void label(Label label) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitLabel(label);
    }

    public void returnVoid() {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitInsn(RETURN);
    }

    public void returnInt() {
        Context context = getContext();
        context.pop(Type.INT_TYPE);

        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitInsn(IRETURN);
    }

    public void returnLong() {
        Context context = getContext();
        context.pop(Type.LONG_TYPE);

        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitInsn(LRETURN);
    }

    public void returnFloat() {
        Context context = getContext();
        context.pop(Type.FLOAT_TYPE);

        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitInsn(FRETURN);
    }

    public void forLoop(Object targets, List<?> expressions, PythonParser.BlockContext block) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;

        Object expr;
        if (expressions.size() == 1) {
            expr = expressions.getFirst();
        } else {
            throw new UnsupportedOperationException("Multiple expressions in for loops are not supported yet");
        }

        Type type;
        if (expr instanceof PyExpr pyExpr) {
            mv.visitLineNumber(pyExpr.lineNo(), new Label());
            type = pyExpr.type(pc);
            forExprDetect(pyExpr, mv, type);
        } else {
            throw new RuntimeException("Can't iterate over " + expr);
        }

        int iteratorIndex = pc.currentVariableIndex;
        pc.currentVariableIndex += pc.computationalType(type);
        storeObject(iteratorIndex, type);

        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterable", "iterator", "()Ljava/util/Iterator;", true); // Call iterator()

        // Start the iteration: while (it.hasNext())
        Label startLoop = new Label();
        Label endLoop = new Label();
        mv.visitLabel(startLoop);

        // Inside the loop: if (!it.hasNext()) break;
        mv.visitVarInsn(ALOAD, 2); // Load the Iterator reference
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z", true); // Call hasNext() on the iterator
        mv.visitJumpInsn(IFEQ, endLoop); // If true (i.e., there is a next element), continue; otherwise, exit the loop

        // Inside the loop: System.out.println(it.next());
        mv.visitVarInsn(ALOAD, 2); // Load the Iterator reference
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;", true); // Call next()
        mv.visitVarInsn(ASTORE, pc.currentVariableIndex += pc.computationalType(Type.getType(Object.class)));

        mv.visitFrame(F_APPEND, 1, new Object[]{Type.getType("Ljava/util/Iterator;")}, 0, null);

        // Call it.hasNext(): it.hasNext()
        String targetName = null;
        if (targets instanceof PyObjectRef ref) {
            int v = pc.currentVariableIndex += pc.computationalType(Type.getType(Object.class));
            Type type1 = Type.getType(Object.class);
            mv.visitLocalVariable(ref.name(), type1.getDescriptor(), null, startLoop, endLoop, v);
            targetName = ref.name();
            pc.symbols.put(ref.name(), new PyVariable(ref.name(), type1, v, ref.lineNo(), startLoop));
            Object preload = ref.preload(mv, pc, false);
            ref.load(mv, pc, preload, false);
        } else if (!(targets instanceof List<?>)) {
            throw new RuntimeException("Can't iterate to " + targets);
        } else for (Object target : (List<?>) targets) {
            if (target instanceof PyExpr pyExpr1) {
                invokeInterface("java/util/Iterator", "hasNext", "()Z", true); // Call hasNext() on the iterator
            } else {
                throw new RuntimeException("Can't iterate to a " + target);
            }
        }
        if (targetName == null) {
            throw new RuntimeException("Can't iterate to " + targets);
        }


        pc.visit(block);

        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

        mv.visitLabel(endLoop);

        mv.visitFrame(F_CHOP, 1, new Object[]{targetName}, 0, new Object[0]);
        pc.symbols.remove(targetName);

        // Go back to the start of the loop
        mv.visitJumpInsn(GOTO, startLoop);

        // End of loop
        mv.visitLabel(endLoop);
    }

    private void forExprDetect(PyExpr pyExpr, MethodVisitor mv, Type type) {
        if (pyExpr instanceof FuncCall funcCall) {
            Constructor<?> constructor = funcCall.constructor;
            if (constructor != null) {
                if (Iterable.class.isAssignableFrom(constructor.getDeclaringClass())) {
                    pyExpr.load(mv, pc, pyExpr.preload(mv, pc, false), false);
                    return;
                }
            }
        }
        if (!type.getInternalName().equals("java/lang/Iterable"))
            throw new RuntimeException("Can't iterate over " + type.getClassName());
        pyExpr.load(mv, pc, pyExpr.preload(mv, pc, false), false);
    }
}
