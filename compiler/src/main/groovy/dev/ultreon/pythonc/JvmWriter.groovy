//file:noinspection unused
//file:noinspection GroovyFallthrough
package dev.ultreon.pythonc

import dev.ultreon.pythonc.Context
import dev.ultreon.pythonc.classes.JvmClass
import dev.ultreon.pythonc.classes.PyClass
import dev.ultreon.pythonc.expr.ConstantExpr
import dev.ultreon.pythonc.expr.MemberExpression
import dev.ultreon.pythonc.expr.PyExpression
import dev.ultreon.pythonc.lang.PyAST
import org.jetbrains.annotations.Nullable
import org.objectweb.asm.*

import java.util.stream.Collectors

import static org.objectweb.asm.Opcodes.*
import static org.objectweb.asm.Type.*
import static org.objectweb.asm.Type.getMethodType as methodType

class JvmWriter {
    public final PythonCompiler pc
    def lastLocation = new Location()
    Label lastLabel

    JvmWriter(PythonCompiler pythonCompiler) {
        this.pc = pythonCompiler
    }


    Context getContext() {
        Context context = pc.context Context.class
        if (context == null) {
            throw new RuntimeException("No context found")
        }
        return context
    }

    LoopContext getLoopContext(Location location) {
        for (int i = pc.contextStack.size() - 1; i >= 0; i--) {
            Context context = pc.contextStack.get i
            if (context instanceof FunctionContext) {
                throw new CompilerException("Not inside a loop or loop is outside of function.", location)
            }
            if (context instanceof LoopContext) {
                return context as LoopContext
            }
        }
        throw new RuntimeException("No loop context found")
    }

    JvmClass classSymbol(String className) {
        return pc.getClassSymbol(className)
    }

    def invokeStatic(String owner, String name, String signature, boolean isInterface) {
        Context context = context
        Type methodType = methodType(signature)
        Type[] argumentTypes = methodType.argumentTypes
        for (Type arg : argumentTypes) {
            Type pop = context.pop()
            classSymbol(pop.className)
        }

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitMethodInsn INVOKESTATIC, owner, name, signature, isInterface
        Type returnType = methodType.returnType
        if (returnType == VOID_TYPE) return
        context.push(returnType)
    }

    def invokeSpecial(String owner, String name, String signature, boolean isInterface) {
        def context = context
        def methodType = methodType signature
        def argumentTypes = methodType.argumentTypes
        context.pop()
        for (Type arg : argumentTypes) {
            Type pop = context.pop()
            classSymbol(pop.className)
        }
        def mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitMethodInsn INVOKESPECIAL, owner, name, signature, isInterface
        if (methodType.returnType.sort != VOID) {
            context.push(methodType.returnType)
        }
    }

    def invokeSpecial(Type owner, String name, Type signature, boolean isInterface) {
        if (signature.sort != METHOD) throw new IllegalStateException("Not a method signature!")

        def context = context
        def argumentTypes = signature.argumentTypes
        context.pop()
        for (Type arg : argumentTypes) {
            def pop = context.pop()
            classSymbol pop.className
        }
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitMethodInsn INVOKESPECIAL, owner.internalName, name, signature.descriptor, isInterface
        if (signature.returnType.sort != VOID) {
            context.push signature.returnType
        }
    }

    @Deprecated
    def newInstance(String owner, String name, String signature, boolean isInterface, Runnable paramInit) {
        newObject getObjectType(owner)
        dup()
        paramInit.run()
        invokeSpecial owner, "<init>", signature, isInterface
    }

    def newInstance(JvmClass owner, List<PyExpression> arguments) {
        dynamicCall(owner, arguments)
    }

    def newObject(Type objectType) {
        def context = context
        def mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitTypeInsn NEW, objectType.internalName
        context.push objectType
    }

    def throwObject() {
        def context = context
        def pop = context.pop()
        classSymbol pop.className
        def mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn ATHROW
    }

    def returnObject() {
        def context = context
        def pop = context.pop()
        classSymbol pop.className
        def mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn ARETURN
    }

    def invokeVirtual(String owner, String name, String signature, boolean isInterface) {
        def context = context
        def methodType = methodType(signature)
        def argumentTypes = methodType.argumentTypes
        for (def arg : argumentTypes) {
            def pop = context.pop()
            classSymbol pop.className
        }
        def pop1 = context.pop()
        if (!PythonCompiler.isInstanceOf(pc, pop1, owner)) {
            throw new RuntimeException("Expected " + pop1.internalName + " to be " + owner)
        }
        def mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitMethodInsn INVOKEVIRTUAL, owner, name, signature, isInterface
        Type returnType = methodType.returnType
        if (returnType == VOID_TYPE) return
        context.push returnType
    }

    def invokeVirtual(Type owner, String name, Type signature, boolean isInterface) {
        if (signature.sort != METHOD) throw new IllegalStateException("Not a method signature!")

        def context = context
        def methodType = signature
        def argumentTypes = methodType.argumentTypes
        for (def arg : argumentTypes) {
            def pop = context.pop()
            classSymbol pop.className
        }
        def pop1 = context.pop()
        if (!PythonCompiler.isInstanceOf(pc, pop1, owner.internalName)) {
            throw new RuntimeException("Expected " + pop1.internalName + " to be " + owner)
        }
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitMethodInsn INVOKEVIRTUAL, owner.internalName, name, signature.descriptor, isInterface
        Type returnType = methodType.returnType
        if (returnType == VOID_TYPE) return
        context.push returnType
    }

    def invokeInterface(String owner, String name, String signature, boolean isInterface) {
        def context = context
        def methodType = methodType signature
        def argumentTypes = methodType.argumentTypes
        context.pop()
        for (def arg : argumentTypes) {
            def pop = context.pop()
            classSymbol pop.className
        }

        def mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitMethodInsn INVOKEINTERFACE, owner, name, signature, isInterface
        def returnType = methodType.returnType
        if (returnType == VOID_TYPE) return
        context.push returnType
    }

    def invokeDynamic(String interaction, String signature, Object... values) {
        def context = context

        def methodType = methodType(signature)
        def argumentTypes = methodType.argumentTypes
        for (def i = argumentTypes.length - 1; i >= 0; i--) {
            def arg = argumentTypes[i]
            def pop = context.pop()

            if (arg.sort != pop.sort) {
                throw new RuntimeException("Expected " + pop.className + " to be " + arg.className)
            }
        }

        def mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        if (interaction == null) throw new IllegalStateException()
        mv.visitInvokeDynamicInsn interaction, signature, new Handle(H_INVOKESTATIC, "org/python/_internal/DynamicCalls", "bootstrap", "(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;" + Arrays.stream(values).map(o -> {
            String s1
            if (Objects.requireNonNull(o) instanceof String) {
                s1 = "Ljava/lang/String;"
            } else if (o instanceof Integer) {
                s1 = "I"
            } else if (o instanceof Long) {
                s1 = "J"
            } else if (o instanceof Double) {
                s1 = "D"
            } else if (o instanceof Float) {
                s1 = "F"
            } else if (o instanceof Boolean) {
                s1 = "Z"
            } else if (o instanceof Character) {
                s1 = "C"
            } else if (o instanceof Byte) {
                s1 = "B"
            } else if (o instanceof Short) {
                s1 = "S"
            } else {
                throw new IllegalStateException("Unexpected value: " + o)
            }
            return s1
        }).collect(Collectors.joining("")) + ")Ljava/lang/invoke/CallSite;", false), values

        Type returnType = methodType.returnType
        if (returnType.sort == VOID) return
        context.push(returnType)
    }

    def magicInvokeDynamic(String interaction, String signature, Object... values) {
        def context = context

        def methodType = methodType(signature)

        def mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInvokeDynamicInsn interaction, signature, new Handle(H_INVOKESTATIC, "org/python/_internal/DynamicCalls", "bootstrap", "(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;" + Arrays.stream(values).map(o -> {
            String s1
            if (o instanceof String) {
                s1 = "Ljava/lang/String;"
            } else if (o instanceof Integer) {
                s1 = "I"
            } else if (o instanceof Long) {
                s1 = "J"
            } else if (o instanceof Double) {
                s1 = "D"
            } else if (o instanceof Float) {
                s1 = "F"
            } else if (o instanceof Boolean) {
                s1 = "Z"
            } else if (o instanceof Character) {
                s1 = "C"
            } else if (o instanceof Byte) {
                s1 = "B"
            } else if (o instanceof Short) {
                s1 = "S"
            } else {
                throw new IllegalStateException("Unexpected value: " + o)
            }
            return s1
        }).collect(Collectors.joining("")) + ")Ljava/lang/invoke/CallSite;", false), values

        Type returnType = methodType.returnType
        if (returnType.sort == VOID) return
        context.push(returnType)
    }

    def hiddenInvokeDynamic(String interaction, String signature, Object... values) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInvokeDynamicInsn(interaction, signature, new Handle(H_INVOKESTATIC, "org/python/_internal/DynamicCalls", "bootstrap", "(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;" + Arrays.stream(values).map(o -> {
            String s1
            if (o instanceof String) {
                s1 = "Ljava/lang/String;"
            } else if (o instanceof Integer) {
                s1 = "I"
            } else if (o instanceof Long) {
                s1 = "J"
            } else if (o instanceof Double) {
                s1 = "D"
            } else if (o instanceof Float) {
                s1 = "F"
            } else if (o instanceof Boolean) {
                s1 = "Z"
            } else if (o instanceof Character) {
                s1 = "C"
            } else if (o instanceof Byte) {
                s1 = "B"
            } else if (o instanceof Short) {
                s1 = "S"
            } else {
                throw new IllegalStateException("Unexpected value: " + o)
            }
            return s1
        }).collect(Collectors.joining("")) + ")Ljava/lang/invoke/CallSite;", false), values)
    }

    def dynamicCall(String name, String signature) {
        invokeDynamic(name, "(Ljava/lang/Object;[Ljava/lang/Object;Ljava/util/Map;)Ljava/lang/Object;", "__call__")
    }

    def dynamicCall(PyExpression owner, String name, List<PyExpression> arguments) {
        owner.write(pc, this)
        createArgs(arguments)
        createKwargs()
        invokeDynamic(name, "(Ljava/lang/Object;[Ljava/lang/Object;Ljava/util/Map;)Ljava/lang/Object;", "__call__")
    }

    def createArgs(List<PyExpression> arguments) {
        createArray(arguments, getType(Object.class))
    }

    def dynamicGetAttr(String name) {
        invokeDynamic(name, "(Ljava/lang/Object;)Ljava/lang/Object;", "__getattr__")
    }

    def dynamicSetAttr(String name) {
        invokeDynamic(name, "(Ljava/lang/Object;Ljava/lang/Object;)V", "__setattr__")
    }

    def dynamicDelAttr(String name) {
        invokeDynamic(name, "(Ljava/lang/Object;)V", "__delattr__")
    }

    def dynamicGetItem(String name) {
        invokeDynamic(name, "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", "__getitem__")
    }

    def dynamicSetItem(String name) {
        invokeDynamic(name, "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V", "__setitem__")
    }

    def getStatic(String owner, String name, String descriptor) {
        Context context = context
        Type type = getType(descriptor)
        pushClass(getObjectType(owner))
        dynamicGetAttr(name)
        context.push(type)
    }

    def putStatic(String owner, String name, String descriptor, PyExpression expr) {
        Type ownerType = getObjectType(owner)
        pushClass(ownerType)
        expr.write(pc, this)
        dynamicSetAttr(name)
    }

    def pushClass(Type ownerType) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitLdcInsn(boxType(ownerType))
        context.push(getType(Class.class))
    }

    def getField(String name, String descriptor) {
        dynamicGetAttr(name)
        cast(getType(descriptor))
    }

    def putField(String owner, String name, String descriptor, PyExpression parent, PyExpression expr) {
        parent.write(pc, this)
        expr.write(pc, this)
        dynamicSetAttr(name)
    }

    def loadConstant(String text) {
        Context context = context
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitLdcInsn(text)
        context.push(getType(String.class))
    }

    def loadConstant(int value) {
        Context context = context
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitLdcInsn(value)
        context.push(INT_TYPE)
    }

    def loadConstant(long value) {
        Context context = context
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitLdcInsn(value)
        context.push(LONG_TYPE)
    }

    def loadConstant(float value) {
        Context context = context
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitLdcInsn(value)
        context.push(FLOAT_TYPE)
    }

    def loadConstant(double value) {
        Context context = context
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitLdcInsn(value)
        context.push(DOUBLE_TYPE)
    }

    def loadConstant(char value) {
        Context context = context
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitIntInsn(SIPUSH, value)
        context.push(CHAR_TYPE)
    }

    def loadConstant(boolean value) {
        Context context = context
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(value ? ICONST_1 : ICONST_0)
        context.push(BOOLEAN_TYPE)
    }

    def loadConstant(byte value) {
        Context context = context
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitIntInsn(BIPUSH, value)
        context.push(BYTE_TYPE)
    }

    def loadConstant(short value) {
        Context context = context
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitIntInsn(SIPUSH, value)
        context.push(SHORT_TYPE)
    }

    def loadClass(Type type) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitLdcInsn(boxType(type))
        context.push(getType(Class))
    }

    def storeInt(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitVarInsn(ISTORE, index)
        Type pop = context.pop()
        if (pop != INT_TYPE) {
            throw new RuntimeException("Expected int, got " + pop)
        }
    }

    def loadInt(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitVarInsn(ILOAD, index)
        context.push(INT_TYPE)
    }

    def storeLong(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitVarInsn(LSTORE, index)
        Type pop = context.pop()
        if (pop != LONG_TYPE) {
            throw new RuntimeException("Expected long, got " + pop)
        }
    }

    def loadLong(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitVarInsn(LLOAD, index)
        context.push(LONG_TYPE)
    }

    def storeFloat(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitVarInsn(FSTORE, index)
        Type pop = context.pop()
        if (pop != FLOAT_TYPE) {
            throw new RuntimeException("Expected float, got " + pop)
        }
    }

    def loadFloat(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitVarInsn(FLOAD, index)
        context.push(FLOAT_TYPE)
    }

    def storeDouble(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitVarInsn(DSTORE, index)
        Type pop = context.pop()
        if (pop != DOUBLE_TYPE) {
            throw new RuntimeException("Expected double, got " + pop)
        }
    }

    def loadDouble(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitVarInsn(DLOAD, index)
        context.push(DOUBLE_TYPE)
    }

    def storeChar(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitVarInsn(ISTORE, index)
        Type pop = context.pop()
        if (pop != CHAR_TYPE) {
            throw new RuntimeException("Expected char, got " + pop)
        }
    }

    def loadChar(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitVarInsn(ILOAD, index)
        context.push(CHAR_TYPE)
    }

    def storeBoolean(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        cast(BOOLEAN_TYPE)
        mv.visitVarInsn(ISTORE, index)
        Type pop = context.pop()
        if (pop != BOOLEAN_TYPE) {
            throw new RuntimeException("Expected boolean, got " + pop)
        }
    }

    def loadBoolean(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitVarInsn(ILOAD, index)
        cast(BOOLEAN_TYPE)
        context.push(BOOLEAN_TYPE)
    }

    def storeByte(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        cast(BYTE_TYPE)
        mv.visitVarInsn(ISTORE, index)
        Type pop = context.pop()
        if (pop != BYTE_TYPE) {
            throw new RuntimeException("Expected byte, got " + pop)
        }
    }

    def loadByte(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitVarInsn(ILOAD, index)
        cast(BYTE_TYPE)
        context.push(BYTE_TYPE)
    }

    def storeShort(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        cast(SHORT_TYPE)
        mv.visitVarInsn(ISTORE, index)
        Type pop = context.pop()
        if (pop != SHORT_TYPE) {
            throw new RuntimeException("Expected short, got " + pop)
        }
    }

    def loadShort(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitVarInsn(ILOAD, index)
        cast(SHORT_TYPE)
        context.push(SHORT_TYPE)
    }

    def storeObject(int index, Type type) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitVarInsn(ASTORE, index)
        context.pop()
    }

    def loadObject(int index, Type type) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitVarInsn(ALOAD, index)
        context.push(getType(Object.class))
        cast(type)
    }

    def jump(Label endLabel) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitJumpInsn(Opcodes.GOTO, endLabel)
    }

    def cast(Class clazz) {
        cast(getType(clazz))
    }

    def castArray() {
        invokeDynamic("castArray", "(Ljava/lang/Object;)[Ljava/lang/Object;")
    }

    def castArray(Type type) {
        loadClass(type)
        invokeDynamic("castArray", "(Ljava/lang/Object;Ljava/lang/Class;)[" + type.getDescriptor())
    }

    def castList() {
        invokeDynamic("castList", "(Ljava/lang/Object;)Ljava/util/List;", "castList")
    }

    def castCollection() {
        invokeDynamic("castCollection", "(Ljava/lang/Object;)Ljava/util/Collection;", "castCollection")
    }

    def castMap() {
        invokeDynamic("castMap", "(Ljava/lang/Object;)Ljava/util/Map;", "castMap")
    }

    def castSet() {
        invokeDynamic("castSet", "(Ljava/lang/Object;)Ljava/util/Set;", "castSet")
    }

    def cast(Type to) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        Context context = context
        Type from = context.peek()
        if (from == to)
            return

        if (to.sort == OBJECT) {
            if (from != to) {
                if (from == BYTE_TYPE)
                    invokeStatic("java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false)
                if (from == SHORT_TYPE)
                    invokeStatic("java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false)
                if (from == CHAR_TYPE)
                    invokeStatic("java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false)
                if (from == INT_TYPE)
                    invokeStatic("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false)
                if (from == LONG_TYPE)
                    invokeStatic("java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false)
                if (from == FLOAT_TYPE)
                    invokeStatic("java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false)
                if (from == DOUBLE_TYPE)
                    invokeStatic("java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false)
                if (from == BOOLEAN_TYPE)
                    invokeStatic("java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false)
                context.pop()
                checkCast(to)
                context.push(to)
            }
        } else switch (to.sort) {
            case BOOLEAN:
                context.pop()
                switch (from.sort) {
                    case BOOLEAN:
                    case INT:// do nothing
                        break
                    case BYTE:
                        mv.visitInsn(I2B)
                        break
                    case SHORT:
                        mv.visitInsn(I2S)
                        break
                    case CHAR:
                        mv.visitInsn(I2C)
                        break
                    case Type.LONG:
                        mv.visitInsn(L2I)
                        break
                    case Type.FLOAT:
                        mv.visitInsn(F2I)
                        break
                    case Type.DOUBLE:
                        mv.visitInsn(D2I)
                        break
                    case OBJECT:
                        mv.visitTypeInsn(CHECKCAST, "java/lang/Boolean")
                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false)
                        break
                    case ARRAY:
                        mv.visitTypeInsn(CHECKCAST, "java/lang/Object")
                        mv.visitTypeInsn(CHECKCAST, "java/lang/Boolean")
                    default:
                        throw new RuntimeException("Unsupported cast to " + to)
                }

                // check if bool != 0 -> true else false
                Label trueLabel = new Label()
                Label falseLabel = new Label()
                mv.visitJumpInsn(Opcodes.IFNE, trueLabel)
                mv.visitInsn(Opcodes.ICONST_0)
                mv.visitJumpInsn(Opcodes.GOTO, falseLabel)
                mv.visitLabel(trueLabel)
                mv.visitInsn(Opcodes.ICONST_1)
                mv.visitLabel(falseLabel)

                context.push(BOOLEAN_TYPE)
                break
            case BYTE:
                unboxObject(from, to)
                break
            case SHORT:
                unboxObject(from, to)
                break
            case CHAR:
                unboxObject(from, to)
                break
            case INT:
                unboxObject(from, to)
                break
            case Type.FLOAT:
                unboxObject(from, to)
                break
            case Type.LONG:
                unboxObject(from, to)
                break
            case Type.DOUBLE:
                unboxObject(from, to)
                break
            case ARRAY:
                context.pop()
                checkCast(to)
                context.push(to)
                break
            case OBJECT:
                context.pop()
                checkCast(to)
                context.push(to)
                break
            default:
                throw new RuntimeException("Unsupported cast to " + to)
        }
    }

    private void unboxObject(Type from, Type to) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut

        String owner
        String method
        String desc

        switch (to.sort) {
            case BOOLEAN:
                owner = "java/lang/Boolean"
                method = "booleanValue"
                desc = "()Z"
                break
            case BYTE:
                owner = "java/lang/Byte"
                method = "byteValue"
                desc = "()B"
                break
            case SHORT:
                owner = "java/lang/Short"
                method = "shortValue"
                desc = "()S"
                break
            case CHAR:
                owner = "java/lang/Character"
                method = "charValue"
                desc = "()C"
                break
            case INT:
                owner = "java/lang/Integer"
                method = "intValue"
                desc = "()I"
                break
            case Type.FLOAT:
                owner = "java/lang/Float"
                method = "floatValue"
                desc = "()F"
                break
            case Type.LONG:
                owner = "java/lang/Long"
                method = "longValue"
                desc = "()J"
                break
            case Type.DOUBLE:
                owner = "java/lang/Double"
                method = "doubleValue"
                desc = "()D"
                break
            default:
                throw new RuntimeException("Not a primitive type: " + to)
        }

        checkCast(getObjectType(owner))
        mv.visitMethodInsn(INVOKEVIRTUAL, owner, method, desc, false)
    }

    private void convertPrimitive(Type from, Type to) {
        if (from == to) return

        // Handle widening/narrowing conversions
        switch (to.sort) {
            case BOOLEAN:
                convertToBoolean(from)
                break
            case BYTE:
                convertToByte(from)
                break
            case SHORT:
                convertToShort(from)
                break
            case CHAR:
                convertToChar(from)
                break
            case INT:
                convertToInt(from)
                break
            case Type.FLOAT:
                convertToFloat(from)
                break
            case Type.LONG:
                convertToLong(from)
                break
            case Type.DOUBLE:
                convertToDouble(from)
                break
            default:
                throw new RuntimeException("Invalid primitive type: " + to)
        }
    }

    private void convertToBoolean(Type from) {
        if (isPrimitive(from)) {
            boxPrimitive(from)
        } else {
            unboxObject(from, BOOLEAN_TYPE)
        }
    }

    private void convertToByte(Type from) {
        if (isPrimitive(from)) {
            boxPrimitive(from)
        } else {
            unboxObject(from, BYTE_TYPE)
        }
    }

    private void convertToShort(Type from) {
        if (isPrimitive(from)) {
            boxPrimitive(from)
        } else {
            unboxObject(from, SHORT_TYPE)
        }
    }

    private void convertToChar(Type from) {
        if (isPrimitive(from)) {
            boxPrimitive(from)
        } else {
            unboxObject(from, CHAR_TYPE)
        }
    }

    private void convertToInt(Type from) {
        if (isPrimitive(from)) {
            boxPrimitive(from)
        } else {
            unboxObject(from, INT_TYPE)
        }
    }

    private void convertToFloat(Type from) {
        if (isPrimitive(from)) {
            boxPrimitive(from)
        } else {
            unboxObject(from, FLOAT_TYPE)
        }
    }

    private void convertToLong(Type from) {
        if (isPrimitive(from)) {
            boxPrimitive(from)
        } else {
            unboxObject(from, LONG_TYPE)
        }
    }

    private void convertToDouble(Type from) {
        if (isPrimitive(from)) {
            boxPrimitive(from)
        } else {
            unboxObject(from, DOUBLE_TYPE)
        }
    }

    private static boolean isPrimitive(Type type) {
        return type.sort >= BOOLEAN && type.sort <= Type.DOUBLE
    }

    private def checkCast(Type to) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitTypeInsn(CHECKCAST, to.internalName)
    }

    def dup() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(DUP)

        Context context = context
        context.push(context.peek())
    }

    def secretDup() {
        // Did anybody say secret dupe glitch? Oh wait...
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(DUP)
    }

    def secretPop() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(POP)
    }

    def pop() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(POP)

        Context context = context
        context.pop()
    }

    def swap() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(SWAP)

        Context context = context
        Type t1 = context.pop()
        if (t1.size == 2) {
            throw new RuntimeException("Cannot swap category-2 types")
        }
        if (!context.popNeeded) {
            throw new RuntimeException("Cannot swap a single value")
        }
        Type t2 = context.pop()

        if (t2.size == 2) {
            throw new RuntimeException("Cannot swap category-2 types")
        }

        context.push(t1)
        context.push(t2)
    }

    def dup2() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(DUP2)

        Context context = context
        Type t1 = context.pop()
        if (t1.size == 2) {
            context.push(t1)
            return
        }
        if (!context.popNeeded) {
            throw new RuntimeException("Needed a second value for dup2")
        }
        Type t2 = context.pop()
        context.push(t2)
        context.push(t1)
        context.push(t2)
        context.push(t1)
    }

    // Pop 2 values (or one category-2 value) from the stack
    def pop2() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(POP2)

        def pop = context.pop()
        if (pop.size == 1) {
            context.pop()
        }
    }

    // Duplicate a single value and insert beneath the top value on the stack
    def dup_x1() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(DUP_X1)

        Context context = context
        Type t1 = context.pop()
        if (t1.size == 2) {
            throw new RuntimeException("Cannot dup_x1 category-2 type")
        }
        Type t2 = context.pop()
        context.push(t1)
        context.push(t2)
        context.push(t1)
    }

    def dup_x2() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        Context context = context

        if (context.stackSize < 3) {
            throw new RuntimeException("Stack underflow - dup_x2 requires at least 3 values on stack")
        }

        Type t1 = context.pop()
        if (t1.size == 2) {
            // Form 2: category-2 value
            Type t2 = context.pop()
            if (t2.size == 2) {
                throw new RuntimeException("Cannot dup_x2 when second value is category-2")
            }

            mv.visitInsn(DUP2_X1)

            // Restore stack
            context.push(t2)
            context.push(t1)
            context.push(t1)
        } else {
            // Form 1: three category-1 values
            Type t2 = context.pop()
            if (t2.size == 2) {
                throw new RuntimeException("Cannot dup_x2 when second value is category-2")
            }
            Type t3 = context.pop()
            if (t3.size == 2) {
                throw new RuntimeException("Cannot dup_x2 when third value is category-2")
            }

            mv.visitInsn(DUP_X2)

            // Restore stack
            context.push(t3)
            context.push(t2)
            context.push(t1)
            context.push(t1)
        }
    }
    // No-operation instruction
    def nop() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(NOP)
    }

    def dup2_x1() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut

        Context context = context
        Type t1 = context.pop()

        if (t1.size == 2) {
            // Form 2: category-2 value
            Type t2 = context.pop()
            if (t2.size == 2) {
                throw new RuntimeException("Cannot dup2_x1 with second value being category-2")
            }

            mv.visitInsn(DUP2_X1)
            context.push(t2)
            context.push(t1)
            context.push(t1)
        } else {
            // Form 1: two category-1 values
            Type t2 = context.pop()
            if (t2.size == 2) {
                throw new RuntimeException("Cannot dup2_x1 with second value being category-2")
            }
            Type t3 = context.pop()
            if (t3.size == 2) {
                throw new RuntimeException("Cannot dup2_x1 with third value being category-2")
            }

            mv.visitInsn(DUP2_X1)
            context.push(t3)
            context.push(t2)
            context.push(t1)
            context.push(t2)
            context.push(t1)
        }
    }

    def dup2_x2() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut

        Context context = context
        Type t1 = context.pop()

        if (t1.size == 2) {
            // Form 2: category-2 value on top
            Type t2 = context.pop()
            if (t2.size == 2) {
                // Form 3: both values are category-2
                mv.visitInsn(DUP2_X2)
                context.push(t2)
                context.push(t1)
                context.push(t1)
            } else {
                Type t3 = context.pop()
                if (t3.size == 2) {
                    throw new RuntimeException("Invalid type combination for dup2_x2")
                }
                // Form 2: category-2 on top, two category-1 below
                mv.visitInsn(DUP2_X2)
                context.push(t3)
                context.push(t2)
                context.push(t1)
                context.push(t1)
            }
        } else {
            // Form 1 or 4: two category-1 values on top
            Type t2 = context.pop()
            if (t2.size == 2) {
                throw new RuntimeException("Invalid type combination for dup2_x2")
            }
            Type t3 = context.pop()
            Type t4 = context.pop()

            if (t3.size == 2 || t4.size == 2) {
                throw new RuntimeException("Invalid type combination for dup2_x2")
            }

            // Form 1: all category-1
            mv.visitInsn(DUP2_X2)
            context.push(t4)
            context.push(t3)
            context.push(t2)
            context.push(t1)
            context.push(t2)
            context.push(t1)
        }
    }
    // Monitor enter/exit for synchronized blocks
    def monitorEnter() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(MONITORENTER)
        context.pop() // removes the object reference
    }

    def monitorExit() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(MONITOREXIT)
        context.pop() // removes the object reference
    }

    def pushStackByte(int i) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitIntInsn(BIPUSH, i)

        context.push(BYTE_TYPE)
    }

    def pushStackShort(int i) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitIntInsn(SIPUSH, i)

        context.push(SHORT_TYPE)
    }

    def lineNumber(int line, Label label) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        if (mv == null) {
            return
        }

        mv.visitLineNumber(line, label)
    }

    def jumpIfEqual(Label label) {
        Context context = context

        cast(BOOLEAN_TYPE)
        context.pop()

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitJumpInsn(Opcodes.IFEQ, label)
    }

    def smartCast(Type from, Type to) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        if (to == DOUBLE_TYPE) {
            if (from == LONG_TYPE) {
                mv.visitInsn(L2D)
            } else if (from == FLOAT_TYPE) {
                mv.visitInsn(F2D)
            } else if (from == INT_TYPE) {
                mv.visitInsn(I2D)
            }
        } else if (to == FLOAT_TYPE) {
            if (from == DOUBLE_TYPE) {
                mv.visitInsn(D2F)
            } else if (from == LONG_TYPE) {
                mv.visitInsn(L2F)
            } else if (from == INT_TYPE) {
                mv.visitInsn(I2F)
            }
        } else if (to == LONG_TYPE) {
            if (from == DOUBLE_TYPE) {
                mv.visitInsn(D2L)
            } else if (from == FLOAT_TYPE) {
                mv.visitInsn(F2L)
            } else if (from == INT_TYPE) {
                mv.visitInsn(I2L)
            }
        } else if (to == INT_TYPE) {
            if (from == DOUBLE_TYPE) {
                mv.visitInsn(D2I)
            } else if (from == FLOAT_TYPE) {
                mv.visitInsn(F2I)
            } else if (from == LONG_TYPE) {
                mv.visitInsn(L2I)
            }
        } else if (to == BOOLEAN_TYPE) {
            if (from == DOUBLE_TYPE) {
                mv.visitInsn(D2I)
            } else if (from == FLOAT_TYPE) {
                mv.visitInsn(F2I)
            } else if (from == LONG_TYPE) {
                mv.visitInsn(L2I)
            }
        } else if (to == CHAR_TYPE) {
            if (from == DOUBLE_TYPE) {
                mv.visitInsn(D2I)
                mv.visitInsn(I2C)
            } else if (from == FLOAT_TYPE) {
                mv.visitInsn(F2I)
                mv.visitInsn(I2C)
            } else if (from == LONG_TYPE) {
                mv.visitInsn(L2I)
                mv.visitInsn(I2C)
            } else if (from == INT_TYPE) {
                mv.visitInsn(I2C)
            }
        } else if (to == SHORT_TYPE) {
            if (from == DOUBLE_TYPE) {
                mv.visitInsn(D2I)
                mv.visitInsn(I2S)
            } else if (from == FLOAT_TYPE) {
                mv.visitInsn(F2I)
                mv.visitInsn(I2S)
            } else if (from == LONG_TYPE) {
                mv.visitInsn(L2I)
                mv.visitInsn(I2S)
            } else if (from == INT_TYPE) {
                mv.visitInsn(I2S)
            }
        } else if (to == BYTE_TYPE) {
            if (from == DOUBLE_TYPE) {
                mv.visitInsn(D2I)
                mv.visitInsn(I2B)
            } else if (from == FLOAT_TYPE) {
                mv.visitInsn(F2I)
                mv.visitInsn(I2B)
            } else if (from == LONG_TYPE) {
                mv.visitInsn(L2I)
                mv.visitInsn(I2B)
            } else if (from == INT_TYPE) {
                mv.visitInsn(I2B)
            }
        } else if (to.sort == OBJECT) {
            if (from == DOUBLE_TYPE) {
                if (to.internalName == "java/lang/Double") {
                    invokeStatic("java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false)
                } else {
                    smartCast(from, boxType(to))
                }
            } else if (from == FLOAT_TYPE && to.internalName == "java/lang/Float") {
                invokeStatic("java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false)
            } else if (from == LONG_TYPE && to.internalName == "java/lang/Long") {
                invokeStatic("java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false)
            } else if (from == INT_TYPE && to.internalName == "java/lang/Integer") {
                invokeStatic("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false)
            } else if (from == BOOLEAN_TYPE && to.internalName == "java/lang/Boolean") {
                invokeStatic("java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false)
            } else if (from == CHAR_TYPE && to.internalName == "java/lang/Character") {
                invokeStatic("java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false)
            } else if (from == BYTE_TYPE && to.internalName == "java/lang/Byte") {
                invokeStatic("java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false)
            } else if (from == SHORT_TYPE && to.internalName == "java/lang/Short") {
                invokeStatic("java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false)
            } else if (from != to) {
                if (PythonCompiler.classCache.require(pc, to).doesInherit(pc, PythonCompiler.classCache.require(pc, from))) {
                    return
                }
                throw new RuntimeException("Cannot smart cast " + from.className + " to " + to.className)
            }
        } else if (to.sort == ARRAY) {
            if (from != to) {
                throw new RuntimeException("Cannot smart cast " + from.className + " to " + to.className)
            }
        } else {
            throw new RuntimeException("Cannot smart cast " + from.className + " to " + to.className)
        }
    }

    static Type boxType(Type from) {
        if (from == DOUBLE_TYPE) {
            return getType(Double.class)
        } else if (from == FLOAT_TYPE) {
            return getType(Float.class)
        } else if (from == LONG_TYPE) {
            return getType(Long.class)
        } else if (from == INT_TYPE) {
            return getType(Integer.class)
        } else if (from == BOOLEAN_TYPE) {
            return getType(Boolean.class)
        } else if (from == CHAR_TYPE) {
            return getType(Character.class)
        } else if (from == BYTE_TYPE) {
            return getType(Byte.class)
        } else if (from == SHORT_TYPE) {
            return getType(Short.class)
        } else {
            return from
        }
    }

    static Type unboxType(Type from) {
        if (from == getType(Double.class) || from == DOUBLE_TYPE) {
            return DOUBLE_TYPE
        } else if (from == getType(Float.class) || from == FLOAT_TYPE) {
            return FLOAT_TYPE
        } else if (from == getType(Long.class) || from == LONG_TYPE) {
            return LONG_TYPE
        } else if (from == getType(Integer.class) || from == INT_TYPE) {
            return INT_TYPE
        } else if (from == getType(Boolean.class) || from == BOOLEAN_TYPE) {
            return BOOLEAN_TYPE
        } else if (from == getType(Character.class) || from == CHAR_TYPE) {
            return CHAR_TYPE
        } else if (from == getType(Byte.class) || from == BYTE_TYPE) {
            return BYTE_TYPE
        } else if (from == getType(Short.class) || from == SHORT_TYPE) {
            return SHORT_TYPE
        } else {
            return from
        }
    }

    def box(Type type) {
        smartCast(type)
        if (type == DOUBLE_TYPE) {
            invokeStatic("java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false)
        } else if (type == FLOAT_TYPE) {
            invokeStatic("java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false)
        } else if (type == LONG_TYPE) {
            invokeStatic("java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false)
        } else if (type == INT_TYPE) {
            invokeStatic("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false)
        } else if (type == BOOLEAN_TYPE) {
            invokeStatic("java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false)
        } else if (type == CHAR_TYPE) {
            invokeStatic("java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false)
        } else if (type == BYTE_TYPE) {
            invokeStatic("java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false)
        } else if (type == SHORT_TYPE) {
            invokeStatic("java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false)
        }
    }

    def unbox(Type type) {
        cast(boxType(type))
        if (type == DOUBLE_TYPE) {
            invokeVirtual("java/lang/Double", "doubleValue", "()D", false)
        } else if (type == FLOAT_TYPE) {
            invokeVirtual("java/lang/Float", "floatValue", "()F", false)
        } else if (type == LONG_TYPE) {
            invokeVirtual("java/lang/Long", "longValue", "()J", false)
        } else if (type == INT_TYPE) {
            invokeVirtual("java/lang/Integer", "intValue", "()I", false)
        } else if (type == BOOLEAN_TYPE) {
            invokeVirtual("java/lang/Boolean", "booleanValue", "()Z", false)
        } else if (type == CHAR_TYPE) {
            invokeVirtual("java/lang/Character", "charValue", "()C", false)
        } else if (type == BYTE_TYPE) {
            invokeVirtual("java/lang/Byte", "byteValue", "()B", false)
        } else if (type == SHORT_TYPE) {
            invokeVirtual("java/lang/Short", "shortValue", "()S", false)
        }
    }

    def jumpIfNotEqual(Label label) {
        Context context = context

        cast(BOOLEAN_TYPE)
        context.pop()

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitJumpInsn(Opcodes.IFNE, label)
    }

    def magicIfNotEqual(Label label) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitJumpInsn(Opcodes.IFNE, label)
    }

    def jumpIfLessThan(Label label) {
        Context context = context

        cast(BOOLEAN_TYPE)
        context.pop()

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitJumpInsn(Opcodes.IFLT, label)
    }

    def jumpIfLessThanOrEqual(Label label) {
        Context context = context

        cast(BOOLEAN_TYPE)
        context.pop()

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitJumpInsn(Opcodes.IFLE, label)
    }

    def jumpIfGreaterThan(Label label) {
        Context context = context

        cast(BOOLEAN_TYPE)
        context.pop()

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitJumpInsn(Opcodes.IFGT, label)
    }

    def jumpIfGreaterThanOrEqual(Label label) {
        Context context = context

        cast(BOOLEAN_TYPE)
        context.pop()

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitJumpInsn(Opcodes.IFGE, label)
    }

    def localVariable(String name, String desc, String signature, int line, Label start, Label end, int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitLocalVariable(name, desc, signature, start, end, index)
    }

    def end() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitMaxs(0, 0)
        mv.visitEnd()
    }

    def newArray(Type type) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        Context context = context
        Type pop = context.pop()
        if (pop != INT_TYPE && pop != SHORT_TYPE && pop != BYTE_TYPE)
            throw new RuntimeException("Expected stack int, got " + pop)
        mv.visitTypeInsn(ANEWARRAY, type.internalName)

        context.push(getType("[" + type.descriptor))
    }

    def arrayStoreObject(Type type) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        cast(type)
        mv.visitInsn(AASTORE)

        Context context = context
        context.pop()
        context.pop()
        context.pop()
    }

    def label(Label label) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitLabel(label)
    }

    def returnVoid() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(RETURN)
    }

    def returnInt() {
        Context context = context
        context.pop(INT_TYPE)

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(IRETURN)
    }

    def returnShort() {
        Context context = context
        context.pop(SHORT_TYPE)

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(IRETURN)
    }

    def returnByte() {
        Context context = context
        context.pop(BYTE_TYPE)

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(IRETURN)
    }

    def returnChar() {
        Context context = context
        context.pop(CHAR_TYPE)

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn IRETURN
    }

    def returnBoolean() {
        Context context = context
        context.pop(BOOLEAN_TYPE)

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(IRETURN)
    }

    def returnFloat() {
        Context context = context
        context.pop(FLOAT_TYPE)

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(FRETURN)
    }

    def returnDouble() {
        Context context = context
        context.pop(DOUBLE_TYPE)

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(DRETURN)
    }

    def returnLong() {
        Context context = context
        context.pop(LONG_TYPE)

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(LRETURN)
    }

    def returnFloatValues() {
        Context context = context
        context.pop(FLOAT_TYPE)

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(FRETURN)
    }

    def pushZeroInt() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(ICONST_0)
        context.push(INT_TYPE)
    }

    def pushOneInt() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(ICONST_1)
        context.push(INT_TYPE)
    }

    def pushFalse() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(ICONST_0)
        context.push(BOOLEAN_TYPE)
    }

    def pushTrue() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(ICONST_1)
        context.push(BOOLEAN_TYPE)
    }

    def pushBoolean(Boolean b) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(b ? ICONST_1 : ICONST_0)
        context.push(BOOLEAN_TYPE)
    }

    def pushInt(Integer i) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        switch (i) {
            case -1:
                mv.visitInsn(ICONST_M1)
                context.push(INT_TYPE)
                break
            case 0:
                mv.visitInsn(ICONST_0)
                context.push(INT_TYPE)
                break
            case 1:
                mv.visitInsn(ICONST_1)
                context.push(INT_TYPE)
                break
            case 2:
                mv.visitInsn(ICONST_2)
                context.push(INT_TYPE)
                break
            case 3:
                mv.visitInsn(ICONST_3)
                context.push(INT_TYPE)
                break
            case 4:
                mv.visitInsn(ICONST_4)
                context.push(INT_TYPE)
                break
            case 5:
                mv.visitInsn(ICONST_5)
                context.push(INT_TYPE)
                break
            default:
                mv.visitIntInsn(BIPUSH, i)
                context.push(INT_TYPE)
                break
        }
    }

    def pushLong(Long l) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        if (l == 0L) {
            mv.visitInsn(LCONST_0)
            return
        } else if (l == 1L) {
            mv.visitInsn(LCONST_1)
            return
        }
        mv.visitLdcInsn(l)
        context.push(LONG_TYPE)
    }

    def pushFloat(Float f) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        if (f == 0F) {
            mv.visitInsn(FCONST_0)
            return
        } else if (f == 1F) {
            mv.visitInsn(FCONST_1)
            return
        } else if (f == 2F) {
            mv.visitInsn(FCONST_2)
            return
        }
        mv.visitLdcInsn(f)
        context.push(FLOAT_TYPE)
    }

    def pushDouble(Double d) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        if (d == 0D) {
            mv.visitInsn(DCONST_0)
            return
        } else if (d == 1D) {
            mv.visitInsn(DCONST_1)
            return
        }
        mv.visitLdcInsn(d)
        context.push(DOUBLE_TYPE)
    }

    def pushString(String s) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        if (s == null) {
            mv.visitInsn(ACONST_NULL)
            context.push(getType(String.class))
            return
        }
        mv.visitLdcInsn(s)
        context.push(getType(String.class))
    }

    def pushChar(Character c) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        if (c == null) {
            throw new IllegalStateException("Unexpected null char")
        }
        switch (c) {
            case '\0':
                mv.visitInsn(ICONST_0)
                break
            case '\1':
                mv.visitInsn(ICONST_1)
                break
            case '\2':
                mv.visitInsn(ICONST_2)
                break
            case '\3':
                mv.visitInsn(ICONST_3)
                break
            case '\4':
                mv.visitInsn(ICONST_4)
                break
            case '\5':
                mv.visitInsn(ICONST_5)
                break
            default:
                mv.visitLdcInsn(c)
                context.push(CHAR_TYPE)
        }
    }

    def pushByte(Byte b) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitLdcInsn(b)
        context.push(BYTE_TYPE)
    }

    def pushShort(Short s) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitLdcInsn(s)
        context.push(SHORT_TYPE)
    }

    def loadConstant(Object value) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        switch (value) {
            case Type:
                mv.visitLdcInsn(value)
                context.push(getType(Class.class))
                break
            case Integer:
                mv.visitLdcInsn(value)
                context.push(INT_TYPE)
                break
            case Long:
                mv.visitLdcInsn(value)
                context.push(LONG_TYPE)
                break
            case Float:
                mv.visitLdcInsn(value)
                context.push(FLOAT_TYPE)
                break
            case Double:
                mv.visitLdcInsn(value)
                context.push(DOUBLE_TYPE)
                break
            case String:
                mv.visitLdcInsn(value)
                context.push(getType(String.class))
                break
            case Character:
                mv.visitLdcInsn(value)
                context.push(CHAR_TYPE)
                break
            case Byte:
                mv.visitLdcInsn(value)
                context.push(BYTE_TYPE)
                break
            case Short:
                mv.visitLdcInsn(value)
                context.push(SHORT_TYPE)
                break
            case Boolean:
                mv.visitLdcInsn(value)
                context.push(BOOLEAN_TYPE)
                break
            default:
                if (value != null) {
                    throw new RuntimeException("Unsupported constant owner: " + value.class)
                }
        }
    }

    def smartCast(Type to) {
        Type pop = context.peek()
        if ((pop.sort != OBJECT && pop.sort != ARRAY) || (to.sort != OBJECT && to.sort != ARRAY))
            smartCast(pop, to)
        else cast(to)
        context.pop(pop)
        context.push(to)
    }

    def loadThis(JvmClass type) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitVarInsn(ALOAD, 0)
        context.push(type.type)
    }

    def pushNull() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(ACONST_NULL)
        context.push(getType(Object.class))
    }

    def createArray(List<PyExpression> arguments, Type type) {
        pushStackByte(arguments.size())
        newArray(type)
        def stackSize = context.stackSize
        for (int i = 0, argumentsSize = arguments.size(); i < argumentsSize; i++) {
            PyExpression argument = arguments.get(i)
            dup()
            pushStackByte(i)
            argument.write(pc, this)
            if (context.peek().sort != OBJECT && argument.type.sort != ARRAY) {
                box(argument.type)
            }
            if (context.peek() != type) {
                cast(type)
            }

            arrayStoreObject(type)
        }

        if (stackSize != context.stackSize) {
            throw new RuntimeException("Stack size mismatch: " + stackSize + " != " + context.stackSize + " after createArray for [" + arguments.collect { it.class.name }.join(", ") + "]")
        }
    }

    def dynamicBuiltinCall(String name) {
        invokeDynamic(name, "([Ljava/lang/Object;Ljava/util/Map;)Ljava/lang/Object;", "__builtincall__")
    }

    def dynamicBuiltinGetAttr(String name) {
        invokeDynamic(name, "()Ljava/lang/Object;", "__builtingetattr__")
    }

    def dynamicBuiltinSetAttr(String name) {
        invokeDynamic(name, "(Ljava/lang/Object;)V", "__builtinsetattr__")
    }

    def dynamicBuiltinDelAttr(String name) {
        invokeDynamic(name, "()V", "__builtindelattr__")
    }

    def dynamicBuiltinHasAttr(String name) {
        invokeDynamic(name, "()Z", "__builtinhasattr__")
    }

    def dynamicAdd() {
        invokeDynamic("__add__", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    }

    def dynamicSub() {
        invokeDynamic("__sub__", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    }

    def dynamicMul() {
        invokeDynamic("__mul__", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    }

    def dynamicDiv() {
        invokeDynamic("__div__", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    }

    def dynamicMod() {
        invokeDynamic("__mod__", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    }

    def dynamicPow() {
        invokeDynamic("__pow__", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    }

    def dynamicFloorDiv() {
        invokeDynamic("__floordiv__", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    }

    def dynamicAnd() {
        invokeDynamic("__and__", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    }

    def dynamicOr() {
        invokeDynamic("__or__", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    }

    def dynamicXor() {
        invokeDynamic("__xor__", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    }

    def dynamicLShift() {
        invokeDynamic("__lshift__", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    }

    def dynamicRShift() {
        invokeDynamic("__rshift__", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    }

    def dynamicNot() {
        invokeDynamic("__not__", "(Ljava/lang/Object;)Ljava/lang/Object;")
    }

    def dynamicNeg() {
        invokeDynamic("__neg__", "(Ljava/lang/Object;)Ljava/lang/Object;")
    }

    def dynamicPos() {
        invokeDynamic("__pos__", "(Ljava/lang/Object;)Ljava/lang/Object;")
    }

    def dynamicIn() {
        invokeDynamic("__contains__", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    }

    def dynamicNotIn() {
        invokeDynamic("__contains__", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
        cast(getType(Object))
        invokeDynamic("__not__", "(Ljava/lang/Object;)Ljava/lang/Object;")
    }

    def dynamicIs() {
        invokeDynamic("__is__", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    }

    def dynamicIsNot() {
        invokeDynamic("__isnot__", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    }

    def dynamicStr() {
        invokeDynamic("__str__", "(Ljava/lang/Object;)Ljava/lang/String;")
    }

    def dynamicRepr() {
        invokeDynamic("__repr__", "(Ljava/lang/Object;)Ljava/lang/String;")
    }

    def dynamicBool() {
        invokeDynamic("__bool__", "(Ljava/lang/Object;)Z")
    }

    def dynamicLen() {
        invokeDynamic("__len__", "(Ljava/lang/Object;)I")
    }

    def dynamicIter() {
        invokeDynamic("__iter__", "(Ljava/lang/Object;)Ljava/lang/Object;")
    }


    def secretIter() {
        hiddenInvokeDynamic("__iter__", "(Ljava/lang/Object;)Ljava/lang/Object;")
    }

    def dynamicNext() {
        invokeDynamic("__next__", "(Ljava/lang/Object;)Ljava/lang/Object;")
    }

    def magicNext() {
        magicInvokeDynamic("__next__", "(Ljava/lang/Object;)Ljava/lang/Object;")
    }

    def magicHasNext() {
        magicInvokeDynamic("__hasnext__", "(Ljava/lang/Object;)Z")
    }

    def dynamicGetItem() {
        invokeDynamic("__getitem__", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    }

    def dynamicSetItem() {
        invokeDynamic("__setitem__", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V")
    }

    def setItem(PyAST owner, PyAST index, PyAST value) {
        owner.write(pc, this)
        index.write(pc, this)
        value.write(pc, this)
        dynamicSetItem()
    }

    def dynamicDelItem() {
        invokeDynamic("__delitem__", "(Ljava/lang/Object;Ljava/lang/Object;)V")
    }

    def delItem(PyAST owner, PyAST index) {
        owner.write(pc, this)
        index.write(pc, this)
        dynamicDelItem()
    }

    def dynamicEq() {
        invokeDynamic("__eq__", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    }

    def eq(PyAST left, PyAST right) {
        left.write pc, this
        cast Object
        right.write pc, this
        cast Object
        dynamicEq()
    }

    def dynamicNe() {
        invokeDynamic("__ne__", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    }

    def ne(PyAST left, PyAST right) {
        left.write pc, this
        cast Object
        right.write pc, this
        cast Object
        dynamicNe()
    }

    def dynamicLt() {
        invokeDynamic("__lt__", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    }

    def lt(PyAST left, PyAST right) {
        left.write pc, this
        cast Object
        right.write pc, this
        cast Object
        dynamicLt()
    }

    def dynamicLe() {
        invokeDynamic("__le__", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    }

    def le(PyAST left, PyAST right) {
        left.write pc, this
        cast Object
        right.write pc, this
        cast Object
        dynamicLe()
    }

    def dynamicGt() {
        invokeDynamic("__gt__", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    }

    def gt(PyAST left, PyAST right) {
        left.write pc, this
        cast Object
        right.write pc, this
        cast Object
        dynamicGt()
    }

    def dynamicGe() {
        invokeDynamic("__ge__", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    }

    def ge(PyAST left, PyAST right) {
        left.write pc, this
        cast Object
        right.write pc, this
        cast Object
        dynamicGe()
    }

    def dynamicCall() {
        invokeDynamic("__call__", "(Ljava/lang/Object;[Ljava/lang/Object;Ljava/util/Map;)Ljava/lang/Object;")
    }

    def call(PyExpression owner, List<PyExpression> arguments, Map<String, PyExpression> kwargs) {
        owner.write(pc, this)
        cast Object
        createArgs(arguments)
        castList()
        createKwargs(kwargs)
        castMap()
        dynamicCall()
    }

    def call(PyExpression owner, Map<String, PyExpression> kwargs) {
        owner.write(pc, this)
        cast Object
        createArgs([])
        castList()
        createKwargs(kwargs)
        castMap()
        dynamicCall()
    }

    def call(PyExpression owner, List<PyExpression> arguments, PyExpression kwargs) {
        owner.write(pc, this)
        cast Object
        createArgs(arguments)
        cast Object[]
        kwargs.write(pc, this)
        castMap()
        dynamicCall()
    }

    def call(PyExpression owner, List<PyExpression> arguments) {
        owner.write(pc, this)
        cast Object
        createArgs(arguments)
        castList()
        createKwargs()
        castMap()
        dynamicCall()
    }

    def call(PyExpression owner) {
        owner.write(pc, this)
        createArgs([])
        castList()
        createKwargs()
        castMap()
        dynamicCall()
    }

    def dynamicCall(String name) {
        invokeDynamic(name, "(Ljava/lang/Object;[Ljava/lang/Object;Ljava/util/Map;)Ljava/lang/Object;", "__call__")
    }

    def magicDynamicCall(String name) {
        magicInvokeDynamic(name, "(Ljava/lang/Object;[Ljava/lang/Object;Ljava/util/Map;)Ljava/lang/Object;", "__call__")
    }

    def hiddenDynamicCall(String name) {
        hiddenInvokeDynamic(name, "(Ljava/lang/Object;[Ljava/lang/Object;Ljava/util/Map;)Ljava/lang/Object;", "__call__")
    }

    def dynamicCall(PyExpression owner, List<PyExpression> arguments) {
        if (owner instanceof MemberExpression) {
            MemberExpression memberExpression = (MemberExpression) owner
            memberExpression.writeAttrOnly(pc, this)
        } else owner.write(pc, this)
        createArgs(arguments)
        createKwargs()
        dynamicCall()
    }

    def returnValue(Type type, Location location) {
        Context context = context

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        if (type == VOID_TYPE) {
            if (context.popNeeded)
                pop()
            mv.visitInsn(RETURN)
            return
        }

        if (!context.popNeeded) {
            throw new CompilerException("No value to return", location)
        }

        Type boxedType = boxType(type)
        cast(boxedType)
        if (boxedType != type) unbox(type)
        context.pop()

        switch (type.sort) {
            case BYTE:
            case SHORT:
            case INT:
            case CHAR:
            case BOOLEAN:
                mv.visitInsn(IRETURN)
                break
            case Type.LONG:
                mv.visitInsn(LRETURN)
                break
            case Type.FLOAT:
                mv.visitInsn(FRETURN)
                break
            case Type.DOUBLE:
                mv.visitInsn(DRETURN)
                break
            case OBJECT:
                mv.visitInsn(ARETURN)
                break
            default:
                throw new RuntimeException("Unknown return type: " + type)
        }
    }

    def loadClass(JvmClass jvmClass) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        Type type = jvmClass.type
        if (type.descriptor.contains(".")) {
            throw new RuntimeException("Invalid type: " + type)
        }
        mv.visitLdcInsn(boxType(type))
        Context context = context
        context.push(getType(Class.class))
    }

    def createKwargs() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitMethodInsn(INVOKESTATIC, "java/util/Collections", "emptyMap", "()Ljava/util/Map;", false)
        Context context = context
        context.push(getType(Map.class))
    }

    def writeArgs(List<PyExpression> arguments, Type[] types) {
        for (int i = 0, argumentsSize = arguments.size(); i < argumentsSize; i++) {
            PyExpression argument = arguments.get(i)
            argument.write(pc, this)
            if (types[i] == null || types[i].sort == OBJECT) {
                if (argument.type.sort != ARRAY && argument.type.sort != OBJECT) {
                    pc.writer.box(argument.type)
                }
            }

            if (types[i] != null) {
                if (types[i].sort != OBJECT && types[i].sort != ARRAY) {
                    pc.writer.unbox(types[i])
                }
                pc.writer.cast(types[i])
            } else {
                pc.writer.cast(getType(Object.class))
            }
        }
    }

    def loadValue(int index, Type type) {
        switch (type.sort) {
            case BYTE:
            case SHORT:
            case INT:
            case CHAR:
            case BOOLEAN:
                var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
                mv.visitVarInsn(ILOAD, index)
                context.push(type)
                break
            case Type.LONG:
                var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
                mv.visitVarInsn(LLOAD, index)
                context.push(type)
                break
            case Type.FLOAT:
                var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
                mv.visitVarInsn(FLOAD, index)
                context.push(type)
                break
            case Type.DOUBLE:
                var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
                mv.visitVarInsn(DLOAD, index)
                context.push(type)
                break
            case OBJECT:
            case ARRAY:
                var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
                mv.visitVarInsn(ALOAD, index)
                context.push(type)
                break
            default:
                throw new RuntimeException("Unknown type: " + type)
        }
    }

    def parameter(String name) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitParameter(name, 0)
        context.push(getType(Object.class))
    }

    def loadThis(PyClass pyClass) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitVarInsn(ALOAD, 0)
        context.push(pyClass.type)
    }

    MethodVisitor mv() {
        if (pc.methodOut == null) {
            if (pc.rootInitMv == null) {
                throw new IllegalStateException("Outside a method definition!")
            }
            return pc.rootInitMv
        }
        return pc.methodOut
    }

    def hiddenDup() {
        mv().visitInsn(DUP)
    }

    def createKwargs(Map<String, PyExpression> kwargs) {
        List<PyExpression> expressions = new ArrayList<>()
        newObject(getType(HashMap.class))
        dup()
        invokeSpecial(getType(HashMap.class), "<init>", methodType(VOID_TYPE), false)

        List<Map.Entry<String, PyExpression>> copyOf = List.copyOf(kwargs.entrySet())
        for (int i = 0, copyOfSize = copyOf.size(); i < copyOfSize; i++) {
            Map.Entry<String, PyExpression> entry = copyOf.get(i)
            String s = entry.key
            PyExpression expression = entry.value
            if (i < copyOfSize - 1)
                dup()
            expressions.add(new ConstantExpr(s, expression.location))
            expressions.add(expression)

            invokeVirtual(getType(HashMap.class), "put", methodType(getType(Object.class), getType(Object.class), getType(Object.class)), false)
        }
    }

    Location lastLocation() {
        return lastLocation
    }

    def lastLocation(Location location) {
        if (location == null) return
        if (location.unknown) return
        if (lastLocation == location) return
        this.lastLocation = location
    }

    static Label newLabel() {
        return new Label()
    }

    void autoReturn(Type returnType) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        if (mv != null) {
            if (returnType != VOID_TYPE) {
                switch (returnType.sort) {
                    case BYTE:
                    case SHORT:
                    case INT:
                    case CHAR:
                    case BOOLEAN:
                        mv.visitInsn(ICONST_0)
                        mv.visitInsn(IRETURN)
                        break
                    case Type.LONG:
                        mv.visitInsn(LCONST_0)
                        mv.visitInsn(LRETURN)
                        break
                    case Type.FLOAT:
                        mv.visitInsn(FCONST_0)
                        mv.visitInsn(FRETURN)
                        break
                    case Type.DOUBLE:
                        mv.visitInsn(DCONST_0)
                        mv.visitInsn(DRETURN)
                        break
                    case OBJECT:
                    case ARRAY:
                        mv.visitInsn(ACONST_NULL)
                        mv.visitInsn(ARETURN)
                        break
                }
            } else {
                mv.visitInsn(RETURN)
            }
        } else {
            throw new IllegalStateException("Outside a method definition!")
        }
    }

    void dynamicImport(ModulePath path, @Nullable PyAlias alias) {
        invokeDynamic("__import__", "()Ljava/lang/Object;", path.toString(), alias == null ? "*" : alias.name)
    }

    void dynamicSubscript() {
        invokeDynamic("__getitem__", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    }

    void dynamicTuple() {
        invokeDynamic("tupleExtract", "(Ljava/lang/Object;)[Ljava/lang/Object;")
    }

    void dynamicTuple(int count) {
        // Extract tuple
        dynamicTuple()
        cast(getType(Object.class))
        
        // Duplicate for length check
        dup()
        
        // Get and check length
        dynamicLen()
        
        // Load count for comparison
        pushInt(count)
        
        Label label = newLabel()
        ifICmpNe(label)

        // Throw path
        newObject(getObjectType("org/python/builtins/ValueError"))
        dup()

        createArgs([new ConstantExpr("Cannot unpack tuple of size " + count, lastLocation())])
        
        // Initialize exception
        invokeSpecial(getObjectType("org/python/builtins/ValueError"), "<init>",
                     methodType(VOID_TYPE, getType(Object[])), false)

        throwObject()
        
        // Continue with valid tuple
        mv().visitLabel(label)
        cast(getType(Object[].class))
    }

    void ifICmpEq(Label label) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        Type type2 = context.pop()
        Type type1 = context.pop()

        if (type1.sort == OBJECT || type2.sort == OBJECT) {
            // For objects, use Python's eq comparison
            invokeStatic("dev/ultreon/pythonc/runtime/Py", "__eq__",
                    "(Ljava/lang/Object;Ljava/lang/Object;)Z", false)
            mv.visitJumpInsn(IFNE, label)
        } else {
            // For primitives
            if (type2 != INT_TYPE) {
                cast(INT_TYPE)
            }
            if (type1 != INT_TYPE) {
                swap()
                cast(INT_TYPE)
                swap()
            }
            mv.visitJumpInsn(IF_ICMPEQ, label)
        }
    }

    void ifICmpNe(Label label) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        Type type2 = context.pop()
        Type type1 = context.pop()

        if (type1.sort == OBJECT || type2.sort == OBJECT) {
            // For objects, use Python's ne comparison
            invokeStatic("dev/ultreon/pythonc/runtime/Py", "__ne__",
                    "(Ljava/lang/Object;Ljava/lang/Object;)Z", false)
            mv.visitJumpInsn(IFNE, label)
        } else {
            // For primitives
            if (type2 != INT_TYPE) {
                cast(INT_TYPE)
            }
            if (type1 != INT_TYPE) {
                swap()
                cast(INT_TYPE)
                swap()
            }
            mv.visitJumpInsn(IF_ICMPNE, label)
        }
    }

    void ifICmpGe(Label label) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        Type type2 = context.pop()
        Type type1 = context.pop()

        if (type1.sort == OBJECT || type2.sort == OBJECT) {
            // For objects, use Python's ge comparison
            invokeStatic("dev/ultreon/pythonc/runtime/ClassUtils", "ge",
                    "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false)
            cast(BOOLEAN_TYPE)
            mv.visitJumpInsn(IFNE, label)
        } else {
            // For primitives
            if (type2 != INT_TYPE) {
                cast(INT_TYPE)
            }
            if (type1 != INT_TYPE) {
                swap()
                cast(INT_TYPE)
                swap()
            }
            mv.visitJumpInsn(IF_ICMPGE, label)
        }
    }

    void ifICmpLe(Label label) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        Type type2 = context.pop()
        Type type1 = context.pop()

        if (type1.sort == OBJECT || type2.sort == OBJECT) {
            // For objects, use Python's le comparison
            invokeStatic("dev/ultreon/pythonc/runtime/ClassUtils", "le",
                    "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false)
            cast(BOOLEAN_TYPE)
            mv.visitJumpInsn(IFNE, label)
        } else {
            // For primitives
            if (type2 != INT_TYPE) {
                cast(INT_TYPE)
            }
            if (type1 != INT_TYPE) {
                swap()
                cast(INT_TYPE)
                swap()
            }
            mv.visitJumpInsn(IF_ICMPLE, label)
        }
    }

    void ifICmpGt(Label label) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        Type type2 = context.pop()
        Type type1 = context.pop()

        if (type1.sort == OBJECT || type2.sort == OBJECT) {
            // For objects, use Python's gt comparison
            invokeStatic("dev/ultreon/pythonc/runtime/ClassUtils", "gt",
                    "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false)
            cast(BOOLEAN_TYPE)
            mv.visitJumpInsn(IFNE, label)
        } else {
            // For primitives
            if (type2 != INT_TYPE) {
                cast(INT_TYPE)
            }
            if (type1 != INT_TYPE) {
                swap()
                cast(INT_TYPE)
                swap()
            }
            mv.visitJumpInsn(IF_ICMPGT, label)
        }
    }

    void ifICmpLt(Label label) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        Type type2 = context.pop()
        Type type1 = context.pop()

        if (type1.sort == OBJECT || type2.sort == OBJECT) {
            // For objects, use Python's lt comparison
            invokeStatic("dev/ultreon/pythonc/runtime/ClassUtils", "lt",
                    "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false)
            cast(BOOLEAN_TYPE)
            mv.visitJumpInsn(IFNE, label)
        } else {
            // For primitives
            if (type2 != INT_TYPE) {
                cast(INT_TYPE)
            }
            if (type1 != INT_TYPE) {
                swap()
                cast(INT_TYPE)
                swap()
            }
            mv.visitJumpInsn(IF_ICMPLT, label)
        }
    }

    void ifNull(Label label) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        cast Object
        mv.visitJumpInsn(IFNULL, label)
    }

    void ifNonNull(Label label) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        cast Object
        mv.visitJumpInsn(IFNONNULL, label)
    }

    void ifEq(Label label) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        Type type = context.pop()

        if (type.sort == OBJECT) {
            // For objects, we need to call equals()
            invokeStatic("dev/ultreon/pythonc/runtime/ClassUtils", "eq",
                    "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false)
            cast(BOOLEAN_TYPE)
            mv.visitJumpInsn(IFNE, label)
        } else {
            // For primitives
            if (type != INT_TYPE) {
                cast(INT_TYPE)
            }
            mv.visitJumpInsn(IFEQ, label)
        }
    }

    void ifNe(Label label) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        Type type = context.pop()

        if (type.sort == OBJECT) {
            // For objects, we need to call equals()
            invokeStatic("dev/ultreon/pythonc/runtime/ClassUtils", "ne",
                    "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false)
            cast(BOOLEAN_TYPE)
            mv.visitJumpInsn(IFNE, label)
        } else {
            // For primitives
            if (type != INT_TYPE) {
                cast(INT_TYPE)
            }
            mv.visitJumpInsn(IFNE, label)
        }
    }

    void ifGe(Label label) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        Type type = context.pop()
        Type type2 = context.pop()

        if (type.sort == OBJECT || type2.sort == OBJECT) {
            // Need to handle object comparison
            invokeStatic("dev/ultreon/pythonc/runtime/ClassUtils", "ge",
                    "(Ljava/lang/Object;Ljava/lang/Object;)Z", false)
            mv.visitJumpInsn(IFNE, label)
        } else {
            // For primitives
            if (type != INT_TYPE) {
                cast(INT_TYPE)
            }
            if (type2 != INT_TYPE) {
                cast(INT_TYPE)
            }
            mv.visitJumpInsn(IFGE, label)
        }
    }

    void ifGt(Label label) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        Type type = context.pop()
        Type type2 = context.pop()

        if (type.sort == OBJECT || type2.sort == OBJECT) {
            // Need to handle object comparison
            invokeStatic("dev/ultreon/pythonc/runtime/ClassUtils", "gt",
                    "(Ljava/lang/Object;Ljava/lang/Object;)Z", false)
            mv.visitJumpInsn(IFNE, label)
        } else {
            // For primitives
            if (type != INT_TYPE) {
                cast(INT_TYPE)
            }
            if (type2 != INT_TYPE) {
                cast(INT_TYPE)
            }
            mv.visitJumpInsn(IFGT, label)
        }
    }

    void ifLe(Label label) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        Type type = context.pop()
        Type type2 = context.pop()

        if (type.sort == OBJECT || type2.sort == OBJECT) {
            // Need to handle object comparison
            invokeStatic("dev/ultreon/pythonc/runtime/ClassUtils", "le",
                    "(Ljava/lang/Object;Ljava/lang/Object;)Z", false)
            mv.visitJumpInsn(IFNE, label)
        } else {
            // For primitives
            if (type != INT_TYPE) {
                cast(INT_TYPE)
            }
            if (type2 != INT_TYPE) {
                cast(INT_TYPE)
            }
            mv.visitJumpInsn(IFLE, label)
        }
    }

    void ifLt(Label label) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        Type type = context.pop()
        Type type2 = context.pop()

        if (type.sort == OBJECT || type2.sort == OBJECT) {
            // Need to handle object comparison
            invokeStatic("dev/ultreon/pythonc/runtime/ClassUtils", "lt",
                    "(Ljava/lang/Object;Ljava/lang/Object;)Z", false)
            mv.visitJumpInsn(IFNE, label)
        } else {
            // For primitives
            if (type != INT_TYPE) {
                cast(INT_TYPE)
            }
            if (type2 != INT_TYPE) {
                cast(INT_TYPE)
            }
            mv.visitJumpInsn(IFLT, label)
        }
    }

    void getArrayElement(int i) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        def pop = context.pop()
        if (pop.sort != ARRAY) {
            throw new RuntimeException("Expected array, got: " + pop.className)
        }
        mv.visitLdcInsn(i)
        mv.visitInsn(AALOAD)
        context.push(getType(Object.class))
    }

    void dynamicGetDynAttr(String name) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        invokeDynamic("__dyngetattr__", "(Ljava/lang/Class;)Ljava/lang/Object;", name)
    }
}
