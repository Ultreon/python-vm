package dev.ultreon.pythonc;

import dev.ultreon.pythonc.classes.JvmClass;
import dev.ultreon.pythonc.classes.LangClass;
import dev.ultreon.pythonc.expr.ConstantExpr;
import dev.ultreon.pythonc.expr.MemberExpression;
import dev.ultreon.pythonc.expr.PyExpression;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.*;

public class JvmWriter {
    public final PythonCompiler pc;
    private Location lastLocation = new Location();

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

    public LoopContext getLoopContext(Location location) {
        for (int i = pc.contextStack.size() - 1; i >= 0; i--) {
            Context context = pc.contextStack.get(i);
            if (context instanceof FunctionContext) {
                throw new CompilerException("Not inside a loop or loop is outside of function.", location);
            }
            if (context instanceof LoopContext) {
                return (LoopContext) context;
            }
        }
        throw new RuntimeException("No loop context found");
    }

    public dev.ultreon.pythonc.classes.JvmClass getClassSymbol(String className) {
        return pc.getClassSymbol(className);
    }

    private void invokeStatic(String owner, String name, String signature, boolean isInterface) {
        Context context = getContext();
        Type methodType = Type.getMethodType(signature);
        Type[] argumentTypes = methodType.getArgumentTypes();
        for (Type arg : argumentTypes) {
            Type pop = context.pop();
            getClassSymbol(pop.getClassName());
        }

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitMethodInsn(INVOKESTATIC, owner, name, signature, isInterface);
        Type returnType = methodType.getReturnType();
        if (returnType.equals(Type.VOID_TYPE)) return;
        context.push(returnType);
    }

    private void invokeSpecial(String owner, String name, String signature, boolean isInterface) {
        Context context = getContext();
        Type methodType = Type.getMethodType(signature);
        Type[] argumentTypes = methodType.getArgumentTypes();
        context.pop();
        for (Type arg : argumentTypes) {
            Type pop = context.pop();
            getClassSymbol(pop.getClassName());
        }
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitMethodInsn(INVOKESPECIAL, owner, name, signature, isInterface);
        if (methodType.getReturnType().getSort() != Type.VOID) {
            context.push(methodType.getReturnType());
        }
    }

    private void invokeSpecial(Type owner, String name, Type signature, boolean isInterface) {
        if (signature.getSort() != Type.METHOD) throw new IllegalStateException("Not a method signature!");

        Context context = getContext();
        Type[] argumentTypes = signature.getArgumentTypes();
        context.pop();
        for (Type arg : argumentTypes) {
            Type pop = context.pop();
            getClassSymbol(pop.getClassName());
        }
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitMethodInsn(INVOKESPECIAL, owner.getInternalName(), name, signature.getDescriptor(), isInterface);
        if (signature.getReturnType().getSort() != Type.VOID) {
            context.push(signature.getReturnType());
        }
    }

    @Deprecated
    private void newInstance(String owner, String name, String signature, boolean isInterface, Runnable paramInit) {
        Context context = getContext();
        Type methodType = Type.getMethodType(signature);
        Type[] argumentTypes = methodType.getArgumentTypes();
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        newObject(Type.getObjectType(owner));
        dup();
        paramInit.run();
        invokeSpecial(owner, "<init>", signature, isInterface);
    }

    public void newInstance(JvmClass owner, List<PyExpression> arguments) {
        Context context = getContext();
        Type methodType = Type.getMethodType("()V");
        Type[] argumentTypes = methodType.getArgumentTypes();
        List<PyExpression> args = new ArrayList<>();
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        dynamicCall(owner, arguments);
    }

    private void newObject(Type objectType) {
        Context context = getContext();
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitTypeInsn(NEW, objectType.getInternalName());
        context.push(objectType);
    }

    public void throwObject() {
        Context context = getContext();
        Type pop = context.pop();
        getClassSymbol(pop.getClassName());
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitInsn(ATHROW);
    }

    public void returnObject() {
        Context context = getContext();
        Type pop = context.pop();
        getClassSymbol(pop.getClassName());
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitInsn(ARETURN);
    }

    public void invokeVirtual(String owner, String name, String signature, boolean isInterface) {
        Context context = getContext();
        Type methodType = Type.getMethodType(signature);
        Type[] argumentTypes = methodType.getArgumentTypes();
        for (Type arg : argumentTypes) {
            Type pop = context.pop();
            getClassSymbol(pop.getClassName());
        }
        Type pop1 = context.pop();
        if (!PythonCompiler.isInstanceOf(pc, pop1, owner)) {
            throw new RuntimeException("Expected " + pop1.getInternalName() + " to be " + owner);
        }
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitMethodInsn(INVOKEVIRTUAL, owner, name, signature, isInterface);
        Type returnType = methodType.getReturnType();
        if (returnType.equals(Type.VOID_TYPE)) return;
        context.push(returnType);
    }

    public void invokeVirtual(Type owner, String name, Type signature, boolean isInterface) {
        if (signature.getSort() != Type.METHOD) throw new IllegalStateException("Not a method signature!");

        Context context = getContext();
        Type methodType = Type.getMethodType(signature);
        Type[] argumentTypes = methodType.getArgumentTypes();
        for (Type arg : argumentTypes) {
            Type pop = context.pop();
            getClassSymbol(pop.getClassName());
        }
        Type pop1 = context.pop();
        if (!PythonCompiler.isInstanceOf(pc, pop1, owner.getInternalName())) {
            throw new RuntimeException("Expected " + pop1.getInternalName() + " to be " + owner);
        }
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitMethodInsn(INVOKEVIRTUAL, owner.getInternalName(), name, signature.getDescriptor(), isInterface);
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

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitMethodInsn(INVOKEINTERFACE, owner, name, signature, isInterface);
        Type returnType = methodType.getReturnType();
        if (returnType.equals(Type.VOID_TYPE)) return;
        context.push(returnType);
    }

    private void invokeDynamic(String interaction, String signature, Object... values) {
        Context context = getContext();

        Type methodType = Type.getMethodType(signature);
        Type[] argumentTypes = methodType.getArgumentTypes();
        for (int i = argumentTypes.length - 1; i >= 0; i--) {
            Type arg = argumentTypes[i];
            Type pop = context.pop();

            if (arg.getSort() != pop.getSort()) {
                throw new RuntimeException("Expected " + pop.getClassName() + " to be " + arg.getClassName());
            }
        }

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitInvokeDynamicInsn(interaction, signature, new Handle(H_INVOKESTATIC, "org/python/_internal/DynamicCalls", "bootstrap", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;" + Arrays.stream(values).map(o -> switch (o) {
            case String s -> "Ljava/lang/String;";
            case Integer i -> "I";
            case Long l -> "J";
            case Double d -> "D";
            case Float f -> "F";
            case Boolean b -> "Z";
            case Character c -> "C";
            case Byte b -> "B";
            case Short s -> "S";
            default -> throw new AssertionError("Unexpected value: " + o);
        }).collect(Collectors.joining("")) + ")Ljava/lang/invoke/CallSite;", false), values);

        Type returnType = methodType.getReturnType();
        if (returnType.getSort() == Type.VOID) return;
        context.push(returnType);
    }

    private void magicInvokeDynamic(String interaction, String signature, Object... values) {
        Context context = getContext();

        Type methodType = Type.getMethodType(signature);

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitInvokeDynamicInsn(interaction, signature, new Handle(H_INVOKESTATIC, "org/python/_internal/DynamicCalls", "bootstrap", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;" + Arrays.stream(values).map(o -> switch (o) {
            case String s -> "Ljava/lang/String;";
            case Integer i -> "I";
            case Long l -> "J";
            case Double d -> "D";
            case Float f -> "F";
            case Boolean b -> "Z";
            case Character c -> "C";
            case Byte b -> "B";
            case Short s -> "S";
            default -> throw new AssertionError("Unexpected value: " + o);
        }).collect(Collectors.joining("")) + ")Ljava/lang/invoke/CallSite;", false), values);

        Type returnType = methodType.getReturnType();
        if (returnType.getSort() == Type.VOID) return;
        context.push(returnType);
    }

    private void hiddenInvokeDynamic(String interaction, String signature, Object... values) {
        Context context = getContext();

        Type methodType = Type.getMethodType(signature);

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitInvokeDynamicInsn(interaction, signature, new Handle(H_INVOKESTATIC, "org/python/_internal/DynamicCalls", "bootstrap", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;" + Arrays.stream(values).map(o -> switch (o) {
            case String s -> "Ljava/lang/String;";
            case Integer i -> "I";
            case Long l -> "J";
            case Double d -> "D";
            case Float f -> "F";
            case Boolean b -> "Z";
            case Character c -> "C";
            case Byte b -> "B";
            case Short s -> "S";
            default -> throw new AssertionError("Unexpected value: " + o);
        }).collect(Collectors.joining("")) + ")Ljava/lang/invoke/CallSite;", false), values);
    }

    public void dynamicCall(String name, String signature) {
        invokeDynamic(name, "(Ljava/lang/Object;[Ljava/lang/Object;Ljava/util/Map;)Ljava/lang/Object;", "__call__");
    }

    public void dynamicCall(PyExpression owner, String name, List<PyExpression> arguments) {
        owner.write(pc, this);
        createArgs(arguments);
        invokeDynamic(name, "(Ljava/lang/Object;[Ljava/lang/Object;Ljava/util/Map;)Ljava/lang/Object;", "__call__");
    }

    public void createArgs(List<PyExpression> arguments) {
        createArray(arguments, Type.getType(Object.class));
    }

    public void dynamicGetAttr(String name) {
        invokeDynamic(name, "(Ljava/lang/Object;)Ljava/lang/Object;", "__getattr__");
    }

    public void dynamicSetAttr(String name) {
        invokeDynamic(name, "(Ljava/lang/Object;Ljava/lang/Object;)V", "__setattr__");
    }

    public void dynamicDelAttr(String name) {
        invokeDynamic(name, "(Ljava/lang/Object;)V", "__delattr__");
    }

    public void dynamicGetItem(String name) {
        invokeDynamic(name, "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", "__getitem__");
    }

    public void dynamicSetItem(String name) {
        invokeDynamic(name, "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V", "__setitem__");
    }

    public void getStatic(String owner, String name, String descriptor) {
        Context context = getContext();
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        Type type = Type.getType(descriptor);
        pushClass(Type.getObjectType(owner));
        dynamicGetAttr(name);
        context.push(type);
    }

    public void putStatic(String owner, String name, String descriptor, PyExpression expr) {
        Context context = getContext();
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        Type ownerType = Type.getObjectType(owner);
        pushClass(ownerType);
        expr.write(pc, this);
        dynamicSetAttr(name);
    }

    private void pushClass(Type ownerType) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitLdcInsn(ownerType);
        getContext().push(Type.getType(java.lang.Class.class));
    }

    public void getField(String name, String descriptor) {
        Context context = getContext();
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        dynamicGetAttr(name);
        cast(Type.getType(descriptor));
    }

    public void putField(String owner, String name, String descriptor, PyExpression parent, PyExpression expr) {
        Context context = getContext();
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        parent.write(pc, this);
        expr.write(pc, this);
        dynamicSetAttr(name);
    }

    public void loadConstant(String text) {
        Context context = getContext();
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitLdcInsn(text);
        context.push(Type.getType(String.class));
    }

    public void loadConstant(int value) {
        Context context = getContext();
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitLdcInsn(value);
        context.push(Type.INT_TYPE);
    }

    public void loadConstant(long value) {
        Context context = getContext();
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitLdcInsn(value);
        context.push(Type.LONG_TYPE);
    }

    public void loadConstant(float value) {
        Context context = getContext();
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitLdcInsn(value);
        context.push(Type.FLOAT_TYPE);
    }

    public void loadConstant(double value) {
        Context context = getContext();
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitLdcInsn(value);
        context.push(Type.DOUBLE_TYPE);
    }

    public void loadConstant(char value) {
        Context context = getContext();
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitLdcInsn(value);
        context.push(Type.CHAR_TYPE);
    }

    public void loadConstant(boolean value) {
        Context context = getContext();
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitLdcInsn(value);
        context.push(Type.BOOLEAN_TYPE);
    }

    public void loadConstant(byte value) {
        Context context = getContext();
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitLdcInsn(value);
        context.push(Type.BYTE_TYPE);
    }

    public void loadConstant(short value) {
        Context context = getContext();
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitLdcInsn(value);
        context.push(Type.SHORT_TYPE);
    }

    public void loadClass(Type type) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitLdcInsn(type);
        getContext().push(Type.getType("Ljava/lang/Class;"));
    }

    public void storeInt(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitVarInsn(ISTORE, index);
        Type pop = getContext().pop();
        if (!pop.equals(Type.INT_TYPE)) {
            throw new RuntimeException("Expected int, got " + pop);
        }
    }

    public void loadInt(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitVarInsn(ILOAD, index);
        getContext().push(Type.INT_TYPE);
    }

    public void storeLong(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitVarInsn(LSTORE, index);
        Type pop = getContext().pop();
        if (!pop.equals(Type.LONG_TYPE)) {
            throw new RuntimeException("Expected long, got " + pop);
        }
    }

    public void loadLong(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitVarInsn(LLOAD, index);
        getContext().push(Type.LONG_TYPE);
    }

    public void storeFloat(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitVarInsn(FSTORE, index);
        Type pop = getContext().pop();
        if (!pop.equals(Type.FLOAT_TYPE)) {
            throw new RuntimeException("Expected float, got " + pop);
        }
    }

    public void loadFloat(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitVarInsn(FLOAD, index);
        getContext().push(Type.FLOAT_TYPE);
    }

    public void storeDouble(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitVarInsn(DSTORE, index);
        Type pop = getContext().pop();
        if (!pop.equals(Type.DOUBLE_TYPE)) {
            throw new RuntimeException("Expected double, got " + pop);
        }
    }

    public void loadDouble(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitVarInsn(DLOAD, index);
        getContext().push(Type.DOUBLE_TYPE);
    }

    public void storeChar(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitVarInsn(ISTORE, index);
        Type pop = getContext().pop();
        if (!pop.equals(Type.CHAR_TYPE)) {
            throw new RuntimeException("Expected char, got " + pop);
        }
    }

    public void loadChar(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitVarInsn(ILOAD, index);
        getContext().push(Type.CHAR_TYPE);
    }

    public void storeBoolean(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        cast(Type.BOOLEAN_TYPE);
        mv.visitVarInsn(ISTORE, index);
        Type pop = getContext().pop();
        if (!pop.equals(Type.BOOLEAN_TYPE)) {
            throw new RuntimeException("Expected boolean, got " + pop);
        }
    }

    public void loadBoolean(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitVarInsn(ILOAD, index);
        cast(Type.BOOLEAN_TYPE);
        getContext().push(Type.BOOLEAN_TYPE);
    }

    public void storeByte(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        cast(Type.BYTE_TYPE);
        mv.visitVarInsn(ISTORE, index);
        Type pop = getContext().pop();
        if (!pop.equals(Type.BYTE_TYPE)) {
            throw new RuntimeException("Expected byte, got " + pop);
        }
    }

    public void loadByte(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitVarInsn(ILOAD, index);
        cast(Type.BYTE_TYPE);
        getContext().push(Type.BYTE_TYPE);
    }

    public void storeShort(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        cast(Type.SHORT_TYPE);
        mv.visitVarInsn(ISTORE, index);
        Type pop = getContext().pop();
        if (!pop.equals(Type.SHORT_TYPE)) {
            throw new RuntimeException("Expected short, got " + pop);
        }
    }

    public void loadShort(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitVarInsn(ILOAD, index);
        cast(Type.SHORT_TYPE);
        getContext().push(Type.SHORT_TYPE);
    }

    public void storeObject(int index, Type type) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitVarInsn(ASTORE, index);
        getContext().pop();
    }

    public void loadObject(int index, Type type) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitVarInsn(ALOAD, index);
        getContext().push(Type.getType(Object.class));
        cast(type);
    }

    public void jump(Label endLabel) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitJumpInsn(Opcodes.GOTO, endLabel);
    }

    public void cast(Type to) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        Context context = getContext();
        Type from = context.peek();
        if (to.getSort() == Type.OBJECT) {
            if (!from.equals(to)) {
                if (from.equals(Type.BYTE_TYPE))
                    invokeStatic("java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
                if (from.equals(Type.SHORT_TYPE))
                    invokeStatic("java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
                if (from.equals(Type.CHAR_TYPE))
                    invokeStatic("java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
                if (from.equals(Type.INT_TYPE))
                    invokeStatic("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                if (from.equals(Type.LONG_TYPE))
                    invokeStatic("java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
                if (from.equals(Type.FLOAT_TYPE))
                    invokeStatic("java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
                if (from.equals(Type.DOUBLE_TYPE))
                    invokeStatic("java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                if (from.equals(Type.BOOLEAN_TYPE))
                    invokeStatic("java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
                context.pop();
                checkCast(to);
                context.push(to);
            }
        } else switch (to.getSort()) {
            case Type.BOOLEAN -> {
                context.pop();
                switch (from.getSort()) {
                    case Type.BOOLEAN, Type.INT -> {
                        // do nothing
                    }
                    case Type.BYTE -> mv.visitInsn(I2B);
                    case Type.SHORT -> mv.visitInsn(I2S);
                    case Type.CHAR -> mv.visitInsn(I2C);
                    case Type.LONG -> mv.visitInsn(L2I);
                    case Type.FLOAT -> mv.visitInsn(F2I);
                    case Type.DOUBLE -> mv.visitInsn(D2I);
                    case Type.OBJECT -> {
                        mv.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
                    }
                    default -> throw new RuntimeException("Unsupported cast to " + to);
                }

                // check if bool != 0 -> true else false
                Label trueLabel = new Label();
                Label falseLabel = new Label();
                mv.visitJumpInsn(Opcodes.IFNE, trueLabel);
                mv.visitInsn(Opcodes.ICONST_0);
                mv.visitJumpInsn(Opcodes.GOTO, falseLabel);
                mv.visitLabel(trueLabel);
                mv.visitInsn(Opcodes.ICONST_1);
                mv.visitLabel(falseLabel);

                context.push(Type.BOOLEAN_TYPE);
            }
            case Type.INT -> {
                context.pop();
                switch (from.getSort()) {
                    case Type.INT -> {
                        // do nothing
                    }
                    case Type.BYTE -> mv.visitInsn(I2B);
                    case Type.SHORT -> mv.visitInsn(I2S);
                    case Type.CHAR -> mv.visitInsn(I2C);
                    case Type.LONG -> mv.visitInsn(I2L);
                    case Type.FLOAT -> mv.visitInsn(F2I);
                    case Type.DOUBLE -> mv.visitInsn(D2I);
                    case Type.OBJECT -> {
                        mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
                    }
                    default -> throw new RuntimeException("Unsupported cast to " + to);
                }
                context.push(Type.INT_TYPE);
            }
            case Type.LONG -> {
                context.pop();
                switch (from.getSort()) {
                    case Type.INT -> mv.visitInsn(I2L);
                    case Type.BYTE -> mv.visitInsn(I2B);
                    case Type.SHORT -> mv.visitInsn(I2S);
                    case Type.CHAR -> mv.visitInsn(I2C);
                    case Type.LONG -> {
                        // do nothing
                    }
                    case Type.FLOAT -> mv.visitInsn(F2L);
                    case Type.DOUBLE -> mv.visitInsn(D2L);
                    case Type.OBJECT -> {
                        mv.visitTypeInsn(CHECKCAST, "java/lang/Long");
                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
                    }
                    default -> throw new RuntimeException("Unsupported cast to " + to);
                }
                context.push(Type.LONG_TYPE);
            }
            case Type.FLOAT -> {
                context.pop();
                switch (from.getSort()) {
                    case Type.INT, Type.CHAR, Type.SHORT, Type.BYTE -> mv.visitInsn(I2F);
                    case Type.LONG -> mv.visitInsn(L2F);
                    case Type.FLOAT -> {
                        // do nothing
                    }
                    case Type.DOUBLE -> mv.visitInsn(D2F);
                    case Type.OBJECT -> {
                        mv.visitTypeInsn(CHECKCAST, "java/lang/Float");
                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
                    }
                    default -> throw new RuntimeException("Unsupported cast to " + to);
                }
                context.push(Type.FLOAT_TYPE);
            }
            case Type.DOUBLE -> {
                context.pop();
                switch (from.getSort()) {
                    case Type.INT, Type.CHAR, Type.SHORT, Type.BYTE -> mv.visitInsn(I2D);
                    case Type.LONG -> mv.visitInsn(L2D);
                    case Type.FLOAT -> mv.visitInsn(F2D);
                    case Type.DOUBLE -> {
                        // do nothing
                    }
                    case Type.OBJECT -> {
                        mv.visitTypeInsn(CHECKCAST, "java/lang/Double");
                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
                    }
                    default -> throw new RuntimeException("Unsupported cast from " + from + " to " + to);
                }
                context.push(Type.DOUBLE_TYPE);
            }
            case Type.ARRAY -> {
                if (!from.equals(to)) {
                    if (from.getSort() != Type.ARRAY && from.getSort() != Type.OBJECT) {
                        throw new RuntimeException("Unsupported cast from " + from + " to " + to);
                    }
                }
            }
            default -> throw new RuntimeException("Unsupported cast to " + to);
        }
    }

    private void checkCast(Type to) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitTypeInsn(CHECKCAST, to.getInternalName());
    }

    public void dup() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitInsn(DUP);

        Context context = getContext();
        context.push(context.peek());
    }

    public void secretDup() {
        // Did anybody say secret dupe glitch? Oh wait...
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitInsn(DUP);
    }

    public void secretPop() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitInsn(POP);
    }

    public void pop() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitInsn(POP);

        Context context = getContext();
        context.pop();
    }

    public void swap() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitInsn(SWAP);

        Context context = getContext();
        Type t1 = context.pop();
        Type t2 = context.pop();
        context.push(t2);
        context.push(t1);
    }

    public void dup2() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitInsn(DUP2);

        Context context = getContext();
        context.push(context.peek());
        context.push(context.peek());
    }

    public void dup2_x1() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitInsn(DUP2_X1);

        Context context = getContext();
        Type t1 = context.pop();
        Type t2 = context.pop();
        context.push(t2);
        context.push(t1);
        context.push(t2);
    }

    public void dup2_x2() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
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
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitIntInsn(BIPUSH, i);

        getContext().push(Type.BYTE_TYPE);
    }

    public void pushStackShort(int i) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitIntInsn(SIPUSH, i);

        getContext().push(Type.SHORT_TYPE);
    }

    public void lineNumber(int line, Label label) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        if (mv == null) {
            return;
        }

        mv.visitLineNumber(line, label);
    }

    public void jumpIfEqual(Label label) {
        Context context = getContext();
        Type pop = context.pop();
        smartCast(pop, getContext().peek());
        context.pop(Type.BOOLEAN_TYPE);

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitJumpInsn(Opcodes.IFEQ, label);
    }

    public void smartCast(Type from, Type to) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
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
                    invokeStatic("java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                } else {
                    smartCast(from, boxType(to));
                }
            } else if (from == Type.FLOAT_TYPE && to.getInternalName().equals("java/lang/Float")) {
                invokeStatic("java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
            } else if (from == Type.LONG_TYPE && to.getInternalName().equals("java/lang/Long")) {
                invokeStatic("java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
            } else if (from == Type.INT_TYPE && to.getInternalName().equals("java/lang/Integer")) {
                invokeStatic("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
            } else if (from == Type.BOOLEAN_TYPE && to.getInternalName().equals("java/lang/Boolean")) {
                invokeStatic("java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
            } else if (from == Type.CHAR_TYPE && to.getInternalName().equals("java/lang/Character")) {
                invokeStatic("java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
            } else if (from == Type.BYTE_TYPE && to.getInternalName().equals("java/lang/Byte")) {
                invokeStatic("java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
            } else if (from == Type.SHORT_TYPE && to.getInternalName().equals("java/lang/Short")) {
                invokeStatic("java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
            } else if (!from.equals(to)) {
                if (PythonCompiler.classCache.require(pc, to).doesInherit(pc, PythonCompiler.classCache.require(pc, from))) {
                    return;
                }
                throw new RuntimeException("Cannot smart cast " + from.getClassName() + " to " + to.getClassName());
            }
        } else if (to.getSort() == Type.ARRAY) {
            if (!from.equals(to)) {
                throw new RuntimeException("Cannot smart cast " + from.getClassName() + " to " + to.getClassName());
            }
        } else {
            throw new RuntimeException("Cannot smart cast " + from.getClassName() + " to " + to.getClassName());
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
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        smartCast(type);
        if (type == Type.DOUBLE_TYPE) {
            invokeStatic("java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
        } else if (type == Type.FLOAT_TYPE) {
            invokeStatic("java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
        } else if (type == Type.LONG_TYPE) {
            invokeStatic("java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
        } else if (type == Type.INT_TYPE) {
            invokeStatic("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
        } else if (type == Type.BOOLEAN_TYPE) {
            invokeStatic("java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
        } else if (type == Type.CHAR_TYPE) {
            invokeStatic("java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
        } else if (type == Type.BYTE_TYPE) {
            invokeStatic("java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
        } else if (type == Type.SHORT_TYPE) {
            invokeStatic("java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
        }
    }

    public void unbox(Type type) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        Type pop = getContext().peek();
        cast(boxType(type));
        if (type == Type.DOUBLE_TYPE) {
            invokeVirtual("java/lang/Double", "doubleValue", "()D", false);
        } else if (type == Type.FLOAT_TYPE) {
            invokeVirtual("java/lang/Float", "floatValue", "()F", false);
        } else if (type == Type.LONG_TYPE) {
            invokeVirtual("java/lang/Long", "longValue", "()J", false);
        } else if (type == Type.INT_TYPE) {
            invokeVirtual("java/lang/Integer", "intValue", "()I", false);
        } else if (type == Type.BOOLEAN_TYPE) {
            invokeVirtual("java/lang/Boolean", "booleanValue", "()Z", false);
        } else if (type == Type.CHAR_TYPE) {
            invokeVirtual("java/lang/Character", "charValue", "()C", false);
        } else if (type == Type.BYTE_TYPE) {
            invokeVirtual("java/lang/Byte", "byteValue", "()B", false);
        } else if (type == Type.SHORT_TYPE) {
            invokeVirtual("java/lang/Short", "shortValue", "()S", false);
        }
    }

    public void jumpIfNotEqual(Label label) {
        Context context = getContext();
        Type pop = context.pop();

        smartCast(pop, getContext().peek());
        context.pop();

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitJumpInsn(Opcodes.IFNE, label);
    }

    public void jumpIfLessThan(Label label) {
        Context context = getContext();
        Type pop = context.pop();

        smartCast(pop, getContext().peek());
        context.pop();

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitJumpInsn(Opcodes.IFLT, label);
    }

    public void jumpIfLessThanOrEqual(Label label) {
        Context context = getContext();
        Type pop = context.pop();

        smartCast(pop, getContext().peek());
        context.pop();

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitJumpInsn(Opcodes.IFLE, label);
    }

    public void jumpIfGreaterThan(Label label) {
        Context context = getContext();
        Type pop = context.pop();

        smartCast(pop, getContext().peek());
        context.pop();

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitJumpInsn(Opcodes.IFGT, label);
    }

    public void jumpIfGreaterThanOrEqual(Label label) {
        Context context = getContext();
        Type pop = context.pop();

        smartCast(pop, getContext().peek());
        context.pop();

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitJumpInsn(Opcodes.IFGE, label);
    }

    public void localVariable(String name, String desc, String signature, int line, Label start, Label end, int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitLocalVariable(name, desc, signature, start, end, index);
    }

    public void end() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    public void newArray(Type type) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        Context context = getContext();
        Type pop = context.pop();
        if (pop != Type.INT_TYPE && pop != Type.SHORT_TYPE && pop != Type.BYTE_TYPE)
            throw new RuntimeException("Expected stack int, got " + pop);
        mv.visitTypeInsn(ANEWARRAY, type.getInternalName());

        context.push(Type.getType("[" + type.getDescriptor()));
    }

    public void arrayStoreObject(Type type) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        cast(type);
        mv.visitInsn(AASTORE);

        Context context = getContext();
        context.pop();
        context.pop();
        context.pop();
    }

    public void label(Label label) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitLabel(label);
    }

    public void returnVoid() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitInsn(RETURN);
    }

    public void returnInt() {
        Context context = getContext();
        context.pop(Type.INT_TYPE);

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitInsn(IRETURN);
    }

    public void returnShort() {
        Context context = getContext();
        context.pop(Type.SHORT_TYPE);

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitInsn(IRETURN);
    }

    public void returnByte() {
        Context context = getContext();
        context.pop(Type.BYTE_TYPE);

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitInsn(IRETURN);
    }

    public void returnChar() {
        Context context = getContext();
        context.pop(Type.CHAR_TYPE);

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitInsn(IRETURN);
    }

    public void returnBoolean() {
        Context context = getContext();
        context.pop(Type.BOOLEAN_TYPE);

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitInsn(IRETURN);
    }

    public void returnFloat() {
        Context context = getContext();
        context.pop(Type.FLOAT_TYPE);

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitInsn(FRETURN);
    }

    public void returnDouble() {
        Context context = getContext();
        context.pop(Type.DOUBLE_TYPE);

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitInsn(DRETURN);
    }

    public void returnLong() {
        Context context = getContext();
        context.pop(Type.LONG_TYPE);

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitInsn(LRETURN);
    }

    public void returnFloatValues() {
        Context context = getContext();
        context.pop(Type.FLOAT_TYPE);

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitInsn(FRETURN);
    }

    public void pushZeroInt() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitInsn(ICONST_0);
        getContext().push(Type.INT_TYPE);
    }

    public void pushOneInt() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitInsn(ICONST_1);
        getContext().push(Type.INT_TYPE);
    }

    public void pushFalse() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitInsn(ICONST_0);
        getContext().push(Type.BOOLEAN_TYPE);
    }

    public void pushTrue() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitInsn(ICONST_1);
        getContext().push(Type.BOOLEAN_TYPE);
    }

    public void pushBoolean(Boolean b) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitInsn(b ? ICONST_1 : ICONST_0);
        getContext().push(Type.BOOLEAN_TYPE);
    }

    public void pushInt(Integer i) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitIntInsn(BIPUSH, i);
        getContext().push(Type.INT_TYPE);
    }

    public void pushLong(Long l) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitLdcInsn(l);
        getContext().push(Type.LONG_TYPE);
    }

    public void pushFloat(Float f) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitLdcInsn(f);
        getContext().push(Type.FLOAT_TYPE);
    }

    public void pushDouble(Double d) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitLdcInsn(d);
        getContext().push(Type.DOUBLE_TYPE);
    }

    public void pushString(String s) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitLdcInsn(s);
        getContext().push(Type.getType(String.class));
    }

    public void pushChar(Character c) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitLdcInsn(c);
        getContext().push(Type.CHAR_TYPE);
    }

    public void pushByte(Byte b) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitLdcInsn(b);
        getContext().push(Type.BYTE_TYPE);
    }

    public void pushShort(Short s) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitLdcInsn(s);
        getContext().push(Type.SHORT_TYPE);
    }

    public void loadConstant(Object value) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        switch (value) {
            case Integer i -> {
                mv.visitLdcInsn(value);
                getContext().push(Type.INT_TYPE);
            }
            case Long l -> {
                mv.visitLdcInsn(value);
                getContext().push(Type.LONG_TYPE);
            }
            case Float v -> {
                mv.visitLdcInsn(value);
                getContext().push(Type.FLOAT_TYPE);
            }
            case Double v -> {
                mv.visitLdcInsn(value);
                getContext().push(Type.DOUBLE_TYPE);
            }
            case String s -> {
                mv.visitLdcInsn(value);
                getContext().push(Type.getType(String.class));
            }
            case Character c -> {
                mv.visitLdcInsn(value);
                getContext().push(Type.CHAR_TYPE);
            }
            case Byte b -> {
                mv.visitLdcInsn(value);
                getContext().push(Type.BYTE_TYPE);
            }
            case Short i -> {
                mv.visitLdcInsn(value);
                getContext().push(Type.SHORT_TYPE);
            }
            case Boolean b -> {
                mv.visitLdcInsn(value);
                getContext().push(Type.BOOLEAN_TYPE);
            }
            case null, default -> {
                if (value != null) {
                    throw new RuntimeException("Unsupported constant owner: " + value.getClass());
                }
            }
        }
    }

    public void smartCast(Type to) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        Type pop = getContext().peek();
        if ((pop.getSort() != Type.OBJECT && pop.getSort() != Type.ARRAY) || (to.getSort() != Type.OBJECT && to.getSort() != Type.ARRAY))
            smartCast(pop, to);
        else cast(to);
        getContext().pop(pop);
        getContext().push(to);
    }

    public void loadThis(JvmClass type) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitVarInsn(ALOAD, 0);
        getContext().push(type.type());
    }

    public void pushNull() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitInsn(ACONST_NULL);
        getContext().push(Type.getType(Object.class));
    }

    public void createArray(List<PyExpression> arguments, Type type) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        pushStackByte(arguments.size());
        newArray(type);
        for (int i = 0, argumentsSize = arguments.size(); i < argumentsSize; i++) {
            PyExpression argument = arguments.get(i);
            dup();
            pushStackByte(i);
            argument.write(pc, this);
            if (getContext().peek().getSort() != Type.OBJECT && argument.type().getSort() != Type.ARRAY) {
                box(argument.type());
            }
            if (!getContext().peek().equals(type)) {
                cast(type);
            }

            arrayStoreObject(type);
        }
    }

    public void dynamicBuiltinCall(String name) {
        invokeDynamic(name, "([Ljava/lang/Object;Ljava/util/Map;)Ljava/lang/Object;", "__builtincall__");
    }

    public void dynamicBuiltinGetAttr(String name) {
        invokeDynamic(name, "()Ljava/lang/Object;", "__builtingetattr__");
    }

    public void dynamicBuiltinSetAttr(String name) {
        invokeDynamic(name, "(Ljava/lang/Object;)V", "__builtinsetattr__");
    }

    public void dynamicBuiltinDelAttr(String name) {
        invokeDynamic(name, "()V", "__builtindelattr__");
    }

    public void dynamicBuiltinHasAttr(String name) {
        invokeDynamic(name, "()Z", "__builtinhasattr__");
    }

    public void dynamicAdd() {
        invokeDynamic("__add__", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    }

    public void dynamicSub() {
        invokeDynamic("__sub__", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    }

    public void dynamicMul() {
        invokeDynamic("__mul__", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    }

    public void dynamicDiv() {
        invokeDynamic("__div__", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    }

    public void dynamicMod() {
        invokeDynamic("__mod__", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    }

    public void dynamicPow() {
        invokeDynamic("__pow__", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    }

    public void dynamicFloorDiv() {
        invokeDynamic("__floordiv__", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    }

    public void dynamicAnd() {
        invokeDynamic("__and__", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    }

    public void dynamicOr() {
        invokeDynamic("__or__", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    }

    public void dynamicXor() {
        invokeDynamic("__xor__", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    }

    public void dynamicLShift() {
        invokeDynamic("__lshift__", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    }

    public void dynamicRShift() {
        invokeDynamic("__rshift__", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    }

    public void dynamicNot() {
        invokeDynamic("__not__", "(Ljava/lang/Object;)Ljava/lang/Object;");
    }

    public void dynamicNeg() {
        invokeDynamic("__neg__", "(Ljava/lang/Object;)Ljava/lang/Object;");
    }

    public void dynamicPos() {
        invokeDynamic("__pos__", "(Ljava/lang/Object;)Ljava/lang/Object;");
    }

    public void dynamicIn() {
        invokeDynamic("__contains__", "(Ljava/lang/Object;Ljava/lang/Object;)Z");
    }

    public void dynamicNotIn() {
        invokeDynamic("__contains__", "(Ljava/lang/Object;Ljava/lang/Object;)Z");
        invokeDynamic("__not__", "(Ljava/lang/Object;)Ljava/lang/Object;");
    }

    public void dynamicIs() {
        invokeDynamic("__is__", "(Ljava/lang/Object;Ljava/lang/Object;)Z");
    }

    public void dynamicIsNot() {
        invokeDynamic("__isnot__", "(Ljava/lang/Object;Ljava/lang/Object;)Z");
    }

    public void dynamicStr() {
        invokeDynamic("__str__", "(Ljava/lang/Object;)Ljava/lang/String;");
    }

    public void dynamicRepr() {
        invokeDynamic("__repr__", "(Ljava/lang/Object;)Ljava/lang/String;");
    }

    public void dynamicBool() {
        invokeDynamic("__bool__", "(Ljava/lang/Object;)Z");
    }

    public void dynamicLen() {
        invokeDynamic("__len__", "(Ljava/lang/Object;)I");
    }

    public void dynamicIter() {
        invokeDynamic("__iter__", "(Ljava/lang/Object;)Ljava/lang/Object;");
    }


    public void secretIter() {
        hiddenInvokeDynamic("__iter__", "(Ljava/lang/Object;)Ljava/lang/Object;");
    }

    public void dynamicNext() {
        invokeDynamic("__next__", "(Ljava/lang/Object;)Ljava/lang/Object;");
    }

    public void magicNext() {
        magicInvokeDynamic("__next__", "(Ljava/lang/Object;)Ljava/lang/Object;");
    }

    public void magicHasNext() {
        magicInvokeDynamic("__hasnext__", "(Ljava/lang/Object;)Z");
    }

    public void dynamicGetItem() {
        invokeDynamic("__getitem__", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    }

    public void dynamicSetItem() {
        invokeDynamic("__setitem__", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V");
    }

    public void dynamicDelItem() {
        invokeDynamic("__delitem__", "(Ljava/lang/Object;Ljava/lang/Object;)V");
    }

    public void dynamicEq() {
        invokeDynamic("__eq__", "(Ljava/lang/Object;Ljava/lang/Object;)Z");
    }

    public void dynamicNe() {
        invokeDynamic("__ne__", "(Ljava/lang/Object;Ljava/lang/Object;)Z");
    }

    public void dynamicLt() {
        invokeDynamic("__lt__", "(Ljava/lang/Object;Ljava/lang/Object;)Z");
    }

    public void dynamicLe() {
        invokeDynamic("__le__", "(Ljava/lang/Object;Ljava/lang/Object;)Z");
    }

    public void dynamicGt() {
        invokeDynamic("__gt__", "(Ljava/lang/Object;Ljava/lang/Object;)Z");
    }

    public void dynamicGe() {
        invokeDynamic("__ge__", "(Ljava/lang/Object;Ljava/lang/Object;)Z");
    }

    public void dynamicCall() {
        invokeDynamic("__call__", "(Ljava/lang/Object;[Ljava/lang/Object;Ljava/util/Map;)Ljava/lang/Object;");
    }

    public void dynamicCall(String name) {
        invokeDynamic(name, "(Ljava/lang/Object;[Ljava/lang/Object;Ljava/util/Map;)Ljava/lang/Object;", "__call__");
    }

    public void magicDynamicCall(String name) {
        magicInvokeDynamic(name, "(Ljava/lang/Object;[Ljava/lang/Object;Ljava/util/Map;)Ljava/lang/Object;", "__call__");
    }

    public void hiddenDynamicCall(String name) {
        hiddenInvokeDynamic(name, "(Ljava/lang/Object;[Ljava/lang/Object;Ljava/util/Map;)Ljava/lang/Object;", "__call__");
    }

    public void dynamicCall(PyExpression owner, List<PyExpression> arguments) {
        if (owner instanceof MemberExpression memberExpression) memberExpression.writeAttrOnly(pc, this);
        else owner.write(pc, this);
        createArray(arguments, Type.getType(Object.class));
        createKwargs();
        dynamicCall();
    }

    public void returnValue(Type type, Location location) {
        Context context = getContext();

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        if (type.equals(Type.VOID_TYPE)) {
            if (context.needsPop())
                pop();
            mv.visitInsn(RETURN);
            return;
        }

        if (!context.needsPop()) {
            throw new CompilerException("No value to return", location);
        }

        Type boxedType = boxType(type);
        cast(boxedType);
        if (!boxedType.equals(type)) unbox(type);
        context.pop();

        switch (type.getSort()) {
            case Type.BYTE, Type.SHORT, Type.INT, Type.CHAR, Type.BOOLEAN -> mv.visitInsn(IRETURN);
            case Type.LONG -> mv.visitInsn(LRETURN);
            case Type.FLOAT -> mv.visitInsn(FRETURN);
            case Type.DOUBLE -> mv.visitInsn(DRETURN);
            case Type.OBJECT -> mv.visitInsn(ARETURN);
            default -> throw new RuntimeException("Unknown return type: " + type);
        }
    }

    public void loadClass(dev.ultreon.pythonc.classes.JvmClass jvmClass) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        Type type = jvmClass.type();
        mv.visitLdcInsn(type);
        Context context = getContext();
        context.push(Type.getType(java.lang.Class.class));
    }

    public void createKwargs() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitMethodInsn(INVOKESTATIC, "java/util/Collections", "emptyMap", "()Ljava/util/Map;", false);
        Context context = getContext();
        context.push(Type.getType(Map.class));
    }

    public void writeArgs(List<PyExpression> arguments, Type[] types) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        for (int i = 0, argumentsSize = arguments.size(); i < argumentsSize; i++) {
            PyExpression argument = arguments.get(i);
            argument.write(pc, this);
            if (types[i] == null || types[i].getSort() == Type.OBJECT) {
                if (argument.type().getSort() != Type.ARRAY && argument.type().getSort() != Type.OBJECT) {
                    pc.writer.box(argument.type());
                }
            }

            if (types[i] != null) {
                if (types[i].getSort() != Type.OBJECT && types[i].getSort() != Type.ARRAY) {
                    pc.writer.unbox(types[i]);
                }
                pc.writer.cast(types[i]);
            } else {
                pc.writer.cast(Type.getType(Object.class));
            }
        }
    }

    public void loadValue(int index, Type type) {
        switch (type.getSort()) {
            case Type.BYTE, Type.SHORT, Type.INT, Type.CHAR, Type.BOOLEAN -> {
                var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
                mv.visitVarInsn(ILOAD, index);
                getContext().push(type);
            }
            case Type.LONG -> {
                var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
                mv.visitVarInsn(LLOAD, index);
                getContext().push(type);
            }
            case Type.FLOAT -> {
                var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
                mv.visitVarInsn(FLOAD, index);
                getContext().push(type);
            }
            case Type.DOUBLE -> {
                var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
                mv.visitVarInsn(DLOAD, index);
                getContext().push(type);
            }
            case Type.OBJECT, Type.ARRAY -> {
                var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
                mv.visitVarInsn(ALOAD, index);
                getContext().push(type);
            }
            default -> throw new RuntimeException("Unknown type: " + type);
        }
    }

    public void parameter(String name) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitParameter(name, 0);
        getContext().push(Type.getType(Object.class));
    }

    public void loadThis(LangClass pyClass) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut;
        mv.visitVarInsn(ALOAD, 0);
        getContext().push(pyClass.type());
    }

    public MethodVisitor mv() {
        if (pc.methodOut == null) {
            if (pc.rootInitMv == null) {
                throw new AssertionError("Outside a method definition!");
            }
            return pc.rootInitMv;
        }
        return pc.methodOut;
    }

    public void hiddenDup() {
        mv().visitInsn(DUP);
    }

    public void createKwargs(Map<String, PyExpression> kwargs) {
        List<PyExpression> expressions = new ArrayList<>();
        newObject(Type.getType(HashMap.class));
        dup();
        invokeSpecial(Type.getType(HashMap.class), "<init>", Type.getMethodType(Type.VOID_TYPE), false);

        List<Map.@NotNull Entry<String, PyExpression>> copyOf = List.copyOf(kwargs.entrySet());
        for (int i = 0, copyOfSize = copyOf.size(); i < copyOfSize; i++) {
            Map.Entry<String, PyExpression> entry = copyOf.get(i);
            String s = entry.getKey();
            PyExpression expression = entry.getValue();
            if (i < copyOfSize - 1)
                dup();
            expressions.add(new ConstantExpr(s, expression.location()));
            expressions.add(expression);

            invokeVirtual(Type.getType(HashMap.class), "put", Type.getMethodType(Type.getType(Object.class), Type.getType(Object.class), Type.getType(Object.class)), false);
        }
    }

    public Location lastLocation() {
        return lastLocation;
    }

    public void lastLocation(Location location) {
        if (location == null) return;
        if (location.isUnknown()) return;
        this.lastLocation = location;
    }
}
