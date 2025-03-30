package dev.ultreon.pythonc.functions.param

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler
import dev.ultreon.pythonc.expr.PyExpression
import org.jetbrains.annotations.Nullable
import org.objectweb.asm.Type

class PyTypedDefaultParameter extends PyParameter {
    private final Type type
    private final PyExpression defaultValue
    private final Location location

    PyTypedDefaultParameter(String name, Type type, PyExpression defaultValue, Location location) {
        super(name)
        this.type = type
        this.defaultValue = defaultValue
        this.location = location
    }

    @Override
    Location getLocation() {
        return location
    }

    @Override
    void writeCode(PythonCompiler compiler, JvmWriter writer) {
        writer.loadValue(index(), type == null ? Type.getType(Object.class) : type)
    }

    @Override
    @Nullable Type type() {
        return type
    }

    @Override
    SpecialParameterType specialType() {
        return SpecialParameterType.NONE
    }
}
