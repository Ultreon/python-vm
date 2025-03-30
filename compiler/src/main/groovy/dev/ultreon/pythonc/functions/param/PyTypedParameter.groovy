package dev.ultreon.pythonc.functions.param

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler
import dev.ultreon.pythonc.classes.JvmClass
import org.jetbrains.annotations.Nullable
import org.objectweb.asm.Type

class PyTypedParameter extends PyParameter {
    private final JvmClass type
    private final Location location

    PyTypedParameter(String name, JvmClass type, Location location) {
        super(name)
        this.type = type
        this.location = location
    }

    @Override
    Location getLocation() {
        return location
    }

    @Override
    void writeCode(PythonCompiler compiler, JvmWriter writer) {
        writer.loadValue(index(), type == null ? Type.getType(Object.class) : type.type)
    }

    @Override
    @Nullable Type type() {
        return type.type
    }

    @Override
    SpecialParameterType specialType() {
        return SpecialParameterType.NONE
    }
}
