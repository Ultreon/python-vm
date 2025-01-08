package dev.ultreon.pythonc;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.Modifier;
import java.util.Objects;

final class JClass implements JvmClass {
    private final String className;
    private final Type asmType;
    private final Class<?> type;

    JClass(String className) {
        this.className = className;
        this.asmType = Type.getType("L" + className + ";");
        try {
            this.type = Class.forName(className.replace("/", "."), false, getClass().getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new CompilerException("JVM class not found: " + className);
        }
    }

    @Override
    public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        return null;
    }

    @Override
    public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
        // Do <name>.class
        mv.visitLdcInsn(Type.getType("L" + className + ";"));
    }

    public Type asmType() {
        return asmType;
    }

    public Class<?> getType() {
        return type;
    }

    @Override
    public int lineNo() {
        return 0;
    }

    @Override
    public String name() {
        return className;
    }

    @Override
    public Type type(PythonCompiler compiler) {
        return asmType;
    }

    @Override
    public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit) {

    }

    public boolean isInterface(PythonCompiler compiler) {
        try {
            Class<?> aClass = Class.forName(className.replace("/", "."), false, getClass().getClassLoader());
            return aClass.isInterface();
        } catch (ClassNotFoundException e) {
            throw new CompilerException("JVM Class '" + className + "' not found (" + compiler.getLocation(this) + ")");
        }
    }

    @Override
    public boolean isInterface() {
        return type.isInterface();
    }

    @Override
    public boolean isAbstract() {
        return Modifier.isAbstract(type.getModifiers());
    }

    @Override
    public boolean isEnum() {
        return type.isEnum();
    }

    @Override
    public boolean doesInherit(Class<?> type) {
        return type.isAssignableFrom(this.type);
    }

    public String className() {
        return className;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (JClass) obj;
        return Objects.equals(this.className, that.className);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className);
    }

    @Override
    public String toString() {
        return "JClass[" +
               "className=" + className + ']';
    }

}
