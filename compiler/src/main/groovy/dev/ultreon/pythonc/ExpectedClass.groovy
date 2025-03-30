package dev.ultreon.pythonc

import org.objectweb.asm.Type
import dev.ultreon.pythonc.classes.JvmClass

class ExpectedClass extends JvmClass {
    private final ModulePath module
    private final String name

    private final List<Type> expectedInheritors = new ArrayList<>()

    ExpectedClass(ModulePath module, String name) {
        super(module.getClass(name).asType(), name, new Location())
        this.module = module
        this.name = name
    }

    @Override
    String className() {
        return type.className
    }

    @Override
    boolean isInterface() {
        return false
    }

    @Override
    boolean isAbstract() {
        return false
    }

    @Override
    boolean isEnum() {
        return false
    }

    @Override
    boolean isAnnotation() {
        return false
    }

    @Override
    boolean isRecord() {
        return false
    }

    @Override
    boolean isSealed() {
        return false
    }

    @Override
    boolean isModule() {
        return false
    }

    @Override
    boolean doesInherit(PythonCompiler compiler, JvmClass type) {
        this.expectedInheritors.add(type.type)
        return true
    }

    List<Type> expectedInheritors() {
        return expectedInheritors
    }

    @Override
    String getName() {
        return ""
    }

    @Override
    void writeReference(PythonCompiler compiler, JvmWriter writer) {
        writer.loadClass(type)
    }
}
