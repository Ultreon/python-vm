package dev.ultreon.pythonc;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Collectors;

public class JFunction implements JvmFunction {
    public String name;
    public Method method;
    public Class<?> owner;
    public Class<?>[] paramTypes;
    public Class<?> returnType;
    private JvmClass ownerClass;
    private JvmClass[] paramClasses;
    private JvmClass returnClass;
    private Type[] paramTypesAsm;

    public JFunction(String name, Method method) {
        this.name = name;
        this.method = method;
        this.owner = method.getDeclaringClass();
        this.paramTypes = method.getParameterTypes();
        this.returnType = method.getReturnType();
    }

    @Override
    public void invoke(Object callArgs, Runnable paramInit) {
        throw new AssertionError("DEBUG");
    }

    @Override
    public Type returnType(PythonCompiler compiler) {
        return Type.getType(returnType);
    }

    @Override
    public JvmClass returnClass(PythonCompiler compiler) {
        if (!PythonCompiler.classCache.load(compiler, returnType)) {
            throw new CompilerException("Class '" + returnType.getName() + "' not found (" + compiler.getLocation(this) + ")");
        }
        return PythonCompiler.classCache.get(returnType);
    }

    @Override
    public Type[] parameterTypes(PythonCompiler compiler) {
        if (this.paramTypesAsm != null) return this.paramTypesAsm;
        Type[] types = new Type[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            types[i] = Type.getType(paramTypes[i]);
        }
        return this.paramTypesAsm = types;
    }

    @Override
    public JvmClass[] parameterClasses(PythonCompiler compiler) {
        if (this.paramClasses != null) return this.paramClasses;
        this.paramClasses = new JvmClass[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            if (!PythonCompiler.classCache.load(compiler, paramTypes[i]))
                throw new CompilerException("Class '" + paramTypes[i].getName() + "' not found (" + compiler.getLocation(this) + ")");
            this.paramClasses[i] = PythonCompiler.classCache.get(paramTypes[i]);
        }
        return this.paramClasses;
    }

    @Override
    public boolean isStatic() {
        return Modifier.isStatic(method.getModifiers());
    }

    @Override
    public JvmClass ownerClass(PythonCompiler compiler) {
        if (this.ownerClass != null) return this.ownerClass;
        if (!PythonCompiler.classCache.load(compiler, owner))
            throw new CompilerException("Class '" + owner.getName() + "' not found (" + compiler.getLocation(this) + ")");
        return this.ownerClass = PythonCompiler.classCache.get(owner);
    }

    @Override
    public boolean isDynamicCall() {
        return false;
    }

    @Override
    public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        return null;
    }

    @Override
    public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
        Type methodType = Type.getMethodType(returnType(compiler), parameterTypes(compiler));
        if (Modifier.isStatic(method.getModifiers())) {
            compiler.writer.invokeStatic(owner(compiler).type(compiler).getInternalName(), name, methodType.getDescriptor(), boxed);
            return;
        }
        compiler.writer.invokeVirtual(owner(compiler).type(compiler).getInternalName(), name, methodType.getDescriptor(), boxed);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Type type(PythonCompiler compiler) {
        return returnType(compiler);
    }

    @Override
    public Location location() {
        return new Location();
    }

    @Override
    public void expectReturnType(PythonCompiler compiler, JvmClass returnType, Location location) {
        if (!returnType.equals(returnClass(compiler)))
            throw new CompilerException("Expected return type " + returnType + " but got " + returnClass(compiler));
    }

    @Override
    public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit) {
        throw new CompilerException("Cannot set non-python JVM function " + compiler.getLocation(this));
    }

    @Override
    public JvmClass owner(PythonCompiler compiler) {
        if (this.ownerClass != null) return this.ownerClass;
        if (!PythonCompiler.classCache.load(compiler, owner))
            throw new CompilerException("Class '" + owner.getName() + "' not found (" + compiler.getLocation(this) + ")");
        this.ownerClass = PythonCompiler.classCache.get(owner);
        return this.ownerClass;
    }

    @Override
    public String toString() {
        return "JFunction< " + owner.getName() + "." + name + "(" + Arrays.stream(paramTypes).map(Class::getName).collect(Collectors.joining(", ")) + ") -> " + returnType.getName() + ">";
    }
}
