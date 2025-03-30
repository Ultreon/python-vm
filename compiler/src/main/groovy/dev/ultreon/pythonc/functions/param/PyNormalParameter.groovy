package dev.ultreon.pythonc.functions.param

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler
import org.jetbrains.annotations.Nullable
import org.objectweb.asm.Type

class PyNormalParameter extends PyParameter {
    private Location location

    PyNormalParameter(String name, Location location) {
        super(name)
        this.location = location
    }

    @Override
    Location getLocation() {
        return location
    }

    @Override
    void writeCode(PythonCompiler compiler, JvmWriter writer) {
        writer.loadValue(index(), Type.getType(Object.class))
    }

    @Override
    @Nullable Type type() {
        return Type.getType(Object.class)
    }

    @Override
    SpecialParameterType specialType() {
        return SpecialParameterType.NONE
    }
}
