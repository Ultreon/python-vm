package dev.ultreon.pythonc;

import com.google.common.base.Preconditions;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class PyFunction implements JvmFunction, Symbol {
    final PyCompileClass owner;
    private final String name;
    private final Type[] paramTypes;
    private Self self = null;
    Type returnType;
    private JvmClass returnClass;
    private final Location location;
    private final boolean isStatic;
    private final PyLocalVariables localSymbols = new PyLocalVariables(this);

    public PyFunction(PythonCompiler compiler, PyCompileClass owner, String name, Type[] paramTypes, Type returnType, Location location, boolean isStatic) {
        this.location = location;
        this.isStatic = isStatic;
        Preconditions.checkArgument(owner != null, "owner == null");
        this.owner = owner;
        this.name = name;
        this.paramTypes = paramTypes;
        this.returnType = returnType;

        if (!isStatic && !(owner instanceof PyModule)) {
            this.self = this.localSymbols.createThis(compiler, location);
        }
    }

    @Override
    public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        return null;
    }

    @Override
    public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
        Type methodType = Type.getMethodType(returnType(compiler), parameterTypes(compiler));
        // TODO
        if (isStatic || owner instanceof PyModule) {
            owner.load(mv, compiler, owner.preload(mv, compiler, false), false);
            compiler.writer.dynamicSetAttr(name);
        } else if (self != null) {
            self.load(mv, compiler, self.preload(mv, compiler, false), false);
            compiler.writer.dynamicSetAttr(name);
        } else {
            throw new AssertionError("DEBUG");
        }
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public PyCompileClass owner(PythonCompiler compiler) {
        return owner;
    }

    @Override
    public Type type(PythonCompiler compiler) {
        return returnType;
    }

    @Override
    public Location location() {
        return location;
    }

    @Override
    public void expectReturnType(PythonCompiler compiler, JvmClass returnType, Location location) {
        if (!returnClass(compiler).doesInherit(compiler, returnType)) {
            throw new CompilerException("Incompatible return type (" + compiler.getLocation(this) + ")");
        }
    }

    @Override
    public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit) {
        throw new RuntimeException("not supported");
    }

    @Override
    public void invoke(Object callArgs, Runnable paramInit) {
        throw new AssertionError("DEBUG");
    }

    @Override
    public Type returnType(PythonCompiler compiler) {
        return returnType;
    }

    @Override
    public JvmClass returnClass(PythonCompiler compiler) {
        if (returnClass != null) return returnClass;
        if (returnType.equals(Type.VOID_TYPE)) return null;
        if (!PythonCompiler.classCache.load(compiler, returnType)) {
            throw new CompilerException("Class '" + returnType.getClassName() + "' not found yet (" + compiler.getLocation(this) + ")");
        }
        return returnClass = PythonCompiler.classCache.get(returnType);
    }

    @Override
    public Type[] parameterTypes(PythonCompiler compiler) {
        return paramTypes;
    }

    @Override
    public JvmClass[] parameterClasses(PythonCompiler compiler) {
        JvmClass[] classes = new JvmClass[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            if (!PythonCompiler.classCache.load(compiler, paramTypes[i]))
                throw new CompilerException("Class '" + paramTypes[i].getClassName() + "' not found yet (" + compiler.getLocation(this) + ")");
            classes[i] = PythonCompiler.classCache.get(paramTypes[i]);
        }
        return classes;
    }

    @Override
    public boolean isStatic() {
        return isStatic;
    }

    @Override
    public JvmClass ownerClass(PythonCompiler compiler) {
        return owner(compiler);
    }

    @Override
    public boolean isDynamicCall() {
        return false;
    }

    public PyVariable createVariable(PythonCompiler compiler, String name, PyExpr expr, boolean boxed, Location location) {
        return localSymbols.createVariable(compiler, name, expr, boxed, location);
    }

    @Deprecated
    public PyVariable createVariable(PythonCompiler compiler, String name, boolean boxed, Location location) {
        return localSymbols.createVariable(compiler, name, boxed, location);
    }

    public PyVariable getVariable(String name) {
        return localSymbols.get(name);
    }

    public PyVariable createParam(String name, PyVariable pyVariable) {
        localSymbols.createParam(name, pyVariable);
        return pyVariable;
    }
}
