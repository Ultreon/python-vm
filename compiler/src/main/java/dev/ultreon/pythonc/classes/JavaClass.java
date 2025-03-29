package dev.ultreon.pythonc.classes;

import dev.ultreon.pythonc.Location;
import dev.ultreon.pythonc.PythonCompiler;
import org.objectweb.asm.Type;

import java.lang.reflect.Modifier;

public class JavaClass extends JvmClass {
    private final Class<?> typeClass;

    public JavaClass(Type type, Class<?> typeClass, String name, Location location) {
        super(type, name, location);
        this.typeClass = typeClass;
    }

    @Override
    public boolean isInterface() {
        return typeClass.isInterface();
    }

    @Override
    public boolean isAbstract() {
        return Modifier.isAbstract(typeClass.getModifiers());
    }

    @Override
    public boolean isEnum() {
        return typeClass.isEnum();
    }

    @Override
    public boolean isAnnotation() {
        return typeClass.isAnnotation();
    }

    @Override
    public boolean isRecord() {
        return typeClass.isRecord();
    }

    @Override
    public boolean isSealed() {
        return typeClass.isSealed();
    }

    @Override
    public boolean isModule() {
        return false;
    }

    @Override
    public boolean doesInherit(PythonCompiler compiler, JvmClass type) {
        if (this.type().equals(type.type())) return true;
        for (JvmClass superClass : superClasses()) {
            if (superClass == null) continue;
            if (superClass.equals(type)) return true;
            if (superClass.doesInherit(compiler, type)) return true;
        }
        for (JvmClass anInterface : interfaces()) {
            if (anInterface == null) continue;
            if (anInterface.equals(type)) return true;
            if (anInterface.doesInherit(compiler, type)) return true;
        }
        return false;
    }

    private JvmClass[] interfaces() {
        Class<?>[] interfaces = typeClass.getInterfaces();
        JvmClass[] jvmClasses = new JvmClass[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            jvmClasses[i] = PythonCompiler.classCache.require(interfaces[i], Location.JAVA);
        }
        return jvmClasses;
    }

    private JvmClass[] superClasses() {
        if (typeClass.getSuperclass() == Object.class || typeClass.getSuperclass() == null) {
            return new JvmClass[0];
        } else {
            Class<?> superclass = typeClass.getSuperclass();
            if (superclass == null) return new JvmClass[]{PythonCompiler.classCache.require(Type.getType(Object.class))};
            return new JvmClass[]{PythonCompiler.classCache.require(superclass, Location.JAVA)};
        }
    }

    public JavaClass(String className, Class<?> aClass, Location location) {
        this(Type.getType(aClass), aClass, aClass.getSimpleName(), location);
    }
}
