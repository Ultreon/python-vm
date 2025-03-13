package dev.ultreon.pythonc;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

record PyObjectRef(String name, int lineNo) implements Symbol {

    @Override
    public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        Symbol symbol1 = compiler.symbols.get(name);
        if (symbol1 == null) {
            throw new CompilerException("Symbol '" + name + "' not found (" + compiler.getLocation(this) + ")");
        }
        switch (symbol1) {
            case PyVariable variable -> {
                // Set variable to "<name>.class"
                Symbol symbol = compiler.symbols.get(name);
                if (symbol == null) {
                    throw new CompilerException("Symbol '" + name + "' not found (" + compiler.getLocation(this) + ")");
                }

                return symbol.preload(mv, compiler, false);
            }
            case PyObjectRef objectRef ->
                // Set variable to "<name>.class"
                //TODO
                    PythonCompiler.throwError(mv, "TODO");
            case JClass jclass -> {
                // Set variable to "<name>.class"
                if (mv == null) {
                    return jclass.asmType();
                }
                jclass.load(mv, compiler, jclass.preload(mv, compiler, false), false);
                return jclass;
            }
            case ImportedField importedField -> {

            }
            default ->
                    throw new CompilerException("Symbol '" + name + "' invalid type: " + compiler.symbols.get(name).getClass().getSimpleName() + " (" + compiler.getLocation(this) + ")");
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
                // Set variable to "<name>.class"
                Symbol symbol = compiler.symbols.get(name);
                if (symbol == null) {
                    throw new CompilerException("Symbol '" + name + "' not found (" + compiler.getLocation(this) + ")");
                }
                symbol.load(mv, compiler, preloaded, boxed);
            }
            case PyObjectRef objectRef -> {
                // Set variable to "<name>.class"
                compiler.writer.loadConstant(name);
                compiler.writer.invokeStatic("java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);

                // Set variable
                compiler.writer.storeInt(compiler.currentVariableIndex++);
            }
            case ImportedField importedField -> importedField.load(mv, compiler, importedField.preload(mv, compiler, false), false);
            default ->
                    throw new CompilerException("Symbol '" + name + "' not found (" + compiler.getLocation(this) + ")");
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
    public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit) {
        throw new CompilerException("Cannot set a object reference (" + compiler.getLocation(visit) + ")");
    }
}
