package dev.ultreon.pythonc.functions.param;

import dev.ultreon.pythonc.JvmWriter;
import dev.ultreon.pythonc.Location;
import dev.ultreon.pythonc.PythonCompiler;
import dev.ultreon.pythonc.TODO;
import dev.ultreon.pythonc.expr.PyExpression;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.Map;

public class PyKwargsParameter extends PyParameter {
    private final Location location;

    public PyKwargsParameter(String name) {
        super(name);
        this.location = new Location();
    }

    public PyKwargsParameter(String name, Location location) {
        super(name);
        this.location = location;
    }

    @Override
    public void writeCode(PythonCompiler compiler, JvmWriter writer) {
        throw new TODO();
    }

    @Override
    public Location location() {
        return location;
    }

    @Override
    public @Nullable Type type() {
        return Type.getType(Map.class);
    }

    @Override
    public SpecialParameterType specialType() {
        return SpecialParameterType.KWARGS;
    }
}
