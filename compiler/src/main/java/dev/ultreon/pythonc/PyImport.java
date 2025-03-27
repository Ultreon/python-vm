package dev.ultreon.pythonc;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.List;

public class PyImport implements Symbol {
    private final String alias;
    final Symbol symbol;
    private Location location;

    public PyImport(String alias, Symbol symbol, Location location) {
        this.alias = alias;
        this.symbol = symbol;
        this.location = location;
    }

    @Override
    public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        return null;
    }

    @Override
    public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
        symbol.load(mv, compiler, symbol.preload(mv, compiler, boxed), boxed);
    }

    @Override
    public String name() {
        return alias;
    }

    @Override
    public Type type(PythonCompiler compiler) {
        return symbol.type(compiler);
    }

    @Override
    public Location location() {
        return location;
    }

    @Override
    public void expectReturnType(PythonCompiler compiler, JvmClass returnType, Location location) {
        symbol.expectReturnType(compiler, returnType, location);
    }

    @Override
    public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit) {
        symbol.set(mv, compiler, visit);
    }

    public String alias(PythonCompiler compiler) {
        return alias;
    }

    public void invoke(MethodVisitor mv, PythonCompiler compiler, String signature, List<PyExpr> callArgs) {
        switch (symbol) {
            case PyBuiltinFunction builtinFunction -> {
                compiler.writer.writeArgs(callArgs, builtinFunction.parameterTypes(compiler));
                compiler.writer.dynamicBuiltinCall(builtinFunction.name, signature);
            }
            case PyBuiltinClass builtinMethod -> {
                compiler.writer.writeArgs(callArgs, builtinMethod.constructor(compiler, callArgs.stream().map(expr -> expr.type(compiler)).toArray(Type[]::new)).parameterTypes(compiler));
                compiler.writer.dynamicBuiltinCall("\uffffinit\uffff", signature);
            }
            case PyObjectRef(String name, Location location) -> {
                Symbol symbol = compiler.symbols.get(name);
                switch (symbol) {
                    case PyBuiltinFunction func -> {
                        compiler.writer.writeArgs(callArgs, func.parameterTypes(compiler));
                        compiler.writer.dynamicBuiltinCall(func.name, signature);
                    }
                    case PyBuiltinClass builtinClass -> {
                        JvmConstructor constructor = builtinClass.constructor(compiler, callArgs.stream().map(expr -> expr.type(compiler)).toArray(Type[]::new));
                        if (constructor == null) {
                            throw new CompilerException("Unsupported call to " + name + " (owner: " + symbol.getClass() + ")");
                        }
                        compiler.writer.writeArgs(callArgs, constructor.parameterTypes(compiler));
                    }
                    case PyImport importSymbol -> importSymbol.invoke(mv, compiler, signature, callArgs);
                    case PyObjectRef pyObjectRef -> throw new CompilerException("Unsupported call to PyObjectRef");
                    case null, default ->
                            throw new CompilerException("Unsupported object reference call to " + name + " (owner: " + symbol.getClass() + ")");
                }
            }
            case PyClass cls -> compiler.writer.newInstance(cls, callArgs);
            case PyFunction func -> compiler.writer.dynamicCall(func.owner(compiler), callArgs);
            case JClass jClass -> compiler.writer.newInstance(jClass, callArgs);
            case PyField field -> compiler.writer.dynamicCall(field, callArgs);
            case ImportedField importedField -> compiler.writer.dynamicCall(importedField, callArgs);
            default -> throw new CompilerException("Unsupported call to " + symbol + " (owner: " + symbol.getClass() + ")");
        }
    }

    public JvmField field(PythonCompiler compiler, String name, Type type) {
        if (symbol instanceof JvmClass cls) {
            return cls.field(compiler, name);
        } else {
            throw new CompilerException("Unsupported call to " + symbol + " (owner: " + symbol.getClass() + ")");
        }
    }

    public JvmFunction function(PythonCompiler compiler, String name, Type[] paramTypes) {
        if (symbol instanceof JvmClass cls) {
            if (name.equals("<init>")) {
                return cls.constructor(compiler, paramTypes);
            }
            return cls.function(compiler, name, paramTypes);
        } else {
            throw new CompilerException("Unsupported call to " + symbol + " (owner: " + symbol.getClass() + ")");
        }
    }
}
