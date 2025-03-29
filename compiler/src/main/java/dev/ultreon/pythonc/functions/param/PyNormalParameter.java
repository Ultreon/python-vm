package dev.ultreon.pythonc.functions.param;

import dev.ultreon.pythonc.JvmWriter;
import dev.ultreon.pythonc.Location;
import dev.ultreon.pythonc.PythonCompiler;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

public class PyNormalParameter extends PyParameter {
    private Location location;

    public PyNormalParameter(String name, Location location) {
        super(name);
        this.location = location;
    }

    @Override
    public Location location() {
        return location;
    }

    @Override
    public void writeCode(PythonCompiler compiler, JvmWriter writer) {
        writer.loadValue(index(), Type.getType(Object.class));
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
