package dev.ultreon.pythonc.modules


import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.ModulePath
import dev.ultreon.pythonc.PythonCompiler
import dev.ultreon.pythonc.classes.JvmClass
import org.objectweb.asm.Type

class JvmModule extends JvmClass {
    private final ModulePath path
    private final List<JvmModule> subModules = new ArrayList<>()

    JvmModule(ModulePath path, Location location) {
        super(path.asType(), path.name, location)
        this.path = path
    }

    ModulePath path() {
        return path
    }

    List<JvmModule> subModules() {
        return subModules
    }

    void addSubModule(JvmModule module) {
        subModules.add(module)
    }

    String module() {
        return path.toString()
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
        return true
    }

    @Override
    boolean doesInherit(PythonCompiler compiler, JvmClass type) {
        return false
    }
}
