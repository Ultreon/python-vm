package dev.ultreon.pythonc;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public final class FuncCall implements Symbol {
    public static final String E_CLASS_NOT_IN_CP = "Class not found in classpath (yet), maybe the class isn't initialized yet: %s (%s)";
    final List<PyExpr> arguments;
    private final @Nullable PyExpr parent;
    private Method method;
    private JvmConstructor constructor;
    private boolean kwargs;
    private boolean varArgs;
    private boolean dynCtor;
    private Type owner;
    private Type[] paramTypes;
    private JvmFunction function;
    private BitSet boxing;
    private final Location location;
    private Type[] argumentTypes;
    private Type returnType;
    private String name;

    public FuncCall(@Nullable PyExpr parent, List<PyExpr> arguments, String name, Location location) {
        this.parent = parent;
        this.arguments = arguments;
        this.location = location;
        this.name = name;
    }

    public void write(MethodVisitor mv, PythonCompiler compiler) {
        if (parent instanceof PyObjectRef pyObjectRef) {
            // Print the array
            Symbol symbol = compiler.symbols.get(pyObjectRef.name());
            if (symbol == null) {
                throw new CompilerException("Symbol '" + name() + "' not found (" + compiler.getLocation(this) + ")");
            }
            String named = symbol.name();
            Type owner = symbol.type(compiler);
            if (symbol instanceof PyObjectRef(String name, Location location)) {
                symbol = compiler.symbols.get(name);
                switch (symbol) {
                    case PyBuiltinFunction builtinFunction -> {
                        this.varArgs = builtinFunction.varArgs;
                        this.kwargs = builtinFunction.kwargs;
                        this.dynCtor = builtinFunction.dynCall;

                        if (dynCtor) {
                            compiler.flags.set(PythonCompiler.F_DYN_CALL);
                            try {
                                setupCallArgs(mv, compiler);
                            } finally {
                                compiler.flags.clear(PythonCompiler.F_DYN_CALL);
                            }

                            // Create Object Array
                            createArray(mv, compiler, arguments, new Object[(arguments).size()]);

                            // Kwargs
                            // Create Map<String, Object>
                            // Call Map.of()
                            compiler.writer.invokeStatic("java/util/Map", "of", "()Ljava/util/Map;", true);
                            compiler.writer.dynamicBuiltinCall(builtinFunction.name, "([Ljava/lang/Object;Ljava/util/Map;)V");

                            return;
                        }

                        if (symbol.isClass()) {
                            Type mapOwner = builtinFunction.mapOwner;
                            setupCallArgs(mv, compiler);

                            compiler.writer.invokeStatic(mapOwner.getInternalName(), name(), "(" + argDesc(compiler, arguments) + ")" + type(compiler), false);
                            return;
                        } else {
                            throw new RuntimeException("Unexpected builtin function call!");
                        }
                    }
                    case JClass jClass -> {
                        if (parent == null) {
                            // Constructor Call
                            Context context = compiler.getContext(Context.class);
                            final Type finalOwner = owner;
                            setupCallArgs(mv, compiler);
                            compiler.writer.newInstance(owner.getInternalName(), "<init>", "(" + argDesc(compiler, arguments) + ")V", false, () -> {
                                installArgs(mv, compiler, finalOwner);
                            });
                            return;
                        }

                        compiler.flags.set(PythonCompiler.F_DYN_CALL);
                        try {
                            setupCallArgs(mv, compiler);
                        } finally {
                            compiler.flags.clear(PythonCompiler.F_DYN_CALL);
                        }

                        if (arguments == null) {
                            throw new RuntimeException("Call args not found for function: " + location);
                        }

                        Type type = jClass.type(compiler);
                        installArgs(mv, compiler, type);

                        boolean isInterface = jClass.isInterface(compiler);
                        compiler.writer.invokeStatic(owner.getInternalName(), name(), "(" + Arrays.stream(paramTypes).map(Type::getDescriptor).collect(Collectors.joining("")) + ")" + type(compiler), isInterface);
                        return;
                    }
                    case PyVariable pyVariable -> {
                        symbol.load(mv, compiler, symbol.preload(mv, compiler, false), false);
                        setupCallArgs(mv, compiler);

                        installArgs(mv, compiler, pyVariable.type(compiler));

                        String s = argDesc(compiler, arguments);
                        if (owner.getSort() == Type.OBJECT) {
                            String internalName = owner.getInternalName();
                            Symbol symbol1 = PythonCompiler.classCache.require(compiler, owner);
                            if (symbol1 == null) {
                                throw new CompilerException("Symbol '" + owner.getClassName() + "' not found (" + compiler.getLocation(this) + ")");
                            }

                            if (symbol1 instanceof JClass jClass) {
                                owner = jClass.type(compiler);
                                if (owner.getSort() == Type.OBJECT) {
                                    String internal = owner.getInternalName();
                                    String qualified = internal.replace("/", ".");
                                    Class<?> aClass = null;
                                    try {
                                        aClass = Class.forName(qualified, false, getClass().getClassLoader());
                                        if (aClass.isInterface()) {
                                            compiler.writer.invokeInterface(internal, name(), "(" + s + ")" + type(compiler), true);
                                            return;
                                        }
                                    } catch (ClassNotFoundException e) {
                                        throw new CompilerException("JVM Class '" + qualified + "' not found (" + compiler.getLocation(this) + ")");
                                    }
                                }
                            } else if (symbol1 instanceof PyBuiltinFunction func) {
                                owner = func.owner(compiler).type(compiler);
                            } else if (symbol1 instanceof PyImport pyImport) {
                                symbol1 = pyImport.symbol;
                                if (symbol1 instanceof JClass jClass) {
                                    owner = jClass.type(compiler);
                                    if (owner.getSort() == Type.OBJECT) {
                                        String internal = owner.getInternalName();
                                        String qualified = internal.replace("/", ".");
                                        Class<?> aClass = null;
                                        try {
                                            aClass = Class.forName(qualified, false, getClass().getClassLoader());
                                            if (aClass.isInterface()) {
                                                compiler.writer.invokeInterface(internal, name(), "(" + s + ")" + type(compiler), true);
                                                return;
                                            }
                                        } catch (ClassNotFoundException e) {
                                            throw new CompilerException("JVM Class '" + qualified + "' not found (" + compiler.getLocation(this) + ")");
                                        }
                                    }
                                } else if (symbol1 instanceof PyBuiltinFunction func) {
                                    owner = func.owner(compiler).type(compiler);
                                } else if (symbol1 instanceof JvmClass jvmClass) {
                                    owner = jvmClass.type(compiler);
                                } else {
                                    throw new RuntimeException("Unknown symbol owner: " + symbol1.getClass().getSimpleName());
                                }
                            } else {
                                throw new RuntimeException("Unknown symbol owner: " + symbol1.getClass().getSimpleName());
                            }
                        } else {
                            throw new RuntimeException("Unknown JVM descriptor for class typedName: " + owner);
                        }
                        compiler.writer.invokeVirtual(owner.getInternalName(), name(), "(" + s + ")" + type(compiler), false);
                    }
                    case PyClass pyClass -> {
                        Type owner1 = owner(compiler);
                        installArgs(mv, compiler, owner1);
                        compiler.writer.newInstance(owner1.getInternalName(), "<init>", "(" + argDesc(compiler, arguments) + ")V", false, () -> setupCallArgs(mv, compiler));
                        return;
                    }
                    case PyImport pyImport -> {
                        Type type = pyImport.type(compiler);
                        compiler.flags.set(PythonCompiler.F_DYN_CALL);
                        setupCallArgs(mv, compiler);
                        compiler.flags.clear(PythonCompiler.F_DYN_CALL);
                        String ourName = this.name();
                        Symbol ourSymbol = compiler.symbols.get(ourName);
                        if (ourSymbol instanceof JvmClass) {
                            ourName = "<init>";
                        } else if (ourSymbol instanceof PyImport pyImport1 && pyImport1.symbol instanceof JvmClass) {
                            ourName = "<init>";
                        }
                        if (!installArgs(mv, compiler, type, true)) {
                            throw new CompilerException("Function '" + ourName + "' not found in '" + pyImport.type(compiler).getClassName() + "' with arguments (" + argNames(compiler, arguments) + ") (" + compiler.getLocation(this) + ")");
                        }
                        Type[] args = argumentTypes(compiler);
                        JvmFunction jvmFunction = pyImport.function(compiler, ourName, paramTypes);
                        if (jvmFunction == null) {
                            throw new CompilerException("Function '" + ourName + "' not found in '" + pyImport.type(compiler).getClassName(), compiler.getLocation(this));
                        }

                        if (jvmFunction instanceof JvmConstructor constructor) {
                            constructor.write(mv, compiler, () -> writeArgs(mv, compiler, arguments, boxing));
                        } else if (!ourName.equals("<init>")) {
                            writeArgs(mv, compiler, arguments, boxing);
                            jvmFunction.write(mv, compiler);
                        } else {
                            throw new CompilerException("Constructor with arguments (" + argNames(compiler, arguments) + ") not found in '" + pyImport.type(compiler).getClassName(), compiler.getLocation(this));
                        }
//                        return;
                    }
                    case JvmConstructor jvmConstructor -> {
                        setupCallArgs(mv, compiler);

                        jvmConstructor.write(mv, compiler, () -> writeArgs(mv, compiler, arguments, boxing));
                        return;
                    }
                    case JvmFunction jvmFunction -> {
                        setupCallArgs(mv, compiler);
                        installArgs(mv, compiler, jvmFunction.owner(compiler).type(compiler), true);

                        if (jvmFunction instanceof JvmConstructor constructor) {
                            constructor.write(mv, compiler, () -> writeArgs(mv, compiler, arguments, boxing));
                        } else {
                            writeArgs(mv, compiler, arguments, boxing);
                            if (compiler.compilingClass.doesInherit(compiler, jvmFunction.ownerClass(compiler))) {
                                compiler.writer.loadThis(compiler, compiler.compilingClass);
                            }
                            jvmFunction.write(mv, compiler);
                        }
                        return;
                    }
                    case JvmClass jvmClass -> {
                        setupCallArgs(mv, compiler);
                        installArgs(mv, compiler, jvmClass.type(compiler), true);

                        jvmClass.constructor(compiler, paramTypes).write(mv, compiler, () -> writeArgs(mv, compiler, arguments, boxing));
                        return;
                    }
                    case null, default ->
                            throw new UnsupportedOperationException("Not implemented: " + symbol.getClass().getSimpleName());
                }

                return;
            }
            if (symbol instanceof PyImport pyImport) {
                pyImport.invoke(mv, compiler, "(" + argDesc(compiler, arguments) + ")" + pyImport.type(compiler).getDescriptor(), arguments, () -> setupCallArgs(mv, compiler));
                return;
            } else if (symbol instanceof PyBuiltinFunction) {
//                    symbol.load(mv, compiler, null, false);
                PythonCompiler.throwError(mv, "Unimplemented function call: " + symbol.getClass().getName());
                return;
            } else if (symbol instanceof JClass) {
                setupCallArgs(mv, compiler);
                compiler.writer.invokeStatic(owner.getInternalName(), name(), "(" + (arguments == null ? "" : argDesc(compiler, arguments)) + ")V", false);
            } else if (symbol instanceof PyVariable pyVariable) {
                symbol.load(mv, compiler, symbol.preload(mv, compiler, false), false);
                setupCallArgs(mv, compiler);

                installArgs(mv, compiler, pyVariable.type(compiler));

                String s = argDesc(compiler, arguments);
                if (owner.getSort() == Type.OBJECT) {
                    String internalName = owner.getInternalName();
                    Symbol symbol1 = compiler.symbols.get(internalName.substring(internalName.lastIndexOf("/") + 1));
                    if (symbol1 == null) {
                        throw new CompilerException("Symbol '" + owner.getClassName() + "' not found (" + compiler.getLocation(this) + ")");
                    }

                    if (symbol1 instanceof JClass jClass) {
                        owner = jClass.type(compiler);
                        if (owner.getSort() == Type.OBJECT) {
                            String internal = owner.getInternalName();
                            String qualified = internal.replace("/", ".");
                            Class<?> aClass = null;
                            try {
                                aClass = Class.forName(qualified, false, getClass().getClassLoader());
                                if (aClass.isInterface()) {
                                    compiler.writer.invokeInterface(internal, name(), "(" + s + ")" + type(compiler), true);
                                    return;
                                }
                            } catch (ClassNotFoundException e) {
                                throw new CompilerException("JVM Class '" + qualified + "' not found (" + compiler.getLocation(this) + ")");
                            }
                        }
                    } else if (symbol1 instanceof PyBuiltinFunction func) {
                        owner = func.owner(compiler).type(compiler);
                    } else {
                        throw new RuntimeException("Unknown symbol owner: " + symbol1.getClass().getSimpleName());
                    }
                } else {
                    throw new RuntimeException("Unknown JVM descriptor for class typedName: " + owner);
                }
                compiler.writer.invokeVirtual(owner.getInternalName(), name(), "(" + s + ")" + type(compiler), false);
            } else {
                throw new UnsupportedOperationException("Not implemented: " + symbol.getClass().getSimpleName());
            }
        } else if (parent instanceof JvmConstructor constructor) {
            setupCallArgs(mv, compiler);
            installArgs(mv, compiler, constructor.owner(compiler).type(compiler), true);
            constructor.write(mv, compiler, () -> writeArgs(mv, compiler, arguments, boxing));
        } else if (parent instanceof JvmFunction func) {
            setupCallArgs(mv, compiler);
            installArgs(mv, compiler, func.owner(compiler).type(compiler));
            func.write(mv, compiler);
        } else if (parent instanceof FuncCall func) {
            FuncCall funcCall = (FuncCall) parent;
            funcCall.write(mv, compiler);

            Type type = func.type(compiler);
            if (!compiler.classes.hasClassName(type.getClassName())) {
                try {
                    Class<?> aClass = Class.forName(type.getClassName(), false, getClass().getClassLoader());
                    Stack<Class<?>> stack = new Stack<>();
                    stack.push(aClass);
                    while (!stack.isEmpty()) {
                        find_method:
                        for (Method method : aClass.getMethods()) {
                            if (!method.getName().equals(name())) continue;
                            if (method.getParameterTypes().length != (arguments == null ? 0 : arguments.size()))
                                continue;
                            Class<?>[] parameterTypes = method.getParameterTypes();
                            for (int i = 0; i < parameterTypes.length; i++) {
                                if (!parameterTypes[i].isAssignableFrom(Class.forName((arguments).get(i).type(compiler).getClassName(), false, getClass().getClassLoader()))) {
                                    continue find_method;
                                }
                            }
                            if (method.getDeclaringClass().isInterface()) {
                                compiler.writer.invokeInterface(type.getInternalName(), name(), Type.getType(method).getDescriptor(), true);
                                return;
                            }
                            compiler.writer.invokeVirtual(type.getInternalName(), name(), Type.getType(method).getDescriptor(), false);
                            return;
                        }
                        for (Class<?> aClass1 : aClass.getInterfaces()) {
                            stack.push(aClass1);
                        }
                        aClass = stack.pop();
                    }

                    throw compiler.functionNotFound(type, name(), this);
                } catch (ClassNotFoundException e) {
                    throw compiler.jvmClassNotFound(type.getClassName(), this);
                }
            }

            owner = type;

            setupArgs(mv, compiler, arguments);

            compiler.writer.invokeVirtual(owner(compiler).getInternalName(), name(), "(" + argDesc(compiler, arguments) + ")" + type(compiler), false);
            // Print the array
            //                throw new RuntimeException("atom not supported at: " + atom.getText());
            //                compiler.invokeDynamic(owner(compiler), typedName(), "(" + compiler.visit(arguments) + ")V", false);
            // Throw exception with error message

//                throwError(mv, "Unimplemented function call: atom=" + atom.getText() + ", primaryContext=" + primaryContext.getText() + ", arguments=" + location);
        } else if (parent instanceof MemberFuncCall call) {
            setupCallArgs(mv, compiler);
            Object preload = call.preload(mv, compiler, false);
            installArgs(mv, compiler, call.owner(compiler));
            call.load(mv, compiler, preload, false);
        } else {
            throw new RuntimeException("No supported matching atom found for: " + parent.getClass().getName());
        }

        compiler.writer.lineNumber(location().lineEnd());
    }

    public Type[] argumentTypes(PythonCompiler compiler) {
        if (argumentTypes != null) return argumentTypes;

        Type[] argumentTypes = new Type[arguments.size()];
        for (int i = 0; i < arguments.size(); i++) {
            argumentTypes[i] = arguments.get(i).type(compiler);
        }
        this.argumentTypes = argumentTypes;
        return argumentTypes;
    }

    public Type returnType(PythonCompiler compiler) {
        if (returnType != null) return returnType;

        returnType = type(compiler);
        return returnType;
    }

    private boolean installArgs(MethodVisitor mv, PythonCompiler compiler, Type type) {
        return installArgs(mv, compiler, type, false);
    }

    private boolean installArgs(MethodVisitor mv, PythonCompiler compiler, Type type, boolean simulate) {
        JvmClass clazz = PythonCompiler.classCache.require(compiler, type);
        if (clazz == null) {
            throw compiler.jvmClassNotFound(type.getClassName(), this);
        }

        String name = name();
        if (compiler.getClassSymbol(name) != null) {
            name = "<init>";
        }
        if (name.equals("<init>")) {
            JvmClass aClass = clazz;
            Stack<JvmClass> stack = new Stack<>();
            stack.push(aClass);
            List<PyExpr> args = arguments;
            while (!stack.isEmpty()) {
                JvmConstructor[] jvmFunctions = aClass.constructors(compiler);
                if (jvmFunctions != null) {
                    find_constructor:
                    for (JvmConstructor constructor : jvmFunctions) {
                        if (constructor.parameterClasses(compiler).length != (arguments.size()))
                            continue;
                        JvmClass[] parameterTypes = constructor.parameterClasses(compiler);
                        BitSet boxing = new BitSet(parameterTypes.length);
                        for (int i = 0; i < parameterTypes.length; i++) {
                            Type type1 = arguments.get(i).type(compiler);
                            JvmClass require = PythonCompiler.classCache.require(compiler, type1);
                            if (!parameterTypes[i].isPrimitive()) {
                                boxing.set(i);
                                if (type1.equals(Type.BYTE_TYPE) && (parameterTypes[i].type(compiler).getSort() != Type.OBJECT && !parameterTypes[i].type(compiler).getInternalName().equals("java/lang/Byte"))) {
                                    continue find_constructor;
                                } else if (type1.equals(Type.SHORT_TYPE) && (parameterTypes[i].type(compiler).getSort() != Type.OBJECT && !parameterTypes[i].type(compiler).getInternalName().equals("java/lang/Short"))) {
                                    continue find_constructor;
                                } else if (type1.equals(Type.CHAR_TYPE) && (parameterTypes[i].type(compiler).getSort() != Type.OBJECT && !parameterTypes[i].type(compiler).getInternalName().equals("java/lang/Character"))) {
                                    continue find_constructor;
                                } else if (type1.equals(Type.INT_TYPE) && (parameterTypes[i].type(compiler).getSort() != Type.OBJECT && !parameterTypes[i].type(compiler).getInternalName().equals("java/lang/Integer"))) {
                                    continue find_constructor;
                                } else if (type1.equals(Type.LONG_TYPE) && (parameterTypes[i].type(compiler).getSort() != Type.OBJECT && !parameterTypes[i].type(compiler).getInternalName().equals("java/lang/Long"))) {
                                    continue find_constructor;
                                } else if (type1.equals(Type.FLOAT_TYPE) && (parameterTypes[i].type(compiler).getSort() != Type.OBJECT && !parameterTypes[i].type(compiler).getInternalName().equals("java/lang/Float"))) {
                                    continue find_constructor;
                                } else if (type1.equals(Type.DOUBLE_TYPE) && (parameterTypes[i].type(compiler).getSort() != Type.OBJECT && !parameterTypes[i].type(compiler).getInternalName().equals("java/lang/Double"))) {
                                    continue find_constructor;
                                } else if (type1.equals(Type.BOOLEAN_TYPE) && (parameterTypes[i].type(compiler).getSort() != Type.OBJECT && !parameterTypes[i].type(compiler).getInternalName().equals("java/lang/Boolean"))) {
                                    continue find_constructor;
                                } else if (type1.getSort() == Type.OBJECT && !require.doesInherit(compiler, parameterTypes[i].type(compiler))) {
                                    continue find_constructor;
                                } else if (type1.getSort() == Type.ARRAY && !parameterTypes[i].doesInherit(compiler, compiler.writer.boxType(type1))) {
                                    continue find_constructor;
                                }
                            } else {
                                boxing.clear(i);
                                if (type1.equals(Type.BYTE_TYPE) && parameterTypes[i].type(compiler).getSort() != Type.BYTE) {
                                    continue find_constructor;
                                } else if (type1.equals(Type.SHORT_TYPE) && parameterTypes[i].type(compiler).getSort() != Type.SHORT) {
                                    continue find_constructor;
                                } else if (type1.equals(Type.CHAR_TYPE) && parameterTypes[i].type(compiler).getSort() != Type.CHAR) {
                                    continue find_constructor;
                                } else if (type1.equals(Type.INT_TYPE) && parameterTypes[i].type(compiler).getSort() != Type.INT) {
                                    continue find_constructor;
                                } else if (type1.equals(Type.LONG_TYPE) && parameterTypes[i].type(compiler).getSort() != Type.LONG) {
                                    continue find_constructor;
                                } else if (type1.equals(Type.FLOAT_TYPE) && parameterTypes[i].type(compiler).getSort() != Type.FLOAT) {
                                    continue find_constructor;
                                } else if (type1.equals(Type.DOUBLE_TYPE) && parameterTypes[i].type(compiler).getSort() != Type.DOUBLE) {
                                    continue find_constructor;
                                } else if (type1.equals(Type.BOOLEAN_TYPE) && parameterTypes[i].type(compiler).getSort() != Type.BOOLEAN) {
                                    continue find_constructor;
                                } else if (!require.doesInherit(compiler, parameterTypes[i])) {
                                    continue find_constructor;
                                }
                            }
                        }
                        this.boxing = boxing;

                        if (!simulate) writeArgs(mv, compiler, args, boxing);

                        this.paramTypes = Arrays.stream(parameterTypes).map(v -> v.type(compiler)).toList().toArray(Type[]::new);
                        return true;
                    }
                }
                if (aClass.firstSuperClass(compiler) != null) stack.push(aClass.firstSuperClass(compiler));
                for (JvmClass aClass1 : aClass.interfaces(compiler)) {
                    stack.push(aClass1);
                }
                aClass = stack.pop();
            }

            throw compiler.functionNotFound(aClass.type(compiler), "<init>", args.stream().map(v -> v.type(compiler)).toList().toArray(Type[]::new), this);
        }

        JvmClass aClass = clazz;
        Stack<JvmClass> stack = new Stack<>();
        stack.push(aClass);
        List<PyExpr> args = arguments;
        while (!stack.isEmpty()) {
            Map<String, List<JvmFunction>> methods = aClass.methods(compiler);
            if (methods != null) {
                List<JvmFunction> jvmFunctions = methods.get(name);
                if (jvmFunctions != null) {
                    find_method:
                    for (JvmFunction method : jvmFunctions) {
                        if (method.parameterClasses(compiler).length != (arguments.size())) {
                            continue;
                        }
                        JvmClass[] parameterTypes = method.parameterClasses(compiler);
                        BitSet boxing = new BitSet(parameterTypes.length);
                        for (int i = 0; i < parameterTypes.length; i++) {
                            Type type1 = arguments.get(i).type(compiler);
                            JvmClass require = PythonCompiler.classCache.require(compiler, type1);
                            if (!parameterTypes[i].isPrimitive()) {
                                boxing.set(i);
                                if (type1.equals(Type.BYTE_TYPE) && (compiler.writer.boxType(parameterTypes[i].type(compiler)).getSort() != Type.OBJECT && !compiler.writer.boxType(parameterTypes[i].type(compiler)).getInternalName().equals("java/lang/Byte"))) {
                                    continue find_method;
                                } else if (type1.equals(Type.SHORT_TYPE) && (compiler.writer.boxType(parameterTypes[i].type(compiler)).getSort() != Type.OBJECT && !compiler.writer.boxType(parameterTypes[i].type(compiler)).getInternalName().equals("java/lang/Short"))) {
                                    continue find_method;
                                } else if (type1.equals(Type.CHAR_TYPE) && (compiler.writer.boxType(parameterTypes[i].type(compiler)).getSort() != Type.OBJECT && !compiler.writer.boxType(parameterTypes[i].type(compiler)).getInternalName().equals("java/lang/Character"))) {
                                    continue find_method;
                                } else if (type1.equals(Type.INT_TYPE) && (compiler.writer.boxType(parameterTypes[i].type(compiler)).getSort() != Type.OBJECT && !compiler.writer.boxType(parameterTypes[i].type(compiler)).getInternalName().equals("java/lang/Integer"))) {
                                    continue find_method;
                                } else if (type1.equals(Type.LONG_TYPE) && (compiler.writer.boxType(parameterTypes[i].type(compiler)).getSort() != Type.OBJECT && !compiler.writer.boxType(parameterTypes[i].type(compiler)).getInternalName().equals("java/lang/Long"))) {
                                    continue find_method;
                                } else if (type1.equals(Type.FLOAT_TYPE) && (compiler.writer.boxType(parameterTypes[i].type(compiler)).getSort() != Type.OBJECT && !compiler.writer.boxType(parameterTypes[i].type(compiler)).getInternalName().equals("java/lang/Float"))) {
                                    continue find_method;
                                } else if (type1.equals(Type.DOUBLE_TYPE) && (compiler.writer.boxType(parameterTypes[i].type(compiler)).getSort() != Type.OBJECT && !compiler.writer.boxType(parameterTypes[i].type(compiler)).getInternalName().equals("java/lang/Double"))) {
                                    continue find_method;
                                } else if (type1.equals(Type.BOOLEAN_TYPE) && (compiler.writer.boxType(parameterTypes[i].type(compiler)).getSort() != Type.OBJECT && !compiler.writer.boxType(parameterTypes[i].type(compiler)).getInternalName().equals("java/lang/Boolean"))) {
                                    continue find_method;
                                } else if (type1.getSort() == Type.OBJECT && !require.doesInherit(compiler, compiler.writer.boxType(parameterTypes[i].type(compiler)))) {
                                    continue find_method;
                                } else if (type1.getSort() == Type.ARRAY && !parameterTypes[i].doesInherit(compiler, compiler.writer.boxType(type1))) {
                                    continue find_method;
                                }
                            } else {
                                boxing.clear(i);
                                if (type1.equals(Type.BYTE_TYPE) && parameterTypes[i].type(compiler).getSort() != Type.BYTE) {
                                    continue find_method;
                                } else if (type1.equals(Type.SHORT_TYPE) && parameterTypes[i].type(compiler).getSort() != Type.SHORT) {
                                    continue find_method;
                                } else if (type1.equals(Type.CHAR_TYPE) && parameterTypes[i].type(compiler).getSort() != Type.CHAR) {
                                    continue find_method;
                                } else if (type1.equals(Type.INT_TYPE) && parameterTypes[i].type(compiler).getSort() != Type.INT) {
                                    continue find_method;
                                } else if (type1.equals(Type.LONG_TYPE) && parameterTypes[i].type(compiler).getSort() != Type.LONG) {
                                    continue find_method;
                                } else if (type1.equals(Type.FLOAT_TYPE) && parameterTypes[i].type(compiler).getSort() != Type.FLOAT) {
                                    continue find_method;
                                } else if (type1.equals(Type.DOUBLE_TYPE) && parameterTypes[i].type(compiler).getSort() != Type.DOUBLE) {
                                    continue find_method;
                                } else if (type1.equals(Type.BOOLEAN_TYPE) && parameterTypes[i].type(compiler).getSort() != Type.BOOLEAN) {
                                    continue find_method;
                                } else if (!require.doesInherit(compiler, parameterTypes[i])) {
                                    continue find_method;
                                }
                            }
                        }
                        this.boxing = boxing;

                        if (!simulate) writeArgs(mv, compiler, args, boxing);

                        this.paramTypes = Arrays.stream(parameterTypes).map(v -> v.type(compiler)).toList().toArray(Type[]::new);
                        return true;
                    }
                }
            }
            if (aClass.firstSuperClass(compiler) != null) stack.push(aClass.firstSuperClass(compiler));
            for (JvmClass aClass1 : aClass.interfaces(compiler)) {
                stack.push(aClass1);
            }
            aClass = stack.pop();
        }

        Type owner1 = owner(compiler);
        JvmClass ownerClass = PythonCompiler.classCache.require(compiler, owner1);
        if (ownerClass == null) {
            throw new RuntimeException("Class not found for " + owner1);
        }

        if (ownerClass instanceof PyClass) {
            PyClass pyClass = (PyClass) ownerClass;
            JvmFunction jvmFunction = pyClass.requireFunction(compiler, name, args.stream().map(v -> v.type(compiler)).toList().toArray(Type[]::new));
            if (jvmFunction != null) {
                this.function = jvmFunction;
                this.owner = owner1;
                return true;
            }
        }
        throw compiler.functionNotFound(type, name, args.stream().map(v -> v.type(compiler)).toList().toArray(Type[]::new), this);
    }

    private static void writeArgs(MethodVisitor mv, PythonCompiler compiler, List<PyExpr> args, BitSet boxing) {
        for (int i = 0, argsSize = args.size(); i < argsSize; i++) {
            PyExpr expr = args.get(i);
            boolean shouldBeBoxed = boxing.get(i);
            Object preloaded = expr.preload(mv, compiler, false);
            expr.load(mv, compiler, preloaded, false);

            Type type1 = expr.type(compiler);
            if (type1 == null) {
                throw new RuntimeException("Type not found for at " + compiler.getLocation(expr));
            }
            if (shouldBeBoxed) {
                if (type1 == Type.INT_TYPE) {
                    compiler.writer.invokeStatic("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                } else if (type1 == Type.LONG_TYPE) {
                    compiler.writer.invokeStatic("java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
                } else if (type1 == Type.FLOAT_TYPE) {
                    compiler.writer.invokeStatic("java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
                } else if (type1 == Type.DOUBLE_TYPE) {
                    compiler.writer.invokeStatic("java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                } else if (type1 == Type.CHAR_TYPE) {
                    compiler.writer.invokeStatic("java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
                } else if (type1 == Type.BOOLEAN_TYPE) {
                    compiler.writer.invokeStatic("java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
                } else if (type1 == Type.SHORT_TYPE) {
                    compiler.writer.invokeStatic("java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
                } else if (type1 == Type.BYTE_TYPE) {
                    compiler.writer.invokeStatic("java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
                }
            }
        }
    }

    private void setupCallArgs(MethodVisitor mv, PythonCompiler compiler) {
        setupArgs(mv, compiler, arguments);
    }

    @Deprecated
    private Type[] args(PythonCompiler compiler, Object arguments) {
        return argumentTypes(compiler);
    }

    private String argNames(PythonCompiler compiler, Object arguments) {
        if (arguments == null) return "";
        if (arguments == null) throw new RuntimeException("Call args not found at: " + location);
        if (paramTypes != null)
            return Arrays.stream(paramTypes).map(Type::getDescriptor).collect(Collectors.joining(""));
        return switch (arguments) {
            case List<?> list -> {
                StringJoiner joiner = new StringJoiner(", ");
                int listSize = list.size();
                if (varArgs) listSize--;
                if (kwargs) listSize--;
                for (int j = 0; j < listSize; j++) {
                    Object o = list.get(j);
                    String string = switch (o) {
                        case String s -> "str";
                        case Integer i -> "jint";
                        case Long i -> "int";
                        case Float i -> "jfloat";
                        case Double i -> "float";
                        case Boolean i -> "bool";
                        case PyVariable pyVar -> compiler.pyName(owner(compiler));
                        case PyObjectRef objectRef -> compiler.pyName(owner(compiler));
                        case FuncCall funcCallWithArgs -> compiler.pyName(funcCallWithArgs.type(compiler));
                        case PyConstant pyConstant -> compiler.pyName(pyConstant.type.type);
                        case PyExpr expr -> compiler.pyName(expr.type(compiler));
                        case null, default -> {
                            if (o != null) {
                                throw new RuntimeException("argument not supported with owner " + o.getClass().getSimpleName() + " at: " + location);
                            } else {
                                throw new RuntimeException("argument not supported with owner <NULL> at: " + location);
                            }
                        }
                    };
                    joiner.add(string);
                }
                if (varArgs) joiner.add("[Ljava/lang/Object;");
                if (kwargs) joiner.add("Ljava/util/Map;");
                yield joiner.toString();
            }
            default -> throw new RuntimeException("argument not supported at: " + location);
        };
    }

    private String argDesc(PythonCompiler compiler, Object arguments) {
        if (arguments == null) return "";
        if (arguments == null) throw new RuntimeException("Call args not found at: " + location);
        if (paramTypes != null)
            return Arrays.stream(paramTypes).map(Type::getDescriptor).collect(Collectors.joining(""));
        return switch (arguments) {
            case List<?> list -> {
                StringJoiner joiner = new StringJoiner("");
                int listSize = list.size();
                if (varArgs) listSize--;
                if (kwargs) listSize--;
                for (int j = 0; j < listSize; j++) {
                    Object o = list.get(j);
                    String string = switch (o) {
                        case String s -> "Ljava/lang/String;";
                        case Integer i -> "I";
                        case Long i -> "J";
                        case Float i -> "F";
                        case Double i -> "D";
                        case Boolean i -> "Z";
                        case PyVariable pyVar -> owner(compiler).getDescriptor();
                        case PyObjectRef objectRef -> owner(compiler).getDescriptor();
                        case FuncCall funcCallWithArgs -> funcCallWithArgs.type(compiler).getDescriptor();
                        case PyConstant pyConstant -> pyConstant.type.type.getDescriptor();
                        case PyExpr expr -> expr.type(compiler).getDescriptor();
                        case null, default -> {
                            if (o != null) {
                                throw new RuntimeException("argument not supported with owner " + o.getClass().getSimpleName() + " at: " + location);
                            } else {
                                throw new RuntimeException("argument not supported with owner <NULL> at: " + location);
                            }
                        }
                    };
                    joiner.add(string);
                }
                if (varArgs) joiner.add("[Ljava/lang/Object;");
                if (kwargs) joiner.add("Ljava/util/Map;");
                yield joiner.toString();
            }
            default -> throw new RuntimeException("argument not supported at: " + location);
        };
    }

    @Override
    public Type type(PythonCompiler compiler) {
        if (arguments == null) {
            compiler.flags.set(PythonCompiler.F_DYN_CALL);
            try {
                setupCallArgs(compiler.mv, compiler);
            } finally {
                compiler.flags.clear(PythonCompiler.F_DYN_CALL);
            }
        }

        switch (parent) {
            case PyObjectRef(String name, Location location) -> {
                Symbol symbol = compiler.symbols.get(name);
                if (symbol instanceof PyBuiltinFunction func) {
                    return func.type(compiler);
                }
            }
            case PyBuiltinFunction func -> {
                return ((PyBuiltinFunction) parent).type(compiler);
            }
            default -> {

            }
        }
        Type owner = owner(compiler);
        if (owner.getSort() == Type.ARRAY) owner = componentTypeR(owner);
        String substring = owner.getClassName();
        String name = name();
        if (compiler.getClassSymbol(name) != null) {
            name = "<init>";
        }
        String desc = argDesc(compiler, arguments);
        ImmutableList<Type> parse = Descriptor.parse(desc);
        JvmClass aClass;
        Type type = Type.getObjectType(substring.replace('.', '/'));
        if (!PythonCompiler.classCache.load(compiler, type)) {
            throw new CompilerException("Class not found: " + substring, compiler.getLocation(this));
        }

        aClass = PythonCompiler.classCache.get(type);
        if (name.equals("<init>")) {
            JvmConstructor constructor1 = aClass.constructor(compiler, parse.toArray(Type[]::new));
            if (constructor1 != null) {
                return constructor1.returnType(compiler);
            }

            throw new CompilerException("Java constructor with parameters (" + Arrays.stream(parse.toArray()).map(PyExpr.class::cast).map(pyExpr -> pyExpr.type(compiler)).map(Type::getClassName).collect(Collectors.joining(", ")) + ") not found in " + substring , compiler.getLocation(this));
        }
        Stack<JvmClass> stack = new Stack<>();
        stack.push(aClass);

        while (!stack.isEmpty()) {
            if (!aClass.type(compiler).equals(Type.getType(Object.class)) && aClass.firstSuperClass(compiler) != null)
                stack.push(aClass.firstSuperClass(compiler));
            for (JvmClass anInterface : aClass.interfaces(compiler)) {
                stack.push(anInterface);
            }

            List<JvmFunction> list = new ArrayList<>();
            for (Map.Entry<String, List<JvmFunction>> stringListEntry : aClass.methods(compiler).entrySet()) {
                list.addAll(stringListEntry.getValue());
            }
            JvmFunction determinedFunction = aClass.function(compiler, name, parse.toArray(Type[]::new));
            if (determinedFunction != null) {
                return determinedFunction.returnType(compiler);
            }
            aClass = stack.pop();
        }

        throw new CompilerException("Function '" + name + "' not found with arguments '" + parse.stream().map(Type::getClassName).collect(Collectors.joining(", ")) + "' in " + substring , compiler.getLocation(this));
    }

    @Override
    public Location location() {
        return location;
    }

    @Override
    public void expectReturnType(PythonCompiler compiler, JvmClass returnType, Location location) {

    }

    private @Nullable Type determineConstructor(PythonCompiler compiler, JvmConstructor constructor, ImmutableList<Type> parse, Type owner) {
        StringBuilder stringBuilder = new StringBuilder();
        if (constructor.parameterCount(compiler) != parse.size()) return null;
        @NotNull JvmClass[] parameterTypes = constructor.parameterClasses(compiler);
        boolean passed = true;
        for (int i = 0, parameterTypesLength = parameterTypes.length; i < parameterTypesLength; i++) {
            try {
                passed = doesConstructorPass(compiler, parameterTypes, i, parse, passed, stringBuilder);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Failed to load class: ", e);
            }
        }
        if (passed) {
            this.constructor = constructor;
            return owner;
        }
        return null;
    }

    private @Nullable Type determineFunction(PythonCompiler compiler, JvmFunction function, String name, ImmutableList<Type> parse, Type owner) {
        if (!function.name().equals(name)) return null;
        StringBuilder sb = new StringBuilder();
        JvmClass[] parameterTypes = function.parameterClasses(compiler);
        boolean passed = true;
        for (int i = 0, parameterTypesLength = parameterTypes.length; i < parameterTypesLength; i++) {
            try {
                passed = doesFunctionPass(compiler, parameterTypes, i, parse, passed, sb);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Failed to load class: ", e);
            }
        }

        if (passed) {
            this.function = function;
            return function.returnType(compiler);
        }
        return null;
    }

    private boolean doesFunctionPass(PythonCompiler compiler, @NotNull JvmClass @NotNull [] parameterTypes, int i, ImmutableList<Type> parse, boolean passed, StringBuilder sb) throws ClassNotFoundException {
        if (i >= parse.size()) return false;

        @NotNull JvmClass paramType = parameterTypes[i];
        if (!paramType.isPrimitive() && compiler.classes.hasClassName(paramType.name())) {
            Type type = parse.get(i);
            PyClass pyClass = compiler.classes.byName(type.getClassName());
            if (type.getSort() != Type.ARRAY && type.getSort() != Type.OBJECT && type.getSort() != Type.VOID) {
                switch (type.getSort()) {
                    case Type.INT -> sb.append("Ljava/lang/Integer;");
                    case Type.LONG -> sb.append("Ljava/lang/Long;");
                    case Type.FLOAT -> sb.append("Ljava/lang/Float;");
                    case Type.DOUBLE -> sb.append("Ljava/lang/Double;");
                    case Type.BOOLEAN -> sb.append("Ljava/lang/Boolean;");
                    case Type.CHAR -> sb.append("Ljava/lang/Character;");
                    case Type.BYTE -> sb.append("Ljava/lang/Byte;");
                    case Type.SHORT -> sb.append("Ljava/lang/Short;");
                    default -> {
                        return false;
                    }
                }
                return passed;
            }
            if (!pyClass.doesInherit(compiler, paramType)) {
                passed = false;
            }
        } else if (paramType.isPrimitive()) {
            if (!paramType.type(compiler).equals(parse.get(i))) {
                return false;
            }
        } else {
            var checkAgainst = parse.get(i);
            if (!PythonCompiler.classCache.require(compiler, checkAgainst).doesInherit(compiler, paramType)) {
                return false;
            }
            sb.append(paramType.type(compiler).getDescriptor());
        }
        return passed;
    }

    private boolean doesConstructorPass(PythonCompiler compiler, @NotNull JvmClass @NotNull [] parameterTypes, int i, ImmutableList<Type> parse, boolean passed, StringBuilder sb) throws ClassNotFoundException {
        @NotNull JvmClass paramType = parameterTypes[i];
        String className = parse.get(i).getClassName();
        if (!paramType.isPrimitive() && compiler.classes.hasClassName(className)) {
            PyClass pyClass = compiler.classes.byName(paramType.className());
            if (!pyClass.doesInherit(compiler, paramType)) {
                passed = false;
            }
        } else if (paramType.isPrimitive()) {
            var checkAgainst = Class.forName(parse.get(i).getClassName(), false, getClass().getClassLoader());
            if (!paramType.type(compiler).equals(parse.get(i))) {
                return false;
            }
            sb.append(paramType.type(compiler).getDescriptor());
        } else {
            JvmClass checkAgainst = PythonCompiler.classCache.require(compiler, parse.get(i));
            if (!checkAgainst.doesInherit(compiler, paramType)) {
                passed = false;
                return passed;
            }
            sb.append(paramType.type(compiler).getDescriptor());
        }
        return passed;
    }

    private Type componentTypeR(Type owner) {
        owner = owner.getElementType();
        if (owner.getSort() == Type.ARRAY) owner = componentTypeR(owner);
        return owner;
    }

    private void setupArgs(MethodVisitor mv, PythonCompiler compiler, List<?> visit1) {
        List<PyExpr> exprs = new ArrayList<>();
        for (int j = 0, objectsSize = visit1.size(); j < objectsSize; j++) {
            Object o = visit1.get(j);
            final Location line = arguments.get(j).location();
            switch (o) {
                case String s -> exprs.add(new PyConstant(s, line));
                case Boolean b -> exprs.add(new PyConstant(b, line));
                case Float f -> exprs.add(new PyConstant(f, line));
                case Double d -> exprs.add(new PyConstant(d, line));
                case Character c -> exprs.add(new PyConstant(c, line));
                case Byte b -> exprs.add(new PyConstant(b, line));
                case Short s -> exprs.add(new PyConstant(s, line));
                case Integer i -> exprs.add(new PyConstant(i, line));
                case Long l -> exprs.add(new PyConstant(l, line));
                case PyExpr ref -> exprs.add(ref);
                case null, default -> {
                    if (o != null) {
                        throw new RuntimeException("argument not supported with owner " + o.getClass().getName() + " at: " + location);
                    }
                    throw new RuntimeException("argument not supported with owner <NULL> at: " + location);
                }
            }
        }

        Object[] preloaded = new Object[exprs.size()];
        for (PyExpr expr : exprs) {
            // What? TODO
//                expr.load(mv, compiler, null, false);
        }

//            createArray(mv, compiler, exprs, preloaded);
    }

    private void createArray(MethodVisitor mv, PythonCompiler compiler, List<PyExpr> exprs, Object[] preloaded) {
        compiler.writer.pushStackByte(exprs.size());
        compiler.writer.newArray(Type.getType(Object.class));

        // Create an array of objects
        for (int i = 0; i < exprs.size(); i++) {
            PyExpr expr = exprs.get(i);

            compiler.writer.lineNumber(arguments.get(i).location().lineStart());
            compiler.writer.dup();
            compiler.writer.pushStackByte(i);
            preloaded[i] = expr.preload(mv, compiler, false);
            expr.load(mv, compiler, preloaded[i], false);

            Type type1 = expr.type(compiler);
            if (type1 == null) {
                throw new RuntimeException("Type not found for at " + compiler.getLocation(expr));
            }
            if (type1 == Type.INT_TYPE) {
                compiler.writer.invokeStatic("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
            } else if (type1 == Type.LONG_TYPE) {
                compiler.writer.invokeStatic("java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
            } else if (type1 == Type.FLOAT_TYPE) {
                compiler.writer.invokeStatic("java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
            } else if (type1 == Type.DOUBLE_TYPE) {
                compiler.writer.invokeStatic("java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
            } else if (type1 == Type.CHAR_TYPE) {
                compiler.writer.invokeStatic("java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
            } else if (type1 == Type.BOOLEAN_TYPE) {
                compiler.writer.invokeStatic("java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
            } else if (type1 == Type.SHORT_TYPE) {
                compiler.writer.invokeStatic("java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
            } else if (type1 == Type.BYTE_TYPE) {
                compiler.writer.invokeStatic("java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
            }

            compiler.writer.arrayStoreObject(Type.getType(Object.class));
        }
    }

    @Override
    public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        return null;
    }

    @Override
    public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
        write(mv, compiler);
    }

    @Override
    public String name() {
        if (name != null) {
            return name;
        }

        return "__call__";
    }

    public Type owner(PythonCompiler compiler) {
        if (parent != null) {
            return parent.type(compiler);
        }
        throw new AssertionError("No owner found for function at: " + location);
    }

    @Override
    public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit) {
        throw new CompilerException("Cannot set a function ", location);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (FuncCall) obj;
        return Objects.equals(this.parent, that.parent) &&
                Objects.equals(this.name, that.name) &&
                Objects.equals(this.arguments, that.arguments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, name, arguments);
    }

    @Override
    public String toString() {
        return "FuncCallWithArgs[" +
                "parent=" + parent +
                ", name='" + name + '\'' +
                ", arguments=" + arguments +
                ']';
    }

    public JvmClass cls(PythonCompiler pythonCompiler) {
        if (!PythonCompiler.classCache.load(pythonCompiler, owner(pythonCompiler))) {
            throw new RuntimeException("Class not found: " + owner(pythonCompiler));
        }

        return PythonCompiler.classCache.get(owner(pythonCompiler));
    }
}
