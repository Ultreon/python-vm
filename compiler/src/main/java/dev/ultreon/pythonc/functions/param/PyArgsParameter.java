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

public class PyArgsParameter extends PyParameter {
    private final Location location;

    public PyArgsParameter(String name) {
        this(name, new Location());
    }

    @Override
    public void writeCode(PythonCompiler compiler, JvmWriter writer) {
        compiler.writer.loadValue(0, Type.getType(Object[].class));
    }

    @Override
    public Location location() {
        return location;
    }

    @Override
    public @Nullable Type type() {
        return Type.getType(Object[].class);
    }

    public PyArgsParameter(String name, Location location) {
        super(name);
        this.location = location;
    }

    @Override
    public SpecialParameterType specialType() {
        return SpecialParameterType.ARGS;
    }
}
