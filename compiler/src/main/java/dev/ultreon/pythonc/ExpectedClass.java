package dev.ultreon.pythonc;

import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;

import dev.ultreon.pythonc.classes.JvmClass;

public class ExpectedClass extends JvmClass {
    private final ModulePath module;
    private final String name;

    private final List<Type> expectedInheritors = new ArrayList<>();

    public ExpectedClass(ModulePath module, String name) {
        super(module.getClass(name).asType(), name, new Location());
        this.module = module;
        this.name = name;
    }

    @Override
    public String className() {
        return type().getClassName();
    }

    @Override
    public boolean isInterface() {
        return false;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }

    @Override
    public boolean isEnum() {
        return false;
    }

    @Override
    public boolean isAnnotation() {
        return false;
    }

    @Override
    public boolean isRecord() {
        return false;
    }

    @Override
    public boolean isSealed() {
        return false;
    }

    @Override
    public boolean isModule() {
        return false;
    }

    @Override
    public boolean doesInherit(PythonCompiler compiler, JvmClass type) {
        this.expectedInheritors.add(type.type());
        return true;
    }

    public List<Type> expectedInheritors() {
        return expectedInheritors;
    }

    @Override
    public String name() {
        return "";
    }

    @Override
    public void writeReference(PythonCompiler compiler, JvmWriter writer) {
        writer.loadClass(type());
    }
}
