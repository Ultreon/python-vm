package dev.ultreon.pythonc.functions.param

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler
import dev.ultreon.pythonc.TODO
import org.jetbrains.annotations.Nullable
import org.objectweb.asm.Type

class PyKwargsParameter extends PyParameter {
    private final Location location

    PyKwargsParameter(String name) {
        super(name)
        this.location = new Location()
    }

    PyKwargsParameter(String name, Location location) {
        super(name)
        this.location = location
    }

    @Override
    void writeCode(PythonCompiler compiler, JvmWriter writer) {
        throw new TODO()
    }

    @Override
    Location getLocation() {
        return location
    }

    @Override
    @Nullable Type type() {
        return Type.getType(Map.class)
    }

    @Override
    SpecialParameterType specialType() {
        return SpecialParameterType.KWARGS
    }
}
