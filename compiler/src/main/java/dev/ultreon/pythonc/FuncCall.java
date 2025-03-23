package dev.ultreon.pythonc;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

final class FuncCall implements Symbol {
    public static final String E_CLASS_NOT_IN_CP = "Class not found in classpath (yet), maybe the class isn't initialized yet: %s (%s)";
    final PythonParser.PrimaryContext atom;
    final PythonParser.PrimaryContext primaryContext;
    final PythonParser.ArgumentsContext arguments;
    private Method method;
    private JvmConstructor constructor;
    private boolean kwargs;
    private boolean varArgs;
    private boolean dynCtor;
    private Object callArgs;
    private Type owner;
    private Type[] paramTypes;
    private JvmFunction function;
    private BitSet boxing;

    FuncCall(PythonParser.PrimaryContext atom, PythonParser.PrimaryContext primaryContext,
             PythonParser.ArgumentsContext arguments) {
        this.atom = atom;
        this.primaryContext = primaryContext;
        this.arguments = arguments;
    }

    public void write(MethodVisitor mv, PythonCompiler compiler) {
        compiler.writer.lineNumber(atom.getStart().getLine(), new Label());
        compiler.pushMemberContext(MemberContext.FUNCTION);
        compiler.callingFunction = this;
        Object visit = compiler.visit(atom);
        compiler.callingFunction = this;
        compiler.popMemberContext();
        if (visit == null) {
            throw new RuntimeException("atom not supported for:\n" + atom.getText());
        }

        if (visit instanceof TypedName) {
            throw new RuntimeException("atom not supported for:\n" + atom.getText());
        } else if (visit instanceof PyObjectRef pyObjectRef) {
            // Print the array
            Symbol symbol = (Symbol) compiler.visit(atom);
            if (symbol == null) {
                throw new CompilerException("Symbol '" + name() + "' not found (" + compiler.getLocation(this) + ")");
            }
            String named = symbol.name();
            Type owner = symbol.type(compiler);
            if (symbol instanceof PyObjectRef(String name, int lineNo)) {
                symbol = compiler.symbols.get(name);
                switch (symbol) {
                    case PyBuiltinFunction builtinFunction -> {
                        this.varArgs = builtinFunction.varArgs;
                        this.kwargs = builtinFunction.kwargs;
                        this.dynCtor = builtinFunction.dynCtor;

                        if (dynCtor) {
                            compiler.flags.set(PythonCompiler.F_DYN_CALL);
                            try {
                                setupCallArgs(mv, compiler);
                            } finally {
                                compiler.flags.clear(PythonCompiler.F_DYN_CALL);
                            }

                            // Create Object Array
                            createArray(mv, compiler, ((List<?>) callArgs).stream().map(v -> (PyExpr) switch (v) {
                                case String s -> new PyConstant(s, lineNo);
                                case Integer i -> new PyConstant(i, lineNo);
                                case Long l -> new PyConstant(l, lineNo);
                                case Float f -> new PyConstant(f, lineNo);
                                case Double d -> new PyConstant(d, lineNo);
                                case Boolean b -> new PyConstant(b, lineNo);
                                case Byte b -> new PyConstant(b, lineNo);
                                case Short s -> new PyConstant(s, lineNo);
                                case Character c -> new PyConstant(c, lineNo);
                                case PyExpr pyExpr -> pyExpr;
                                case null, default ->
                                        throw new RuntimeException("argument not supported for:\n" + arguments.getText());
                            }).toList(), new Object[((List<?>) callArgs).size()]);

                            // Kwargs
                            // Create Map<String, Object>
                            // Call Mao.of()
                            compiler.writer.invokeStatic("java/util/Map", "of", "()Ljava/util/Map;", true);
                            compiler.writer.invokeStatic(builtinFunction.mapOwner.getInternalName(), builtinFunction.name, "([Ljava/lang/Object;Ljava/util/Map;)V", false);
                            return;
                        }

                        if (atom.NAME() == null && atom.atom() != null && atom.atom().NAME() != null) {
                            Type mapOwner = builtinFunction.mapOwner;
                            setupCallArgs(mv, compiler);

                            compiler.writer.invokeStatic(mapOwner.getInternalName(), name(), "(" + argDesc(compiler, callArgs) + ")" + type(compiler), false);
                            return;
                        } else {
                            throw new RuntimeException("Unexpected builtin function call!");
                        }
                    }
                    case JClass jClass -> {
                        if (atom.NAME() == null && atom.atom() != null && atom.atom().NAME() != null) {
                            // Constructor Call
                            Context context = compiler.getContext(Context.class);
                            final Type finalOwner = owner;
                            setupCallArgs(mv, compiler);
                            compiler.writer.newInstance(owner.getInternalName(), "<init>", "(" + argDesc(compiler, callArgs) + ")V", false, () -> {
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

                        if (callArgs == null) {
                            throw new RuntimeException("Call args not found for:\n" + arguments.getText());
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

                        String s = argDesc(compiler, callArgs);
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
                                owner = func.owner(compiler);
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
                                    owner = func.owner(compiler);
                                } else if (symbol1 instanceof JvmClass jvmClass) {
                                    owner = jvmClass.type(compiler);
                                } else {
                                    throw new RuntimeException("Unknown symbol type: " + symbol1.getClass().getSimpleName());
                                }
                            } else {
                                throw new RuntimeException("Unknown symbol type: " + symbol1.getClass().getSimpleName());
                            }
                        } else {
                            throw new RuntimeException("Unknown JVM descriptor for class typedName: " + owner);
                        }
                        compiler.writer.invokeVirtual(owner.getInternalName(), name(), "(" + s + ")" + type(compiler), false);
                    }
                    case PyClass pyClass -> {
                        Type owner1 = owner(compiler);
                        installArgs(mv, compiler, owner1);
                        compiler.writer.newInstance(owner1.getInternalName(), "<init>", "(" + argDesc(compiler, callArgs) + ")V", false, () -> setupCallArgs(mv, compiler));
                        return;
                    }
                    case PyImport pyImport -> {
                        Type type = pyImport.type(compiler);
                        setupCallArgs(mv, compiler);
                        String ourName = this.name();
                        Symbol ourSymbol = compiler.symbols.get(ourName);
                        if (ourSymbol instanceof JvmClass) {
                            ourName = "<init>";
                        } else if (ourSymbol instanceof PyImport pyImport1 && pyImport1.symbol instanceof JvmClass) {
                            ourName = "<init>";
                        }
                        if (!installArgs(mv, compiler, type, true)) {
                            throw new CompilerException("Function '" + ourName + "' not found in '" + pyImport.type(compiler).getClassName() + "' with arguments (" + argNames(compiler, callArgs) + ") (" + compiler.getLocation(this) + ")");
                        }
                        Type[] args = args(compiler, callArgs);
                        JvmFunction jvmFunction = pyImport.function(compiler, ourName, args);
                        if (jvmFunction == null) {
                            throw new CompilerException("Function '" + ourName + "' not found in '" + pyImport.type(compiler).getClassName() + "' (" + compiler.getLocation(this) + ")");
                        }

                        if (jvmFunction instanceof JvmConstructor constructor) {
                            constructor.write(mv, compiler, () -> writeArgs(mv, compiler, (List<PyExpr>) callArgs, boxing));
                        } else if (!ourName.equals("<init>")) {
                            writeArgs(mv, compiler, (List<PyExpr>) callArgs, boxing);
                            jvmFunction.write(mv, compiler);
                        } else {
                            throw new CompilerException("Constructor with arguments (" + argNames(compiler, callArgs) + ") not found in '" + pyImport.type(compiler).getClassName() + "' (" + compiler.getLocation(this) + ")");
                        }
//                        return;
                    }
                    case JvmConstructor jvmConstructor -> {
                        setupCallArgs(mv, compiler);

                        jvmConstructor.write(mv, compiler, () -> writeArgs(mv, compiler, (List<PyExpr>) callArgs, boxing));
                        return;
                    }
                    case JvmFunction jvmFunction -> {
                        setupCallArgs(mv, compiler);
                        installArgs(mv, compiler, jvmFunction.owner(compiler).type(compiler), true);

                        if (jvmFunction instanceof JvmConstructor constructor) {
                            constructor.write(mv, compiler, () -> writeArgs(mv, compiler, (List<PyExpr>) callArgs, boxing));
                        } else {
                            writeArgs(mv, compiler, (List<PyExpr>) callArgs, boxing);
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

                        jvmClass.constructor(compiler, paramTypes).write(mv, compiler, () -> writeArgs(mv, compiler, (List<PyExpr>) callArgs, boxing));
                        return;
                    }
                    case null, default ->
                            throw new UnsupportedOperationException("Not implemented: " + symbol.getClass().getSimpleName());
                }

                return;
            }
            if (symbol instanceof PyImport pyImport) {
                pyImport.invoke(mv, compiler, "(" + argDesc(compiler, callArgs) + ")" + pyImport.type(compiler).getDescriptor(), callArgs, () -> setupCallArgs(mv, compiler));
                return;
            } else if (symbol instanceof PyBuiltinFunction) {
//                    symbol.load(mv, compiler, null, false);
                PythonCompiler.throwError(mv, "Unimplemented function call: atom=" + atom.getText() + ", primaryContext=" + primaryContext.getText() + ", arguments=" + arguments.getText());
                return;
            } else if (symbol instanceof JClass) {
                setupCallArgs(mv, compiler);
                compiler.writer.invokeStatic(owner.getInternalName(), name(), "(" + (arguments == null ? "" : argDesc(compiler, callArgs)) + ")V", false);
            } else if (symbol instanceof PyVariable pyVariable) {
                symbol.load(mv, compiler, symbol.preload(mv, compiler, false), false);
                setupCallArgs(mv, compiler);

                installArgs(mv, compiler, pyVariable.type(compiler));

                String s = argDesc(compiler, callArgs);
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
                        owner = func.owner(compiler);
                    } else {
                        throw new RuntimeException("Unknown symbol type: " + symbol1.getClass().getSimpleName());
                    }
                } else {
                    throw new RuntimeException("Unknown JVM descriptor for class typedName: " + owner);
                }
                compiler.writer.invokeVirtual(owner.getInternalName(), name(), "(" + s + ")" + type(compiler), false);
            } else {
                throw new UnsupportedOperationException("Not implemented: " + symbol.getClass().getSimpleName());
            }
        } else if (visit instanceof JvmConstructor constructor) {
            setupCallArgs(mv, compiler);
            installArgs(mv, compiler, constructor.owner(compiler).type(compiler), true);
            constructor.write(mv, compiler, () -> writeArgs(mv, compiler, (List<PyExpr>) callArgs, boxing));
        } else if (visit instanceof JvmFunction func) {
            setupCallArgs(mv, compiler);
            installArgs(mv, compiler, func.owner(compiler).type(compiler));
            func.write(mv, compiler);
        } else if (visit instanceof FuncCall func) {
            FuncCall funcCall = (FuncCall) visit;
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
                            if (method.getParameterTypes().length != (callArgs == null ? 0 : ((List<Symbol>) callArgs).size()))
                                continue;
                            Class<?>[] parameterTypes = method.getParameterTypes();
                            for (int i = 0; i < parameterTypes.length; i++) {
                                if (!parameterTypes[i].isAssignableFrom(Class.forName(((List<Symbol>) callArgs).get(i).type(compiler).getClassName(), false, getClass().getClassLoader()))) {
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

            setupArgs(mv, compiler, arguments == null ? List.of() : (List<?>) compiler.visit(arguments));

            if (atom.NAME() == null && atom.atom() != null && atom.atom().NAME() != null) {
                // Constructor Call
                compiler.writer.newInstance(owner.getInternalName(), "<init>", "(" + argDesc(compiler, callArgs) + ")V", false, () -> setupCallArgs(mv, compiler));
                return;
            }
            compiler.writer.invokeVirtual(owner(compiler).getInternalName(), name(), "(" + argDesc(compiler, callArgs) + ")" + type(compiler), false);
            // Print the array
            //                throw new RuntimeException("atom not supported for:\n" + atom.getText());
            //                compiler.invokeDynamic(type(compiler), typedName(), "(" + compiler.visit(arguments) + ")V", false);
            // Throw exception with error message

//                throwError(mv, "Unimplemented function call: atom=" + atom.getText() + ", primaryContext=" + primaryContext.getText() + ", arguments=" + arguments.getText());
        } else if (visit instanceof MemberFuncCall call) {
            setupCallArgs(mv, compiler);
            Object preload = call.preload(mv, compiler, false);
            installArgs(mv, compiler, call.owner(compiler));
            call.load(mv, compiler, preload, false);
        } else {
            throw new RuntimeException("No supported matching atom found for: " + visit.getClass().getName());
        }

        compiler.writer.lineNumber(atom.getStop().getLine(), new Label());
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
            List<PyExpr> args = (List<PyExpr>) callArgs;
            while (!stack.isEmpty()) {
                JvmConstructor[] jvmFunctions = aClass.constructors(compiler);
                if (jvmFunctions != null) {
                    find_constructor:
                    for (JvmConstructor constructor : jvmFunctions) {
                        if (constructor.parameterClasses(compiler).length != (((List<PyExpr>) callArgs).size()))
                            continue;
                        JvmClass[] parameterTypes = constructor.parameterClasses(compiler);
                        BitSet boxing = new BitSet(parameterTypes.length);
                        for (int i = 0; i < parameterTypes.length; i++) {
                            Type type1 = ((List<PyExpr>) callArgs).get(i).type(compiler);
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
                if (aClass.superClass(compiler) != null) stack.push(aClass.superClass(compiler));
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
        List<PyExpr> args = (List<PyExpr>) callArgs;
        while (!stack.isEmpty()) {
            Map<String, List<JvmFunction>> methods = aClass.methods(compiler);
            if (methods != null) {
                List<JvmFunction> jvmFunctions = methods.get(name);
                if (jvmFunctions != null) {
                    find_method:
                    for (JvmFunction method : jvmFunctions) {
                        if (method.parameterClasses(compiler).length != (((List<Symbol>) callArgs).size())) {
                            continue;
                        }
                        JvmClass[] parameterTypes = method.parameterClasses(compiler);
                        BitSet boxing = new BitSet(parameterTypes.length);
                        for (int i = 0; i < parameterTypes.length; i++) {
                            Type type1 = ((List<PyExpr>) callArgs).get(i).type(compiler);
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
            if (aClass.superClass(compiler) != null) stack.push(aClass.superClass(compiler));
            for (JvmClass aClass1 : aClass.interfaces(compiler)) {
                stack.push(aClass1);
            }
            aClass = stack.pop();
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
        callArgs = List.of();
        if (arguments != null) {
            callArgs = compiler.visit(arguments);
        }
        if (!(callArgs instanceof List)) {
            throw new RuntimeException("arguments not supported for:\n" + arguments.getText());
        }
        setupArgs(mv, compiler, (List<?>) callArgs);
    }

    private Type[] args(PythonCompiler compiler, Object callArgs) {
        if (arguments == null) return new Type[0];
        if (callArgs == null) throw new RuntimeException("Call args not found for:\n" + arguments.getText());
        if (paramTypes != null)
            return paramTypes;
        return switch (callArgs) {
            case List<?> list -> {
                int listSize = list.size();
                if (varArgs) listSize--;
                if (kwargs) listSize--;
                List<Type> args = new ArrayList<>(listSize);
                for (int j = 0; j < listSize; j++) {
                    Object o = list.get(j);
                    Type string = switch (o) {
                        case String s -> Type.getType(String.class);
                        case Integer i -> Type.INT_TYPE;
                        case Long i -> Type.LONG_TYPE;
                        case Float i -> Type.FLOAT_TYPE;
                        case Double i -> Type.DOUBLE_TYPE;
                        case Boolean i -> Type.BOOLEAN_TYPE;
                        case PyVariable pyVar -> type(compiler);
                        case PyObjectRef objectRef -> type(compiler);
                        case FuncCall funcCallWithArgs -> funcCallWithArgs.type(compiler);
                        case PyConstant pyConstant -> pyConstant.type.type;
                        case PyExpr expr -> expr.type(compiler);
                        case null, default -> {
                            if (o != null) {
                                throw new RuntimeException("argument not supported with type " + o.getClass().getSimpleName() + " for:\n" + arguments.getText());
                            } else {
                                throw new RuntimeException("argument not supported with type <NULL> for:\n" + arguments.getText());
                            }
                        }
                    };
                }
                if (varArgs) args.add(Type.getType(Object[].class));
                if (kwargs) args.add(Type.getType(Map.class));
                yield args.toArray(new Type[0]);
            }
            default -> throw new RuntimeException("argument not supported for:\n" + arguments.getText());
        };
    }

    private String argNames(PythonCompiler compiler, Object callArgs) {
        if (arguments == null) return "";
        if (callArgs == null) throw new RuntimeException("Call args not found for:\n" + arguments.getText());
        if (paramTypes != null)
            return Arrays.stream(paramTypes).map(Type::getDescriptor).collect(Collectors.joining(""));
        return switch (callArgs) {
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
                                throw new RuntimeException("argument not supported with type " + o.getClass().getSimpleName() + " for:\n" + arguments.getText());
                            } else {
                                throw new RuntimeException("argument not supported with type <NULL> for:\n" + arguments.getText());
                            }
                        }
                    };
                    joiner.add(string);
                }
                if (varArgs) joiner.add("[Ljava/lang/Object;");
                if (kwargs) joiner.add("Ljava/util/Map;");
                yield joiner.toString();
            }
            default -> throw new RuntimeException("argument not supported for:\n" + arguments.getText());
        };
    }

    private String argDesc(PythonCompiler compiler, Object callArgs) {
        if (arguments == null) return "";
        if (callArgs == null) throw new RuntimeException("Call args not found for:\n" + arguments.getText());
        if (paramTypes != null)
            return Arrays.stream(paramTypes).map(Type::getDescriptor).collect(Collectors.joining(""));
        return switch (callArgs) {
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
                                throw new RuntimeException("argument not supported with type " + o.getClass().getSimpleName() + " for:\n" + arguments.getText());
                            } else {
                                throw new RuntimeException("argument not supported with type <NULL> for:\n" + arguments.getText());
                            }
                        }
                    };
                    joiner.add(string);
                }
                if (varArgs) joiner.add("[Ljava/lang/Object;");
                if (kwargs) joiner.add("Ljava/util/Map;");
                yield joiner.toString();
            }
            default -> throw new RuntimeException("argument not supported for:\n" + arguments.getText());
        };
    }

    @Override
    public Type type(PythonCompiler compiler) {
        if (callArgs == null) {
            compiler.flags.set(PythonCompiler.F_DYN_CALL);
            try {
                setupCallArgs(compiler.mv, compiler);
            } finally {
                compiler.flags.clear(PythonCompiler.F_DYN_CALL);
            }
        }

        compiler.pushMemberContext(MemberContext.FUNCTION);
        compiler.callingFunction = this;
        Object visit = compiler.visit(atom);
        compiler.callingFunction = this;
        compiler.popMemberContext();
        switch (visit) {
            case PyObjectRef(String name, int lineNo) -> {
                Symbol symbol = compiler.symbols.get(name);
                if (symbol instanceof PyBuiltinFunction func) {
                    return func.type(compiler);
                }
            }
            case PyBuiltinFunction func -> {
                return ((PyBuiltinFunction) visit).type(compiler);
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
        String desc = argDesc(compiler, callArgs);
        ImmutableList<Type> parse = Descriptor.parse(desc);
        JvmClass aClass;
        try {
            Type type = Type.getType("L" + substring.replace('.', '/') + ";");
            if (!PythonCompiler.classCache.load(compiler, type)) {
                throw new ClassNotFoundException(substring);
            }

            aClass = PythonCompiler.classCache.get(type);
        } catch (ClassNotFoundException e) {
            JvmClass symbol = PythonCompiler.classCache.require(compiler, owner);
            if (symbol != null) {
                List<JvmFunction> functions = symbol.methods(compiler).get(name());
                if (functions == null) throw compiler.functionNotFound(owner, name, this);
                if (name.equals("<init>")) {
                    return owner;
                }
                for (JvmFunction function : functions) {
                    Type type = determineFunction(compiler, function, name, parse, owner);
                    if (type != null) {
                        return type;
                    }
                }
                throw compiler.functionNotFound(owner, name, this);
            }
            throw compiler.typeNotFound(substring, this);
        }
        if (name.equals("<init>")) {
            for (JvmConstructor constructor : aClass.constructors(compiler)) {
                @Nullable Type owner1 = determineConstructor(compiler, constructor, parse, null);
                if (owner1 != null) return owner1;
            }

            for (JvmConstructor constructor : aClass.constructors(compiler)) {
                @Nullable Type owner1 = determineConstructor(compiler, constructor, parse, owner);
                if (owner1 != null) return owner1;
            }

            throw new CompilerException("Java constructor not found in " + substring + " (" + compiler.getLocation(this) + ")");
        }
        Stack<JvmClass> stack = new Stack<>();
        stack.push(aClass);

        while (!stack.isEmpty()) {
            if (!aClass.type(compiler).equals(Type.getType(Object.class)) && aClass.superClass(compiler) != null)
                stack.push(aClass.superClass(compiler));
            for (JvmClass anInterface : aClass.interfaces(compiler)) {
                stack.push(anInterface);
            }

            List<JvmFunction> list = new ArrayList<>();
            for (Map.Entry<String, List<JvmFunction>> stringListEntry : aClass.methods(compiler).entrySet()) {
                list.addAll(stringListEntry.getValue());
            }
            for (JvmFunction method : list) {
                @Nullable Type method1 = determineFunction(compiler, method, name, parse, null);
                if (method1 != null) {
                    this.function = method;
                    return method1;
                }
            }
            List<JvmFunction> result = new ArrayList<>();
            for (Map.Entry<String, List<JvmFunction>> e : aClass.methods(compiler).entrySet()) {
                for (JvmFunction jvmFunction : e.getValue()) {
                    result.add(jvmFunction);
                }
            }
            for (JvmFunction function : result) {
                @Nullable Type method1 = determineFunction(compiler, function, name, parse, owner);
                if (method1 != null) {
                    this.function = function;
                    return method1;
                }
            }
            aClass = stack.pop();
        }
        throw new CompilerException("Function '" + name + "' not found with arguments '" + parse.stream().map(Type::getClassName).collect(Collectors.joining(", ")) + "' in " + substring + " (" + compiler.getLocation(this) + ")");
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
            if (!paramType.doesInherit(compiler, checkAgainst)) {
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
            @Nullable PythonParser.ExpressionContext expression = arguments.args().expression(j);
            final int line = expression != null ? expression.getStart().getLine() : arguments.getStart().getLine();
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
                        throw new RuntimeException("argument not supported with type " + o.getClass().getName() + " for:\n" + arguments.getText());
                    }
                    throw new RuntimeException("argument not supported with type <NULL> for:\n" + arguments.getText());
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

            compiler.writer.lineNumber(arguments.args().expression(i).getStart().getLine(), new Label());
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
        if (atom.NAME() != null) {
            return atom.NAME().getText();
        }

        if (atom.atom() != null) {
            return atom.atom().NAME().getText();
        }

        throw new RuntimeException("No NAME found for:\n" + atom.getText());
    }

    public Type owner(PythonCompiler compiler) {
        compiler.pushMemberContext(MemberContext.FUNCTION);
        compiler.callingFunction = this;
        Object visit = compiler.visit(atom);
        compiler.callingFunction = this;
        compiler.popMemberContext();
        switch (visit) {
            case PyObjectRef(String name, int lineNo) -> {
                Symbol symbol = compiler.symbols.get(name);
                if (symbol instanceof PyBuiltinFunction func) {
                    return func.mapOwner;
                } else if (symbol instanceof JvmClass cls) {
                    return cls.type(compiler);
                } else if (symbol instanceof JvmFunction imp) {
                    JvmClass funcOwner = imp.owner(compiler);
                    return funcOwner.type(compiler);
                } else if (symbol instanceof PyImport imp) {
                    if (imp.symbol == null) {
                        throw new CompilerException("Unknown symbol: " + name + " at " + compiler.getLocation(atom));
                    }

                    if (imp.symbol instanceof PyBuiltinFunction func) {
                        return func.mapOwner;
                    } else if (imp.symbol instanceof JvmClass cls) {
                        return cls.type(compiler);
                    } else if (imp.symbol instanceof JvmFunction func) {
                        return func.owner(compiler).type(compiler);
                    }

                    return imp.symbol.type(compiler);
                }

                if (symbol == null) {
                    throw new CompilerException("Unknown symbol: " + name + " at " + compiler.getLocation(atom));
                }

                return symbol.type(compiler);
            }
            case PyBuiltinFunction func -> {
                return ((PyBuiltinFunction) visit).mapOwner;
            }
            case JvmFunction func -> {
                return func.owner(compiler).type(compiler);
            }
            case FuncCall func -> {
                return func.type(compiler);
            }
            case MemberFuncCall func -> {
                return func.owner(compiler);
            }
            case null, default -> {
                if (visit != null) {
                    throw new RuntimeException("Unknown atom: " + visit.getClass().getSimpleName());
                } else {
                    throw new RuntimeException("Unknown atom: " + atom.getText());
                }
            }
        }
    }

    @Override
    public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit) {
        throw new CompilerException("Cannot set a function " + compiler.getLocation(atom));
    }

    @Override
    public int lineNo() {
        return atom.getStart().getLine();
    }

    public PythonParser.PrimaryContext atom() {
        return atom;
    }

    public PythonParser.PrimaryContext primaryContext() {
        return primaryContext;
    }

    public PythonParser.ArgumentsContext arguments() {
        return arguments;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (FuncCall) obj;
        return Objects.equals(this.atom, that.atom) &&
                Objects.equals(this.primaryContext, that.primaryContext) &&
                Objects.equals(this.arguments, that.arguments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(atom, primaryContext, arguments);
    }

    @Override
    public String toString() {
        return "FuncCallWithArgs[" +
                "atom=" + atom + ", " +
                "primaryContext=" + primaryContext + ", " +
                "arguments=" + arguments + ']';
    }

    public JvmClass cls(PythonCompiler pythonCompiler) {
        if (!PythonCompiler.classCache.load(pythonCompiler, owner(pythonCompiler))) {
            throw new RuntimeException("Class not found: " + owner(pythonCompiler));
        }

        return PythonCompiler.classCache.get(owner(pythonCompiler));
    }
}
