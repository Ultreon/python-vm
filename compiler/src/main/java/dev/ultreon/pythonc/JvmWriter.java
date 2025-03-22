package dev.ultreon.pythonc;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

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

    public void floorDivideValues() {
        Context context = getContext();
        Type left = context.pop();
        Type right = context.pop();

        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        if (left == Type.INT_TYPE && right == Type.INT_TYPE) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "floorDiv", "(II)I", false);
            context.push(Type.INT_TYPE);
        } else if (left == Type.LONG_TYPE && right == Type.INT_TYPE) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "floorDiv", "(JI)J", false);
            context.push(Type.LONG_TYPE);
        } else if (left == Type.INT_TYPE && right == Type.LONG_TYPE) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "floorDiv", "(JI)J", false);
            context.push(Type.LONG_TYPE);
        } else if (left == Type.LONG_TYPE && right == Type.LONG_TYPE) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "floorDiv", "(JJ)J", false);
            context.push(Type.LONG_TYPE);
        } else {
            throw new RuntimeException("Unsupported floorDiv between " + left + " and " + right);
        }
    }

    public void powValues() {
        Context context = getContext();
        Type left = context.pop();
        Type right = context.pop();

        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        if (left == Type.DOUBLE_TYPE && right == Type.DOUBLE_TYPE) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "pow", "(DD)D", false);
            context.push(Type.DOUBLE_TYPE);
        } else {
            throw new RuntimeException("Unsupported pow between " + left + " and " + right);
        }
    }

    public void notValue() {
        Context context = getContext();
        Type value = context.pop();
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        if (value == Type.INT_TYPE) {
            mv.visitInsn(ICONST_M1); // Push -1
            mv.visitInsn(IXOR);      // Perform value ^ -1
            context.push(Type.INT_TYPE);
        } else if (value == Type.LONG_TYPE) {
            mv.visitLdcInsn(-1L); // Push -1 as a long
            mv.visitInsn(LXOR);   // Perform value ^ -1
            context.push(Type.LONG_TYPE);
        } else {
            throw new RuntimeException("Unsupported not for " + value);
        }
    }

    public void positiveValue() {
        Context context = getContext();
        Type pop = context.pop();
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        if (pop == Type.BOOLEAN_TYPE) {
            // Assume boolean value is on the stack (1 for true, 0 for false)
            Label isFalse = new Label();
            Label end = new Label();

            mv.visitJumpInsn(IFEQ, isFalse); // If value == 0 (false), jump
            mv.visitInsn(ICONST_1);  // Push 1 for true
            mv.visitJumpInsn(GOTO, end);
            mv.visitLabel(isFalse);
            mv.visitInsn(ICONST_0);  // Push 0 for false
            mv.visitLabel(end);

            context.push(Type.INT_TYPE);
        } else {
            context.push(pop);
        }
    }

    public void negateValue() {
        Context context = getContext();
        Type value = context.pop();
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        if (value == Type.INT_TYPE) {
            mv.visitInsn(INEG);
            context.push(Type.INT_TYPE);
        } else if (value == Type.LONG_TYPE) {
            mv.visitInsn(LNEG);
            context.push(Type.LONG_TYPE);
        } else if (value == Type.DOUBLE_TYPE) {
            mv.visitInsn(DNEG);
            context.push(Type.DOUBLE_TYPE);
        } else if (value == Type.FLOAT_TYPE) {
            mv.visitInsn(FNEG);
            context.push(Type.FLOAT_TYPE);
        } else {
            throw new RuntimeException("Unsupported negate for " + value);
        }
    }

    public void leftShiftValues() {
        Context context = getContext();
        Type left = context.pop();
        Type right = context.pop();
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        if (left == Type.INT_TYPE && right == Type.INT_TYPE) {
            mv.visitInsn(ISHL);
            context.push(Type.INT_TYPE);
        } else if (left == Type.LONG_TYPE && right == Type.INT_TYPE) {
            mv.visitInsn(LSHL);
            context.push(Type.LONG_TYPE);
        } else if (left == Type.INT_TYPE && right == Type.LONG_TYPE) {
            mv.visitInsn(L2I);
            mv.visitInsn(LSHL);
            context.push(Type.LONG_TYPE);
        } else if (left == Type.LONG_TYPE && right == Type.LONG_TYPE) {
            mv.visitInsn(L2I);
            mv.visitInsn(LSHL);
            context.push(Type.LONG_TYPE);
        } else {
            throw new RuntimeException("Unsupported left shift between " + left + " and " + right);
        }
    }

    public void rightShiftValues() {
        Context context = getContext();
        Type left = context.pop();
        Type right = context.pop();
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        if (left == Type.INT_TYPE && right == Type.INT_TYPE) {
            mv.visitInsn(ISHR);
            context.push(Type.INT_TYPE);
        } else if (left == Type.LONG_TYPE && right == Type.INT_TYPE) {
            mv.visitInsn(LSHR);
            context.push(Type.LONG_TYPE);
        } else if (left == Type.INT_TYPE && right == Type.LONG_TYPE) {
            mv.visitInsn(L2I);
            mv.visitInsn(LSHR);
            context.push(Type.LONG_TYPE);
        } else if (left == Type.LONG_TYPE && right == Type.LONG_TYPE) {
            mv.visitInsn(L2I);
            mv.visitInsn(LSHR);
            context.push(Type.LONG_TYPE);
        } else {
            throw new RuntimeException("Unsupported right shift between " + left + " and " + right);
        }
    }

    public void jump(Label endLabel) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitJumpInsn(Opcodes.GOTO, endLabel);
    }

    public void cast(Type type) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        Context context = getContext();
        Type pop = context.pop();
        if (type.getSort() == Type.OBJECT) {
            if (!pop.equals(type)) {
                if (pop.equals(Type.BYTE_TYPE)) invokeStatic("java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
                if (pop.equals(Type.SHORT_TYPE))
                    invokeStatic("java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
                if (pop.equals(Type.CHAR_TYPE))
                    invokeStatic("java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
                if (pop.equals(Type.INT_TYPE))
                    invokeStatic("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                if (pop.equals(Type.LONG_TYPE)) invokeStatic("java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
                if (pop.equals(Type.FLOAT_TYPE))
                    invokeStatic("java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
                if (pop.equals(Type.DOUBLE_TYPE))
                    invokeStatic("java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                if (pop.equals(Type.BOOLEAN_TYPE))
                    invokeStatic("java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
                mv.visitTypeInsn(CHECKCAST, type.getInternalName());
            }
        } else switch (type.getSort()) {
            case Type.BOOLEAN -> {
                switch (pop.getSort()) {
                    case Type.BOOLEAN, Type.INT -> {
                        // do nothing
                    }
                    case Type.BYTE -> mv.visitInsn(I2B);
                    case Type.SHORT -> mv.visitInsn(I2S);
                    case Type.CHAR -> mv.visitInsn(I2C);
                    case Type.LONG -> mv.visitInsn(L2I);
                    case Type.FLOAT -> mv.visitInsn(F2I);
                    case Type.DOUBLE -> mv.visitInsn(D2I);
                    default -> throw new RuntimeException("Unsupported cast to " + type);
                }
            }
            case Type.INT -> {
                switch (pop.getSort()) {
                    case Type.INT -> {
                        // do nothing
                    }
                    case Type.BYTE -> mv.visitInsn(I2B);
                    case Type.SHORT -> mv.visitInsn(I2S);
                    case Type.CHAR -> mv.visitInsn(I2C);
                    case Type.LONG -> mv.visitInsn(I2L);
                    case Type.FLOAT -> mv.visitInsn(F2I);
                    case Type.DOUBLE -> mv.visitInsn(D2I);
                    default -> throw new RuntimeException("Unsupported cast to " + type);
                }
            }
            case Type.LONG -> {
                switch (pop.getSort()) {
                    case Type.INT -> mv.visitInsn(I2L);
                    case Type.BYTE -> mv.visitInsn(I2B);
                    case Type.SHORT -> mv.visitInsn(I2S);
                    case Type.CHAR -> mv.visitInsn(I2C);
                    case Type.LONG -> {
                        // do nothing
                    }
                    case Type.FLOAT -> mv.visitInsn(F2L);
                    case Type.DOUBLE -> mv.visitInsn(D2L);
                    default -> throw new RuntimeException("Unsupported cast to " + type);
                }
            }
            case Type.FLOAT -> {
                switch (pop.getSort()) {
                    case Type.INT -> mv.visitInsn(I2F);
                    case Type.BYTE -> mv.visitInsn(I2B);
                    case Type.SHORT -> mv.visitInsn(I2S);
                    case Type.CHAR -> mv.visitInsn(I2C);
                    case Type.LONG -> mv.visitInsn(L2F);
                    case Type.FLOAT -> {
                        // do nothing
                    }
                    case Type.DOUBLE -> mv.visitInsn(D2F);
                    default -> throw new RuntimeException("Unsupported cast to " + type);
                }
            }
            case Type.DOUBLE -> {
                switch (pop.getSort()) {
                    case Type.INT -> mv.visitInsn(I2D);
                    case Type.BYTE -> mv.visitInsn(I2B);
                    case Type.SHORT -> mv.visitInsn(I2S);
                    case Type.CHAR -> mv.visitInsn(I2C);
                    case Type.LONG -> mv.visitInsn(L2D);
                    case Type.FLOAT -> mv.visitInsn(F2D);
                    case Type.DOUBLE -> {
                        // do nothing
                    }
                    default -> throw new RuntimeException("Unsupported cast from " + pop + " to " + type);
                }
            }
            default -> throw new RuntimeException("Unsupported cast to " + type);
        }
        context.push(type);
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
        smartCast(pop, getContext().peek());
        context.pop();

        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitJumpInsn(Opcodes.IFEQ, label);
    }

    public void smartCast(Type from, Type to) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        if (to == Type.DOUBLE_TYPE) {
            if (from == Type.LONG_TYPE) {
                mv.visitInsn(L2D);
            } else if (from == Type.FLOAT_TYPE) {
                mv.visitInsn(F2D);
            } else if (from == Type.INT_TYPE) {
                mv.visitInsn(I2D);
            }
        } else if (to == Type.FLOAT_TYPE) {
            if (from == Type.DOUBLE_TYPE) {
                mv.visitInsn(D2F);
            } else if (from == Type.LONG_TYPE) {
                mv.visitInsn(L2F);
            } else if (from == Type.INT_TYPE) {
                mv.visitInsn(I2F);
            }
        } else if (to == Type.LONG_TYPE) {
            if (from == Type.DOUBLE_TYPE) {
                mv.visitInsn(D2L);
            } else if (from == Type.FLOAT_TYPE) {
                mv.visitInsn(F2L);
            } else if (from == Type.INT_TYPE) {
                mv.visitInsn(I2L);
            }
        } else if (to == Type.INT_TYPE) {
            if (from == Type.DOUBLE_TYPE) {
                mv.visitInsn(D2I);
            } else if (from == Type.FLOAT_TYPE) {
                mv.visitInsn(F2I);
            } else if (from == Type.LONG_TYPE) {
                mv.visitInsn(L2I);
            }
        } else if (to == Type.BOOLEAN_TYPE) {
            if (from == Type.DOUBLE_TYPE) {
                mv.visitInsn(D2I);
            } else if (from == Type.FLOAT_TYPE) {
                mv.visitInsn(F2I);
            } else if (from == Type.LONG_TYPE) {
                mv.visitInsn(L2I);
            }
        } else if (to == Type.CHAR_TYPE) {
            if (from == Type.DOUBLE_TYPE) {
                mv.visitInsn(D2I);
                mv.visitInsn(I2C);
            } else if (from == Type.FLOAT_TYPE) {
                mv.visitInsn(F2I);
                mv.visitInsn(I2C);
            } else if (from == Type.LONG_TYPE) {
                mv.visitInsn(L2I);
                mv.visitInsn(I2C);
            } else if (from == Type.INT_TYPE) {
                mv.visitInsn(I2C);
            }
        } else if (to == Type.SHORT_TYPE) {
            if (from == Type.DOUBLE_TYPE) {
                mv.visitInsn(D2I);
                mv.visitInsn(I2S);
            } else if (from == Type.FLOAT_TYPE) {
                mv.visitInsn(F2I);
                mv.visitInsn(I2S);
            } else if (from == Type.LONG_TYPE) {
                mv.visitInsn(L2I);
                mv.visitInsn(I2S);
            } else if (from == Type.INT_TYPE) {
                mv.visitInsn(I2S);
            }
        } else if (to == Type.BYTE_TYPE) {
            if (from == Type.DOUBLE_TYPE) {
                mv.visitInsn(D2I);
                mv.visitInsn(I2B);
            } else if (from == Type.FLOAT_TYPE) {
                mv.visitInsn(F2I);
                mv.visitInsn(I2B);
            } else if (from == Type.LONG_TYPE) {
                mv.visitInsn(L2I);
                mv.visitInsn(I2B);
            } else if (from == Type.INT_TYPE) {
                mv.visitInsn(I2B);
            }
        } else if (to.getSort() == Type.OBJECT) {
            if (from == Type.DOUBLE_TYPE) {
                if (to.getInternalName().equals("java/lang/Double")) {
                    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                } else {
                    smartCast(from, boxType(to));
                }
            } else if (from == Type.FLOAT_TYPE && to.getInternalName().equals("java/lang/Float")) {
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
            } else if (from == Type.LONG_TYPE && to.getInternalName().equals("java/lang/Long")) {
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
            } else if (from == Type.INT_TYPE && to.getInternalName().equals("java/lang/Integer")) {
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
            } else if (from == Type.BOOLEAN_TYPE && to.getInternalName().equals("java/lang/Boolean")) {
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
            } else if (from == Type.CHAR_TYPE && to.getInternalName().equals("java/lang/Character")) {
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
            } else if (from == Type.BYTE_TYPE && to.getInternalName().equals("java/lang/Byte")) {
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
            } else if (from == Type.SHORT_TYPE && to.getInternalName().equals("java/lang/Short")) {
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
            } else if (!from.equals(to)) {
                throw new RuntimeException("Cannot smart cast " + from + " to " + to);
            }
        } else {
            throw new RuntimeException("Cannot smart cast " + from + " to " + to);
        }
    }

    public Type boxType(Type from) {
        if (from == Type.DOUBLE_TYPE) {
            return Type.getType(Double.class);
        } else if (from == Type.FLOAT_TYPE) {
            return Type.getType(Float.class);
        } else if (from == Type.LONG_TYPE) {
            return Type.getType(Long.class);
        } else if (from == Type.INT_TYPE) {
            return Type.getType(Integer.class);
        } else if (from == Type.BOOLEAN_TYPE) {
            return Type.getType(Boolean.class);
        } else if (from == Type.CHAR_TYPE) {
            return Type.getType(Character.class);
        } else if (from == Type.BYTE_TYPE) {
            return Type.getType(Byte.class);
        } else if (from == Type.SHORT_TYPE) {
            return Type.getType(Short.class);
        } else {
            return from;
        }
    }

    public Type unboxType(Type from) {
        if (from == Type.getType(Double.class) || from == Type.DOUBLE_TYPE) {
            return Type.DOUBLE_TYPE;
        } else if (from == Type.getType(Float.class) || from == Type.FLOAT_TYPE) {
            return Type.FLOAT_TYPE;
        } else if (from == Type.getType(Long.class) || from == Type.LONG_TYPE) {
            return Type.LONG_TYPE;
        } else if (from == Type.getType(Integer.class) || from == Type.INT_TYPE) {
            return Type.INT_TYPE;
        } else if (from == Type.getType(Boolean.class) || from == Type.BOOLEAN_TYPE) {
            return Type.BOOLEAN_TYPE;
        } else if (from == Type.getType(Character.class) || from == Type.CHAR_TYPE) {
            return Type.CHAR_TYPE;
        } else if (from == Type.getType(Byte.class) || from == Type.BYTE_TYPE) {
            return Type.BYTE_TYPE;
        } else if (from == Type.getType(Short.class) || from == Type.SHORT_TYPE) {
            return Type.SHORT_TYPE;
        } else {
            return from;
        }
    }

    public void box(Type type) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        Type pop = getContext().pop();
        smartCast(pop, type);
        if (type == Type.DOUBLE_TYPE) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
        } else if (type == Type.FLOAT_TYPE) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
        } else if (type == Type.LONG_TYPE) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
        } else if (type == Type.INT_TYPE) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
        } else if (type == Type.BOOLEAN_TYPE) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
        } else if (type == Type.CHAR_TYPE) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
        } else if (type == Type.BYTE_TYPE) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
        } else if (type == Type.SHORT_TYPE) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
        }
        getContext().push(boxType(type));
    }

    public void unbox(Type type) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        Type pop = getContext().pop();
        smartCast(pop, type);
        if (type == Type.DOUBLE_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
        } else if (type == Type.FLOAT_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
        } else if (type == Type.LONG_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
        } else if (type == Type.INT_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
        } else if (type == Type.BOOLEAN_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
        } else if (type == Type.CHAR_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
        } else if (type == Type.BYTE_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
        } else if (type == Type.SHORT_TYPE) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
        }

        getContext().push(type);
    }

    public void jumpIfNotEqual(Label label) {
        Context context = getContext();
        Type pop = context.pop();

        smartCast(pop, getContext().peek());
        context.pop();

        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitJumpInsn(Opcodes.IFNE, label);
    }

    public void jumpIfLessThan(Label label) {
        Context context = getContext();
        Type pop = context.pop();

        smartCast(pop, getContext().peek());
        context.pop();

        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitJumpInsn(Opcodes.IFLT, label);
    }

    public void jumpIfLessThanOrEqual(Label label) {
        Context context = getContext();
        Type pop = context.pop();

        smartCast(pop, getContext().peek());
        context.pop();

        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitJumpInsn(Opcodes.IFLE, label);
    }

    public void jumpIfGreaterThan(Label label) {
        Context context = getContext();
        Type pop = context.pop();

        smartCast(pop, getContext().peek());
        context.pop();

        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitJumpInsn(Opcodes.IFGT, label);
    }

    public void jumpIfGreaterThanOrEqual(Label label) {
        Context context = getContext();
        Type pop = context.pop();

        smartCast(pop, getContext().peek());
        context.pop();

        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitJumpInsn(Opcodes.IFGE, label);
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

    public void returnFloatValues() {
        Context context = getContext();
        context.pop(Type.FLOAT_TYPE);

        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitInsn(FRETURN);
    }

    public void pushZeroInt() {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitInsn(ICONST_0);
        getContext().push(Type.INT_TYPE);
    }
    
    public void pushOneInt() {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitInsn(ICONST_1);
        getContext().push(Type.INT_TYPE);
    }

    public void pushFalse() {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitInsn(ICONST_0);
        getContext().push(Type.BOOLEAN_TYPE);
    }
    
    public void pushTrue() {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitInsn(ICONST_1);
        getContext().push(Type.BOOLEAN_TYPE);
    }

    public void pushBoolean(Boolean b) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitInsn(b ? ICONST_1 : ICONST_0);
        getContext().push(Type.BOOLEAN_TYPE);
    }

    public void pushInt(Integer i) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitIntInsn(BIPUSH, i);
        getContext().push(Type.INT_TYPE);
    }

    public void pushLong(Long l) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitLdcInsn(l);
        getContext().push(Type.LONG_TYPE);
    }

    public void pushFloat(Float f) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitLdcInsn(f);
        getContext().push(Type.FLOAT_TYPE);
    }

    public void pushDouble(Double d) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitLdcInsn(d);
        getContext().push(Type.DOUBLE_TYPE);
    }

    public void pushString(String s) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitLdcInsn(s);
        getContext().push(Type.getType(String.class));
    }

    public void pushChar(Character c) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitLdcInsn(c);
        getContext().push(Type.CHAR_TYPE);
    }

    public void pushByte(Byte b) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitLdcInsn(b);
        getContext().push(Type.BYTE_TYPE);
    }

    public void pushShort(Short s) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        mv.visitLdcInsn(s);
        getContext().push(Type.SHORT_TYPE);
    }

    public void loadConstant(Object value) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        if (value instanceof Integer) {
            mv.visitLdcInsn(value);
            getContext().push(Type.INT_TYPE);
        } else if (value instanceof Long) {
            mv.visitLdcInsn(value);
            getContext().push(Type.LONG_TYPE);
        } else if (value instanceof Float) {
            mv.visitLdcInsn(value);
            getContext().push(Type.FLOAT_TYPE);
        } else if (value instanceof Double) {
            mv.visitLdcInsn(value);
            getContext().push(Type.DOUBLE_TYPE);
        } else if (value instanceof String) {
            mv.visitLdcInsn(value);
            getContext().push(Type.getType(String.class));
        } else if (value instanceof Character) {
            mv.visitLdcInsn(value);
            getContext().push(Type.CHAR_TYPE);
        } else if (value instanceof Byte) {
            mv.visitLdcInsn(value);
            getContext().push(Type.BYTE_TYPE);
        } else if (value instanceof Short) {
            mv.visitLdcInsn(value);
            getContext().push(Type.SHORT_TYPE);
        } else if (value instanceof Boolean) {
            mv.visitLdcInsn(value);
            getContext().push(Type.BOOLEAN_TYPE);
        } else {
            throw new RuntimeException("Unsupported constant type: " + value.getClass());
        }
    }

    public void smartCast(Type to) {
        var mv = pc.mv == null ? pc.rootInitMv : pc.mv;
        Type pop = getContext().pop();
        smartCast(pop, to);
        getContext().push(to);
    }
}
