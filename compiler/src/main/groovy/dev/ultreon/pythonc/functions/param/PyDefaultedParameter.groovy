package dev.ultreon.pythonc.functions.param

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler
import dev.ultreon.pythonc.TODO
import dev.ultreon.pythonc.expr.PyExpression
import org.jetbrains.annotations.Nullable
import org.objectweb.asm.Type

class PyDefaultedParameter extends PyParameter {
    private Location location

    PyDefaultedParameter(String name, PyExpression defaultValue, Location location) {
        super(name)
        this.location = location
        throw new TODO()
    }

    @Override
    Type write(PythonCompiler compiler, JvmWriter writer) {
        throw new TODO()
    }

    @Override
    Location getLocation() {
        return location
    }

    @Override
    void writeCode(PythonCompiler compiler, JvmWriter writer) {
        throw new TODO()
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
