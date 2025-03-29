package dev.ultreon.pythonc.functions.param;

import dev.ultreon.pythonc.JvmWriter;
import dev.ultreon.pythonc.Location;
import dev.ultreon.pythonc.PythonCompiler;
import dev.ultreon.pythonc.classes.JvmClass;
import dev.ultreon.pythonc.expr.PyExpression;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.Map;

public class PyTypedParameter extends PyParameter {
    private final JvmClass type;
    private final Location location;

    public PyTypedParameter(String name, JvmClass type, Location location) {
        super(name);
        this.type = type;
        this.location = location;
    }

    @Override
    public Location location() {
        return location;
    }

    @Override
    public void writeCode(PythonCompiler compiler, JvmWriter writer) {
        writer.loadValue(index(), type == null ? Type.getType(Object.class) : type.type());
    }

    @Override
    public @Nullable Type type() {
        return type.type();
    }

    @Override
    public SpecialParameterType specialType() {
        return SpecialParameterType.NONE;
    }
}
