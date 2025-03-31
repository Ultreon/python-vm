//file:noinspection unused
//file:noinspection GroovyFallthrough
package dev.ultreon.pythonc

import dev.ultreon.pythonc.classes.JvmClass
import dev.ultreon.pythonc.classes.PyClass
import dev.ultreon.pythonc.expr.ConstantExpr
import dev.ultreon.pythonc.expr.MemberExpression
import dev.ultreon.pythonc.expr.PyExpression
import org.objectweb.asm.*

import java.util.stream.Collectors

import static org.objectweb.asm.Opcodes.*
import static org.objectweb.asm.Type.BOOLEAN_TYPE
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
        if (returnType == Type.VOID_TYPE) return
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
        if (methodType.returnType.sort != Type.VOID) {
            context.push(methodType.returnType)
        }
    }

    def invokeSpecial(Type owner, String name, Type signature, boolean isInterface) {
        if (signature.sort != Type.METHOD) throw new IllegalStateException("Not a method signature!")

        def context = context
        def argumentTypes = signature.argumentTypes
        context.pop()
        for (Type arg : argumentTypes) {
            def pop = context.pop()
            classSymbol pop.className
        }
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitMethodInsn INVOKESPECIAL, owner.internalName, name, signature.descriptor, isInterface
        if (signature.returnType.sort != Type.VOID) {
            context.push signature.returnType
        }
    }

    @Deprecated
    def newInstance(String owner, String name, String signature, boolean isInterface, Runnable paramInit) {
        newObject Type.getObjectType(owner)
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
        if (returnType == Type.VOID_TYPE) return
        context.push returnType
    }

    def invokeVirtual(Type owner, String name, Type signature, boolean isInterface) {
        if (signature.sort != Type.METHOD) throw new IllegalStateException("Not a method signature!")

        def context = context
        def methodType = methodType signature
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
        if (returnType == Type.VOID_TYPE) return
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
        if (returnType == Type.VOID_TYPE) return
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
        if (returnType.sort == Type.VOID) return
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
        if (returnType.sort == Type.VOID) return
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
        invokeDynamic(name, "(Ljava/lang/Object;[Ljava/lang/Object;Ljava/util/Map;)Ljava/lang/Object;", "__call__")
    }

    def createArgs(List<PyExpression> arguments) {
        createArray(arguments, Type.getType(Object.class))
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
        Type type = Type.getType(descriptor)
        pushClass(Type.getObjectType(owner))
        dynamicGetAttr(name)
        context.push(type)
    }

    def putStatic(String owner, String name, String descriptor, PyExpression expr) {
        Type ownerType = Type.getObjectType(owner)
        pushClass(ownerType)
        expr.write(pc, this)
        dynamicSetAttr(name)
    }

    def pushClass(Type ownerType) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitLdcInsn(ownerType)
        context.push(Type.getType(Class.class))
    }

    def getField(String name, String descriptor) {
        dynamicGetAttr(name)
        cast(Type.getType(descriptor))
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
        context.push(Type.getType(String.class))
    }

    def loadConstant(int value) {
        Context context = context
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitLdcInsn(value)
        context.push(Type.INT_TYPE)
    }

    def loadConstant(long value) {
        Context context = context
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitLdcInsn(value)
        context.push(Type.LONG_TYPE)
    }

    def loadConstant(float value) {
        Context context = context
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitLdcInsn(value)
        context.push(Type.FLOAT_TYPE)
    }

    def loadConstant(double value) {
        Context context = context
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitLdcInsn(value)
        context.push(Type.DOUBLE_TYPE)
    }

    def loadConstant(char value) {
        Context context = context
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitLdcInsn(value)
        context.push(Type.CHAR_TYPE)
    }

    def loadConstant(boolean value) {
        Context context = context
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitLdcInsn(value)
        context.push(Type.BOOLEAN_TYPE)
    }

    def loadConstant(byte value) {
        Context context = context
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitLdcInsn(value)
        context.push(Type.BYTE_TYPE)
    }

    def loadConstant(short value) {
        Context context = context
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitLdcInsn(value)
        context.push(Type.SHORT_TYPE)
    }

    def loadClass(Type type) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitLdcInsn(type)
        context.push(Type.getType("Ljava/lang/Class;"))
    }

    def storeInt(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitVarInsn(ISTORE, index)
        Type pop = context.pop()
        if (pop != Type.INT_TYPE) {
            throw new RuntimeException("Expected int, got " + pop)
        }
    }

    def loadInt(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitVarInsn(ILOAD, index)
        context.push(Type.INT_TYPE)
    }

    def storeLong(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitVarInsn(LSTORE, index)
        Type pop = context.pop()
        if (pop != Type.LONG_TYPE) {
            throw new RuntimeException("Expected long, got " + pop)
        }
    }

    def loadLong(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitVarInsn(LLOAD, index)
        context.push(Type.LONG_TYPE)
    }

    def storeFloat(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitVarInsn(FSTORE, index)
        Type pop = context.pop()
        if (pop != Type.FLOAT_TYPE) {
            throw new RuntimeException("Expected float, got " + pop)
        }
    }

    def loadFloat(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitVarInsn(FLOAD, index)
        context.push(Type.FLOAT_TYPE)
    }

    def storeDouble(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitVarInsn(DSTORE, index)
        Type pop = context.pop()
        if (pop != Type.DOUBLE_TYPE) {
            throw new RuntimeException("Expected double, got " + pop)
        }
    }

    def loadDouble(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitVarInsn(DLOAD, index)
        context.push(Type.DOUBLE_TYPE)
    }

    def storeChar(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitVarInsn(ISTORE, index)
        Type pop = context.pop()
        if (pop != Type.CHAR_TYPE) {
            throw new RuntimeException("Expected char, got " + pop)
        }
    }

    def loadChar(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitVarInsn(ILOAD, index)
        context.push(Type.CHAR_TYPE)
    }

    def storeBoolean(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        cast(Type.BOOLEAN_TYPE)
        mv.visitVarInsn(ISTORE, index)
        Type pop = context.pop()
        if (pop != Type.BOOLEAN_TYPE) {
            throw new RuntimeException("Expected boolean, got " + pop)
        }
    }

    def loadBoolean(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitVarInsn(ILOAD, index)
        cast(Type.BOOLEAN_TYPE)
        context.push(Type.BOOLEAN_TYPE)
    }

    def storeByte(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        cast(Type.BYTE_TYPE)
        mv.visitVarInsn(ISTORE, index)
        Type pop = context.pop()
        if (pop != Type.BYTE_TYPE) {
            throw new RuntimeException("Expected byte, got " + pop)
        }
    }

    def loadByte(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitVarInsn(ILOAD, index)
        cast(Type.BYTE_TYPE)
        context.push(Type.BYTE_TYPE)
    }

    def storeShort(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        cast(Type.SHORT_TYPE)
        mv.visitVarInsn(ISTORE, index)
        Type pop = context.pop()
        if (pop != Type.SHORT_TYPE) {
            throw new RuntimeException("Expected short, got " + pop)
        }
    }

    def loadShort(int index) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitVarInsn(ILOAD, index)
        cast(Type.SHORT_TYPE)
        context.push(Type.SHORT_TYPE)
    }

    def storeObject(int index, Type type) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitVarInsn(ASTORE, index)
        context.pop()
    }

    def loadObject(int index, Type type) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitVarInsn(ALOAD, index)
        context.push(Type.getType(Object.class))
        cast(type)
    }

    def jump(Label endLabel) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitJumpInsn(Opcodes.GOTO, endLabel)
    }

    def cast(Type to) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        Context context = context
        Type from = context.peek()
        if (to.sort == Type.OBJECT) {
            if (from != to) {
                if (from == Type.BYTE_TYPE)
                    invokeStatic("java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false)
                if (from == Type.SHORT_TYPE)
                    invokeStatic("java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false)
                if (from == Type.CHAR_TYPE)
                    invokeStatic("java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false)
                if (from == Type.INT_TYPE)
                    invokeStatic("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false)
                if (from == Type.LONG_TYPE)
                    invokeStatic("java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false)
                if (from == Type.FLOAT_TYPE)
                    invokeStatic("java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false)
                if (from == Type.DOUBLE_TYPE)
                    invokeStatic("java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false)
                if (from == Type.BOOLEAN_TYPE)
                    invokeStatic("java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false)
                context.pop()
                checkCast(to)
                context.push(to)
            }
        } else switch (to.sort) {
            case Type.BOOLEAN:
                context.pop()
                switch (from.sort) {
                    case Type.BOOLEAN:
                    case Type.INT:// do nothing
                        break
                    case Type.BYTE:
                        mv.visitInsn(I2B)
                        break
                    case Type.SHORT:
                        mv.visitInsn(I2S)
                        break
                    case Type.CHAR:
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
                    case Type.OBJECT:
                        mv.visitTypeInsn(CHECKCAST, "java/lang/Boolean")
                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false)
                        break
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

                context.push(Type.BOOLEAN_TYPE)
                break
            case Type.INT:
                context.pop()
                switch (from.sort) {
                    case Type.INT:// do nothing
                        break
                    case Type.BYTE:
                        mv.visitInsn(I2B)
                        break
                    case Type.SHORT:
                        mv.visitInsn(I2S)
                        break
                    case Type.CHAR:
                        mv.visitInsn(I2C)
                        break
                    case Type.LONG:
                        mv.visitInsn(I2L)
                        break
                    case Type.FLOAT:
                        mv.visitInsn(F2I)
                        break
                    case Type.DOUBLE:
                        mv.visitInsn(D2I)
                        break
                    case Type.OBJECT:
                        mv.visitTypeInsn(CHECKCAST, "java/lang/Integer")
                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false)
                        break
                    default:
                        throw new RuntimeException("Unsupported cast to " + to)
                }
                context.push(Type.INT_TYPE)
                break
            case Type.LONG:
                context.pop()
                switch (from.sort) {
                    case Type.INT:
                        mv.visitInsn(I2L)
                        break
                    case Type.BYTE:
                        mv.visitInsn(I2B)
                        break
                    case Type.SHORT:
                        mv.visitInsn(I2S)
                        break
                    case Type.CHAR:
                        mv.visitInsn(I2C)
                        break
                    case Type.LONG:// do nothing
                        break
                    case Type.FLOAT:
                        mv.visitInsn(F2L)
                        break
                    case Type.DOUBLE:
                        mv.visitInsn(D2L)
                        break
                    case Type.OBJECT:
                        mv.visitTypeInsn(CHECKCAST, "java/lang/Long")
                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false)
                        break
                    default:
                        throw new RuntimeException("Unsupported cast to " + to)
                }
                context.push(Type.LONG_TYPE)
                break
            case Type.FLOAT:
                context.pop()
                switch (from.sort) {
                    case Type.INT:
                    case Type.CHAR:
                    case Type.SHORT:
                    case Type.BYTE:
                        mv.visitInsn(I2F)
                        break
                    case Type.LONG:
                        mv.visitInsn(L2F)
                        break
                    case Type.FLOAT:// do nothing
                        break
                    case Type.DOUBLE:
                        mv.visitInsn(D2F)
                        break
                    case Type.OBJECT:
                        mv.visitTypeInsn(CHECKCAST, "java/lang/Float")
                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false)
                        break
                    default:
                        throw new RuntimeException("Unsupported cast to " + to)
                }
                context.push(Type.FLOAT_TYPE)
                break
            case Type.DOUBLE:
                context.pop()
                switch (from.sort) {
                    case Type.INT:
                    case Type.CHAR:
                    case Type.SHORT:
                    case Type.BYTE:
                        mv.visitInsn(I2D)
                        break
                    case Type.LONG:
                        mv.visitInsn(L2D)
                        break
                    case Type.FLOAT:
                        mv.visitInsn(F2D)
                        break
                    case Type.DOUBLE:// do nothing
                        break
                    case Type.OBJECT:
                        mv.visitTypeInsn(CHECKCAST, "java/lang/Double")
                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false)
                        break
                    default:
                        throw new RuntimeException("Unsupported cast from " + from + " to " + to)
                }
                context.push(Type.DOUBLE_TYPE)
                break
            case Type.ARRAY:
                if (from != to) {
                    if (from.sort != Type.ARRAY && from.sort != Type.OBJECT) {
                        throw new RuntimeException("Unsupported cast from " + from + " to " + to)
                    }
                }
                break
            default:
                throw new RuntimeException("Unsupported cast to " + to)
        }
    }

    def checkCast(Type to) {
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
        Type t2 = context.pop()
        context.push(t2)
        context.push(t1)
    }

    def dup2() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(DUP2)

        Context context = context
        context.push(context.peek())
        context.push(context.peek())
    }

    def dup2_x1() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(DUP2_X1)

        Context context = context
        Type t1 = context.pop()
        Type t2 = context.pop()
        context.push(t2)
        context.push(t1)
        context.push(t2)
    }

    def dup2_x2() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(DUP2_X2)

        Context context = context
        Type t1 = context.pop()
        Type t2 = context.pop()
        context.push(t2)
        context.push(t1)
        context.push(t2)
        context.push(t1)
    }

    def pushStackByte(int i) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitIntInsn(BIPUSH, i)

        context.push(Type.BYTE_TYPE)
    }

    def pushStackShort(int i) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitIntInsn(SIPUSH, i)

        context.push(Type.SHORT_TYPE)
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
        if (to == Type.DOUBLE_TYPE) {
            if (from == Type.LONG_TYPE) {
                mv.visitInsn(L2D)
            } else if (from == Type.FLOAT_TYPE) {
                mv.visitInsn(F2D)
            } else if (from == Type.INT_TYPE) {
                mv.visitInsn(I2D)
            }
        } else if (to == Type.FLOAT_TYPE) {
            if (from == Type.DOUBLE_TYPE) {
                mv.visitInsn(D2F)
            } else if (from == Type.LONG_TYPE) {
                mv.visitInsn(L2F)
            } else if (from == Type.INT_TYPE) {
                mv.visitInsn(I2F)
            }
        } else if (to == Type.LONG_TYPE) {
            if (from == Type.DOUBLE_TYPE) {
                mv.visitInsn(D2L)
            } else if (from == Type.FLOAT_TYPE) {
                mv.visitInsn(F2L)
            } else if (from == Type.INT_TYPE) {
                mv.visitInsn(I2L)
            }
        } else if (to == Type.INT_TYPE) {
            if (from == Type.DOUBLE_TYPE) {
                mv.visitInsn(D2I)
            } else if (from == Type.FLOAT_TYPE) {
                mv.visitInsn(F2I)
            } else if (from == Type.LONG_TYPE) {
                mv.visitInsn(L2I)
            }
        } else if (to == Type.BOOLEAN_TYPE) {
            if (from == Type.DOUBLE_TYPE) {
                mv.visitInsn(D2I)
            } else if (from == Type.FLOAT_TYPE) {
                mv.visitInsn(F2I)
            } else if (from == Type.LONG_TYPE) {
                mv.visitInsn(L2I)
            }
        } else if (to == Type.CHAR_TYPE) {
            if (from == Type.DOUBLE_TYPE) {
                mv.visitInsn(D2I)
                mv.visitInsn(I2C)
            } else if (from == Type.FLOAT_TYPE) {
                mv.visitInsn(F2I)
                mv.visitInsn(I2C)
            } else if (from == Type.LONG_TYPE) {
                mv.visitInsn(L2I)
                mv.visitInsn(I2C)
            } else if (from == Type.INT_TYPE) {
                mv.visitInsn(I2C)
            }
        } else if (to == Type.SHORT_TYPE) {
            if (from == Type.DOUBLE_TYPE) {
                mv.visitInsn(D2I)
                mv.visitInsn(I2S)
            } else if (from == Type.FLOAT_TYPE) {
                mv.visitInsn(F2I)
                mv.visitInsn(I2S)
            } else if (from == Type.LONG_TYPE) {
                mv.visitInsn(L2I)
                mv.visitInsn(I2S)
            } else if (from == Type.INT_TYPE) {
                mv.visitInsn(I2S)
            }
        } else if (to == Type.BYTE_TYPE) {
            if (from == Type.DOUBLE_TYPE) {
                mv.visitInsn(D2I)
                mv.visitInsn(I2B)
            } else if (from == Type.FLOAT_TYPE) {
                mv.visitInsn(F2I)
                mv.visitInsn(I2B)
            } else if (from == Type.LONG_TYPE) {
                mv.visitInsn(L2I)
                mv.visitInsn(I2B)
            } else if (from == Type.INT_TYPE) {
                mv.visitInsn(I2B)
            }
        } else if (to.sort == Type.OBJECT) {
            if (from == Type.DOUBLE_TYPE) {
                if (to.internalName == "java/lang/Double") {
                    invokeStatic("java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false)
                } else {
                    smartCast(from, boxType(to))
                }
            } else if (from == Type.FLOAT_TYPE && to.internalName == "java/lang/Float") {
                invokeStatic("java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false)
            } else if (from == Type.LONG_TYPE && to.internalName == "java/lang/Long") {
                invokeStatic("java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false)
            } else if (from == Type.INT_TYPE && to.internalName == "java/lang/Integer") {
                invokeStatic("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false)
            } else if (from == Type.BOOLEAN_TYPE && to.internalName == "java/lang/Boolean") {
                invokeStatic("java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false)
            } else if (from == Type.CHAR_TYPE && to.internalName == "java/lang/Character") {
                invokeStatic("java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false)
            } else if (from == Type.BYTE_TYPE && to.internalName == "java/lang/Byte") {
                invokeStatic("java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false)
            } else if (from == Type.SHORT_TYPE && to.internalName == "java/lang/Short") {
                invokeStatic("java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false)
            } else if (from != to) {
                if (PythonCompiler.classCache.require(pc, to).doesInherit(pc, PythonCompiler.classCache.require(pc, from))) {
                    return
                }
                throw new RuntimeException("Cannot smart cast " + from.className + " to " + to.className)
            }
        } else if (to.sort == Type.ARRAY) {
            if (from != to) {
                throw new RuntimeException("Cannot smart cast " + from.className + " to " + to.className)
            }
        } else {
            throw new RuntimeException("Cannot smart cast " + from.className + " to " + to.className)
        }
    }

    static Type boxType(Type from) {
        if (from == Type.DOUBLE_TYPE) {
            return Type.getType(Double.class)
        } else if (from == Type.FLOAT_TYPE) {
            return Type.getType(Float.class)
        } else if (from == Type.LONG_TYPE) {
            return Type.getType(Long.class)
        } else if (from == Type.INT_TYPE) {
            return Type.getType(Integer.class)
        } else if (from == Type.BOOLEAN_TYPE) {
            return Type.getType(Boolean.class)
        } else if (from == Type.CHAR_TYPE) {
            return Type.getType(Character.class)
        } else if (from == Type.BYTE_TYPE) {
            return Type.getType(Byte.class)
        } else if (from == Type.SHORT_TYPE) {
            return Type.getType(Short.class)
        } else {
            return from
        }
    }

    static Type unboxType(Type from) {
        if (from == Type.getType(Double.class) || from == Type.DOUBLE_TYPE) {
            return Type.DOUBLE_TYPE
        } else if (from == Type.getType(Float.class) || from == Type.FLOAT_TYPE) {
            return Type.FLOAT_TYPE
        } else if (from == Type.getType(Long.class) || from == Type.LONG_TYPE) {
            return Type.LONG_TYPE
        } else if (from == Type.getType(Integer.class) || from == Type.INT_TYPE) {
            return Type.INT_TYPE
        } else if (from == Type.getType(Boolean.class) || from == Type.BOOLEAN_TYPE) {
            return Type.BOOLEAN_TYPE
        } else if (from == Type.getType(Character.class) || from == Type.CHAR_TYPE) {
            return Type.CHAR_TYPE
        } else if (from == Type.getType(Byte.class) || from == Type.BYTE_TYPE) {
            return Type.BYTE_TYPE
        } else if (from == Type.getType(Short.class) || from == Type.SHORT_TYPE) {
            return Type.SHORT_TYPE
        } else {
            return from
        }
    }

    def box(Type type) {
        smartCast(type)
        if (type == Type.DOUBLE_TYPE) {
            invokeStatic("java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false)
        } else if (type == Type.FLOAT_TYPE) {
            invokeStatic("java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false)
        } else if (type == Type.LONG_TYPE) {
            invokeStatic("java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false)
        } else if (type == Type.INT_TYPE) {
            invokeStatic("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false)
        } else if (type == Type.BOOLEAN_TYPE) {
            invokeStatic("java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false)
        } else if (type == Type.CHAR_TYPE) {
            invokeStatic("java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false)
        } else if (type == Type.BYTE_TYPE) {
            invokeStatic("java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false)
        } else if (type == Type.SHORT_TYPE) {
            invokeStatic("java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false)
        }
    }

    def unbox(Type type) {
        cast(boxType(type))
        if (type == Type.DOUBLE_TYPE) {
            invokeVirtual("java/lang/Double", "doubleValue", "()D", false)
        } else if (type == Type.FLOAT_TYPE) {
            invokeVirtual("java/lang/Float", "floatValue", "()F", false)
        } else if (type == Type.LONG_TYPE) {
            invokeVirtual("java/lang/Long", "longValue", "()J", false)
        } else if (type == Type.INT_TYPE) {
            invokeVirtual("java/lang/Integer", "intValue", "()I", false)
        } else if (type == Type.BOOLEAN_TYPE) {
            invokeVirtual("java/lang/Boolean", "booleanValue", "()Z", false)
        } else if (type == Type.CHAR_TYPE) {
            invokeVirtual("java/lang/Character", "charValue", "()C", false)
        } else if (type == Type.BYTE_TYPE) {
            invokeVirtual("java/lang/Byte", "byteValue", "()B", false)
        } else if (type == Type.SHORT_TYPE) {
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
        if (pop != Type.INT_TYPE && pop != Type.SHORT_TYPE && pop != Type.BYTE_TYPE)
            throw new RuntimeException("Expected stack int, got " + pop)
        mv.visitTypeInsn(ANEWARRAY, type.internalName)

        context.push(Type.getType("[" + type.descriptor))
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
        context.pop(Type.INT_TYPE)

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(IRETURN)
    }

    def returnShort() {
        Context context = context
        context.pop(Type.SHORT_TYPE)

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(IRETURN)
    }

    def returnByte() {
        Context context = context
        context.pop(Type.BYTE_TYPE)

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(IRETURN)
    }

    def returnChar() {
        Context context = context
        context.pop(Type.CHAR_TYPE)

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn IRETURN
    }

    def returnBoolean() {
        Context context = context
        context.pop(Type.BOOLEAN_TYPE)

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(IRETURN)
    }

    def returnFloat() {
        Context context = context
        context.pop(Type.FLOAT_TYPE)

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(FRETURN)
    }

    def returnDouble() {
        Context context = context
        context.pop(Type.DOUBLE_TYPE)

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(DRETURN)
    }

    def returnLong() {
        Context context = context
        context.pop(Type.LONG_TYPE)

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(LRETURN)
    }

    def returnFloatValues() {
        Context context = context
        context.pop(Type.FLOAT_TYPE)

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(FRETURN)
    }

    def pushZeroInt() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(ICONST_0)
        context.push(Type.INT_TYPE)
    }

    def pushOneInt() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(ICONST_1)
        context.push(Type.INT_TYPE)
    }

    def pushFalse() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(ICONST_0)
        context.push(Type.BOOLEAN_TYPE)
    }

    def pushTrue() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(ICONST_1)
        context.push(Type.BOOLEAN_TYPE)
    }

    def pushBoolean(Boolean b) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitInsn(b ? ICONST_1 : ICONST_0)
        context.push(Type.BOOLEAN_TYPE)
    }

    def pushInt(Integer i) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitIntInsn(BIPUSH, i)
        context.push(Type.INT_TYPE)
    }

    def pushLong(Long l) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitLdcInsn(l)
        context.push(Type.LONG_TYPE)
    }

    def pushFloat(Float f) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitLdcInsn(f)
        context.push(Type.FLOAT_TYPE)
    }

    def pushDouble(Double d) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitLdcInsn(d)
        context.push(Type.DOUBLE_TYPE)
    }

    def pushString(String s) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitLdcInsn(s)
        context.push(Type.getType(String.class))
    }

    def pushChar(Character c) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitLdcInsn(c)
        context.push(Type.CHAR_TYPE)
    }

    def pushByte(Byte b) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitLdcInsn(b)
        context.push(Type.BYTE_TYPE)
    }

    def pushShort(Short s) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitLdcInsn(s)
        context.push(Type.SHORT_TYPE)
    }

    def loadConstant(Object value) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        if (value instanceof Integer) {
            mv.visitLdcInsn(value)
            context.push(Type.INT_TYPE)
        } else if (value instanceof Long) {
            mv.visitLdcInsn(value)
            context.push(Type.LONG_TYPE)
        } else if (value instanceof Float) {
            mv.visitLdcInsn(value)
            context.push(Type.FLOAT_TYPE)
        } else if (value instanceof Double) {
            mv.visitLdcInsn(value)
            context.push(Type.DOUBLE_TYPE)
        } else if (value instanceof String) {
            mv.visitLdcInsn(value)
            context.push(Type.getType(String.class))
        } else if (value instanceof Character) {
            mv.visitLdcInsn(value)
            context.push(Type.CHAR_TYPE)
        } else if (value instanceof Byte) {
            mv.visitLdcInsn(value)
            context.push(Type.BYTE_TYPE)
        } else if (value instanceof Short) {
            mv.visitLdcInsn(value)
            context.push(Type.SHORT_TYPE)
        } else if (value instanceof Boolean) {
            mv.visitLdcInsn(value)
            context.push(Type.BOOLEAN_TYPE)
        } else {
            if (value != null) {
                throw new RuntimeException("Unsupported constant owner: " + value.class)
            }
        }
    }

    def smartCast(Type to) {
        Type pop = context.peek()
        if ((pop.sort != Type.OBJECT && pop.sort != Type.ARRAY) || (to.sort != Type.OBJECT && to.sort != Type.ARRAY))
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
        context.push(Type.getType(Object.class))
    }

    def createArray(List<PyExpression> arguments, Type type) {
        pushStackByte(arguments.size())
        newArray(type)
        for (int i = 0, argumentsSize = arguments.size(); i < argumentsSize; i++) {
            PyExpression argument = arguments.get(i)
            dup()
            pushStackByte(i)
            argument.write(pc, this)
            if (context.peek().sort != Type.OBJECT && argument.type.sort != Type.ARRAY) {
                box(argument.type)
            }
            if (context.peek() != type) {
                cast(type)
            }

            arrayStoreObject(type)
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

    def dynamicDelItem() {
        invokeDynamic("__delitem__", "(Ljava/lang/Object;Ljava/lang/Object;)V")
    }

    def dynamicEq() {
        invokeDynamic("__eq__", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    }

    def dynamicNe() {
        invokeDynamic("__ne__", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    }

    def dynamicLt() {
        invokeDynamic("__lt__", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    }

    def dynamicLe() {
        invokeDynamic("__le__", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    }

    def dynamicGt() {
        invokeDynamic("__gt__", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    }

    def dynamicGe() {
        invokeDynamic("__ge__", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
    }

    def dynamicCall() {
        invokeDynamic("__call__", "(Ljava/lang/Object;[Ljava/lang/Object;Ljava/util/Map;)Ljava/lang/Object;")
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
        createArray(arguments, Type.getType(Object.class))
        createKwargs()
        dynamicCall()
    }

    def returnValue(Type type, Location location) {
        Context context = context

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        if (type == Type.VOID_TYPE) {
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
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
            case Type.CHAR:
            case Type.BOOLEAN:
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
            case Type.OBJECT:
                mv.visitInsn(ARETURN)
                break
            default:
                throw new RuntimeException("Unknown return type: " + type)
        }
    }

    def loadClass(JvmClass jvmClass) {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        Type type = jvmClass.type
        mv.visitLdcInsn(type)
        Context context = context
        context.push(Type.getType(Class.class))
    }

    def createKwargs() {
        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
        mv.visitMethodInsn(INVOKESTATIC, "java/util/Collections", "emptyMap", "()Ljava/util/Map;", false)
        Context context = context
        context.push(Type.getType(Map.class))
    }

    def writeArgs(List<PyExpression> arguments, Type[] types) {
        for (int i = 0, argumentsSize = arguments.size(); i < argumentsSize; i++) {
            PyExpression argument = arguments.get(i)
            argument.write(pc, this)
            if (types[i] == null || types[i].sort == Type.OBJECT) {
                if (argument.type.sort != Type.ARRAY && argument.type.sort != Type.OBJECT) {
                    pc.writer.box(argument.type)
                }
            }

            if (types[i] != null) {
                if (types[i].sort != Type.OBJECT && types[i].sort != Type.ARRAY) {
                    pc.writer.unbox(types[i])
                }
                pc.writer.cast(types[i])
            } else {
                pc.writer.cast(Type.getType(Object.class))
            }
        }
    }

    def loadValue(int index, Type type) {
        switch (type.sort) {
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
            case Type.CHAR:
            case Type.BOOLEAN:
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
            case Type.OBJECT:
            case Type.ARRAY:
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
        context.push(Type.getType(Object.class))
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
        newObject(Type.getType(HashMap.class))
        dup()
        invokeSpecial(Type.getType(HashMap.class), "<init>", methodType(Type.VOID_TYPE), false)

        List<Map.Entry<String, PyExpression>> copyOf = List.copyOf(kwargs.entrySet())
        for (int i = 0, copyOfSize = copyOf.size(); i < copyOfSize; i++) {
            Map.Entry<String, PyExpression> entry = copyOf.get(i)
            String s = entry.key
            PyExpression expression = entry.value
            if (i < copyOfSize - 1)
                dup()
            expressions.add(new ConstantExpr(s, expression.location))
            expressions.add(expression)

            invokeVirtual(Type.getType(HashMap.class), "put", methodType(Type.getType(Object.class), Type.getType(Object.class), Type.getType(Object.class)), false)
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

        var mv = pc.methodOut == null ? pc.rootInitMv : pc.methodOut
//        Label label = new Label()
//        if (mv != null) {
//            mv.visitLabel(label)
//            mv.visitLineNumber(location.lineStart, label)
//        }

//        this.lastLabel = label
    }

    Label newLabel() {
        return new Label()
    }
}
