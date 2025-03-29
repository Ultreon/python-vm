package dev.ultreon.pythonc.functions.param;

import dev.ultreon.pythonc.JvmWriter;
import dev.ultreon.pythonc.Location;
import dev.ultreon.pythonc.PythonCompiler;
import dev.ultreon.pythonc.TODO;
import dev.ultreon.pythonc.expr.PyExpression;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

public class PyDefaultedParameter extends PyParameter {
    private Location location;

    public PyDefaultedParameter(String name, PyExpression defaultValue, Location location) {
        super(name);
        this.location = location;
        throw new TODO();
    }

    @Override
    public Type write(PythonCompiler compiler, JvmWriter writer) {
        throw new TODO();
    }

    @Override
    public Location location() {
        return location;
    }

    @Override
    public void writeCode(PythonCompiler compiler, JvmWriter writer) {
        throw new TODO();
    }

    @Override
    public @Nullable Type type() {
        return Type.getType(Object.class);
    }

    @Override
    public SpecialParameterType specialType() {
        return SpecialParameterType.NONE;
    }
}
