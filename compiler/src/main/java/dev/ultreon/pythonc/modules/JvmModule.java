package dev.ultreon.pythonc.modules;

import dev.ultreon.pythonc.*;
import dev.ultreon.pythonc.classes.JvmClass;
import dev.ultreon.pythonc.expr.PyExpression;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JvmModule extends JvmClass {
    private final ModulePath path;
    private final List<JvmModule> subModules = new ArrayList<>();

    public JvmModule(ModulePath path, Location location) {
        super(path.asType(), path.getName(), location);
        this.path = path;
    }

    public ModulePath path() {
        return path;
    }

    public List<JvmModule> subModules() {
        return subModules;
    }

    public void addSubModule(JvmModule module) {
        subModules.add(module);
    }

    public String module() {
        return path.toString();
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
        return true;
    }

    @Override
    public boolean doesInherit(PythonCompiler compiler, JvmClass type) {
        return false;
    }
}
