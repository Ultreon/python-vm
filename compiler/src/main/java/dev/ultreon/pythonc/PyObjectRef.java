package dev.ultreon.pythonc;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

record PyObjectRef(String name, Location location) implements Symbol {

    @Override
    public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        Symbol symbol1 = compiler.symbols.get(name);
        if (symbol1 == null) {
            throw new CompilerException("Symbol '" + name + "' not found (" + compiler.getLocation(this) + ")");
        }
        switch (symbol1) {
            case PyVariable variable -> {
                // Set variable to "<typedName>.class"
                Symbol symbol = compiler.symbols.get(name);
                if (symbol == null) {
                    throw new CompilerException("Symbol '" + name + "' not found (" + compiler.getLocation(this) + ")");
                }

                return symbol.preload(mv, compiler, false);
            }
            case PyObjectRef objectRef ->
                // Set variable to "<typedName>.class"
                //TODO
                    PythonCompiler.throwError(mv, "TODO");
            case JClass jclass -> {
                // Set variable to "<typedName>.class"
                if (mv == null) {
                    return jclass.asmType();
                }
                jclass.load(mv, compiler, jclass.preload(mv, compiler, false), false);
                return jclass;
            }
            case ImportedField importedField -> {

            }
            case PyImport importSymbol -> {
                switch (importSymbol.symbol) {
                    case PyVariable variable -> {
                        // Set variable to "<typedName>.class"
                        Symbol symbol = compiler.symbols.get(name);
                        if (symbol == null) {
                            throw new CompilerException("Symbol '" + name + "' not found (" + compiler.getLocation(this) + ")");
                        }

                        return symbol.preload(mv, compiler, false);
                    }
                    case PyObjectRef objectRef ->
                        // Set variable to "<typedName>.class"
                        //TODO
                            PythonCompiler.throwError(mv, "TODO");
                    case JClass jclass -> {
                        // Set variable to "<typedName>.class"
                        if (mv == null) {
                            return jclass.asmType();
                        }
                        jclass.load(mv, compiler, jclass.preload(mv, compiler, false), false);
                        return jclass;
                    }
                    case ImportedField importedField -> {

                    }
                    default -> throw new CompilerException("Symbol '" + name + "' invalid owner: " + compiler.symbols.get(name).getClass().getSimpleName() , compiler.getLocation(this));
                }
            }
            case JvmFunction jvmFunction -> {
                // Set variable to "<typedName>.class"
                return jvmFunction.preload(mv, compiler, false);
            }
            case JvmField jvmField -> {
                // Set variable to "<typedName>.class"
                return jvmField.preload(mv, compiler, false);
            }
            default ->
                    throw new CompilerException("Symbol '" + name + "' invalid owner: " + compiler.symbols.get(name).getClass().getSimpleName() , compiler.getLocation(this));
        }

        return null;
    }

    @Override
    public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
        if (compiler.symbols.get(name) == null) {
            throw new CompilerException("Symbol '" + name + "' not found (" + compiler.getLocation(this) + ")");
        }

        switch (compiler.symbols.get(name)) {
            case PyVariable variable -> {
                // Set variable to "<typedName>.class"
                Symbol symbol = compiler.symbols.get(name);
                if (symbol == null) {
                    throw new CompilerException("Symbol '" + name + "' not found (" + compiler.getLocation(this) + ")");
                }
                symbol.load(mv, compiler, preloaded, boxed);
            }
            case PyObjectRef objectRef -> {
                // Set variable to "<typedName>.class"
                compiler.writer.loadConstant(compiler.imports.get(objectRef.name).type(compiler));

                // Set variable
                compiler.writer.storeInt(compiler.currentVariableIndex++);
            }
            case ImportedField importedField -> importedField.load(mv, compiler, importedField.preload(mv, compiler, false), false);
            case PyImport importSymbol -> {
                switch (importSymbol.symbol) {
                    case PyVariable variable -> {
                        // Set variable to "<typedName>.class"
                        Symbol symbol = compiler.symbols.get(name);
                        if (symbol == null) {
                            throw new CompilerException("Symbol '" + name + "' not found (" + compiler.getLocation(this) + ")");
                        }
                        symbol.load(mv, compiler, preloaded, boxed);
                    }
                    case PyObjectRef objectRef -> {
                        // Set variable to "<typedName>.class"
                        compiler.writer.loadConstant(compiler.imports.get(objectRef.name).type(compiler));

                        // Set variable
                        compiler.writer.storeInt(compiler.currentVariableIndex++);
                    }
                    case JClass jclass -> {
                        // Set variable to "<typedName>.class"
                        if (mv == null) {
                            return;
                        }
                        jclass.load(mv, compiler, jclass.preload(mv, compiler, false), false);
                    }
                    case ImportedField importedField -> importedField.load(mv, compiler, importedField.preload(mv, compiler, false), false);
                    default -> throw new CompilerException("Symbol '" + name + "' invalid owner: " + importSymbol.symbol.getClass().getSimpleName() , compiler.getLocation(this));
                }
            }
            case JvmConstructor jvmConstructor -> // Set variable to "<typedName>.class"
                    jvmConstructor.load(mv, compiler, preloaded, false);
            case JvmFunction jvmFunction -> {
                // Set variable to "<typedName>.class"
                if (!jvmFunction.isStatic()) {
                    JvmClass owner = jvmFunction.owner(compiler);
                    PyCompileClass compilingClass = compiler.compilingClass;
                    if (compilingClass.doesInherit(compiler, owner)) {
                        compiler.writer.loadThis(compiler, compiler.compilingClass);
                    }
                }
                jvmFunction.load(mv, compiler, preloaded, false);
            }
            case JvmField jvmField -> // Set variable to "<typedName>.class"
                    jvmField.load(mv, compiler, preloaded, false);
            default ->
                    throw new CompilerException("Symbol '" + name + "' invalid owner: " + compiler.symbols.get(name).getClass().getSimpleName() , compiler.getLocation(this));
        }
    }

    @Override
    public Type type(PythonCompiler compiler) {
        if (compiler.symbols.get(name) == null) {
            throw new CompilerException("Symbol '" + name + "' not found (" + compiler.getLocation(this) + ")");
        }
        return compiler.symbols.get(name).type(compiler);
    }

    @Override
    public void expectReturnType(PythonCompiler compiler, JvmClass returnType, Location location) {
        if (compiler.symbols.get(name) == null) {
            throw new CompilerException("Symbol '" + name + "' not found (" + compiler.getLocation(this) + ")", location);
        }
        compiler.symbols.get(name).expectReturnType(compiler, returnType, location);
    }

    @Override
    public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit) {
        throw new CompilerException("Cannot set a object reference (" + compiler.getLocation(visit) + ")");
    }
}
