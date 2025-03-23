package dev.ultreon.pythonc;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class PyImport implements Symbol {
    private final String alias;
    final Symbol symbol;

    public PyImport(String alias, Symbol symbol) {
        this.alias = alias;
        this.symbol = symbol;
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
    public int lineNo() {
        return symbol.lineNo();
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
    public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit) {
        symbol.set(mv, compiler, visit);
    }

    public String alias(PythonCompiler compiler) {
        return alias;
    }

    public void invoke(MethodVisitor mv, PythonCompiler compiler, String signature, Object callArgs, Runnable paramInit) {
        switch (symbol) {
            case PyBuiltinFunction builtinFunction -> {
                paramInit.run();
                compiler.writer.invokeStatic(builtinFunction.mapOwner.getInternalName(), builtinFunction.name, signature, false);
            }
            case PyBuiltinClass builtinMethod -> compiler.writer.newInstance(builtinMethod.extName.getInternalName(), "<init>", signature, false, paramInit);
            case PyObjectRef(String name, int lineNo) -> {
                Symbol symbol = compiler.symbols.get(name);
                switch (symbol) {
                    case PyBuiltinFunction func -> {
                        paramInit.run();
                        compiler.writer.invokeStatic(func.mapOwner.getInternalName(), func.name, signature, false);
                    }
                    case PyBuiltinClass builtinClass ->
                            compiler.writer.newInstance(builtinClass.extName.getInternalName(), "<init>", signature, false, paramInit);
                    case PyImport importSymbol -> importSymbol.invoke(mv, compiler, signature, callArgs, paramInit);
                    case PyObjectRef pyObjectRef -> throw new CompilerException("Unsupported call to PyObjectRef");
                    case null, default ->
                            throw new CompilerException("Unsupported object reference call to " + name + " (type: " + symbol.getClass() + ")");
                }
            }
            case PyClass cls -> {
                paramInit.run();
                compiler.writer.newInstance(cls.owner.getInternalName(), "<init>", signature, false, paramInit);
            }
            case PyFunction func -> {
                paramInit.run();
                compiler.writer.invokeStatic(func.owner(compiler).type(compiler).getInternalName(), func.name(), signature, false);
            }
            case JClass jClass -> {
                paramInit.run();
                compiler.writer.newInstance(jClass.type(compiler).getInternalName(), "<init>", signature, false, paramInit);
            }
            case PyField field -> {
                paramInit.run();
                field.load(mv, compiler, field.preload(mv, compiler, false), false);
                compiler.writer.invokeStatic(field.typeClass(compiler).type(compiler).getInternalName(), "__call__", signature, false);
            }
            case ImportedField importedField -> {
                paramInit.run();
                importedField.load(mv, compiler, importedField.preload(mv, compiler, false), false);
                compiler.writer.invokeStatic(importedField.typeClass(compiler).type(compiler).getInternalName(), "__call__", signature, false);
            }
            default -> {
                throw new CompilerException("Unsupported call to " + symbol + " (type: " + symbol.getClass() + ")");
            }
        }
    }

    public JvmField field(PythonCompiler compiler, String name, Type type) {
        if (symbol instanceof JvmClass cls) {
            return cls.field(compiler, name);
        } else {
            throw new CompilerException("Unsupported call to " + symbol + " (type: " + symbol.getClass() + ")");
        }
    }

    public JvmFunction function(PythonCompiler compiler, String name, Type[] paramTypes) {
        if (symbol instanceof JvmClass cls) {
            if (name.equals("<init>")) {
                return cls.constructor(compiler, paramTypes);
            }
            return cls.function(compiler, name, paramTypes);
        } else {
            throw new CompilerException("Unsupported call to " + symbol + " (type: " + symbol.getClass() + ")");
        }
    }
}
