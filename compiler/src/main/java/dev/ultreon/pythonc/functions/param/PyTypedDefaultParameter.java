package dev.ultreon.pythonc.functions.param;

import dev.ultreon.pythonc.JvmWriter;
import dev.ultreon.pythonc.Location;
import dev.ultreon.pythonc.PythonCompiler;
import dev.ultreon.pythonc.TODO;
import dev.ultreon.pythonc.expr.PyExpression;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

public class PyTypedDefaultParameter extends PyParameter {
    private final Type type;
    private final PyExpression defaultValue;
    private final Location location;

    public PyTypedDefaultParameter(String name, Type type, PyExpression defaultValue, Location location) {
        super(name);
        this.type = type;
        this.defaultValue = defaultValue;
        this.location = location;
    }

    @Override
    public Location location() {
        return location;
    }

    @Override
    public void writeCode(PythonCompiler compiler, JvmWriter writer) {
        writer.loadValue(index(), type == null ? Type.getType(Object.class) : type);
    }

    @Override
    public @Nullable Type type() {
        return type;
    }

    @Override
    public SpecialParameterType specialType() {
        return SpecialParameterType.NONE;
    }
}
