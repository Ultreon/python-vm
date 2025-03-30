package dev.ultreon.pythonc.classes

import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler
import org.objectweb.asm.Type

import java.lang.reflect.Modifier

class JavaClass extends JvmClass {
    private final Class<?> typeClass

    JavaClass(Type type, Class<?> typeClass, String name, Location location) {
        super(type, name, location)
        this.typeClass = typeClass
    }

    @Override
    boolean isInterface() {
        return typeClass.interface
    }

    @Override
    boolean isAbstract() {
        return Modifier.isAbstract(typeClass.modifiers)
    }

    @Override
    boolean isEnum() {
        return typeClass.enum
    }

    @Override
    boolean isAnnotation() {
        return typeClass.annotation
    }

    @Override
    boolean isRecord() {
        return typeClass.record
    }

    @Override
    boolean isSealed() {
        return typeClass.sealed
    }

    @Override
    boolean isModule() {
        return false
    }

    @Override
    boolean doesInherit(PythonCompiler compiler, JvmClass type) {
        if (this.type == type.type) return true
        for (JvmClass superClass : superClasses()) {
            if (superClass == null) continue
            if (superClass == type) return true
            if (superClass.doesInherit(compiler, type)) return true
        }
        for (JvmClass anInterface : interfaces()) {
            if (anInterface == null) continue
            if (anInterface == type) return true
            if (anInterface.doesInherit(compiler, type)) return true
        }
        return false
    }

    private JvmClass[] interfaces() {
        Class<?>[] interfaces = typeClass.interfaces
        JvmClass[] jvmClasses = new JvmClass[interfaces.length]
        for (int i = 0; i < interfaces.length; i++) {
            jvmClasses[i] = PythonCompiler.classCache.require(interfaces[i], Location.JAVA)
        }
        return jvmClasses
    }

    private JvmClass[] superClasses() {
        if (typeClass.superclass == Object.class || typeClass.superclass == null) {
            return new JvmClass[0]
        } else {
            Class<?> superclass = typeClass.superclass
            if (superclass == null) return new JvmClass[]{PythonCompiler.classCache.require(Type.getType(Object.class))}
            return new JvmClass[]{PythonCompiler.classCache.require(superclass, Location.JAVA)}
        }
    }

    JavaClass(String className, Class<?> aClass, Location location) {
        this(Type.getType(aClass), aClass, aClass.simpleName, location)
    }
}
