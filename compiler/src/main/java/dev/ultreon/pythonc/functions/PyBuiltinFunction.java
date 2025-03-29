package dev.ultreon.pythonc.functions;

import dev.ultreon.pythonc.JvmWriter;
import dev.ultreon.pythonc.Location;
import dev.ultreon.pythonc.PythonCompiler;
import dev.ultreon.pythonc.TODO;
import dev.ultreon.pythonc.classes.JvmClass;
import dev.ultreon.pythonc.classes.PyBuiltin;
import dev.ultreon.pythonc.expr.PyExpression;
import dev.ultreon.pythonc.functions.param.PyParameter;
import dev.ultreon.pythonc.modules.JvmModule;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.Map;

public class PyBuiltinFunction extends PyBaseFunction implements PyBuiltin {
    private final JvmClass owner;
    private final Location location;

    public PyBuiltinFunction(String name, JvmClass owner, PyParameter[] parameters, @Nullable JvmClass returnType, Location location) {
        super(name, parameters, returnType);
        this.owner = owner;

        this.location = location;
    }

    public PyBuiltinFunction(String asc, JvmModule module) {
        this(asc, module, PythonCompiler.BUILTIN_FUNCTION_PARAMETER_TYPES, null, new Location());
    }

    @Override
    public boolean isStatic() {
        return true;
    }

    @Override
    public void writeFunction(PythonCompiler compiler, JvmWriter writer) {
        throw new RuntimeException("Builtin function cannot be written");
    }

    @Override
    public Location location() {
        return location;
    }

    @Override
    public void writeCall(PythonCompiler compiler, JvmWriter writer, List<PyExpression> args, Map<String, PyExpression> kwargs) {
        writer.createArray(args, Type.getType(Object.class));
        writer.createKwargs(kwargs);
        writer.dynamicBuiltinCall(name());
    }

    @Override
    public String pyName() {
        return name();
    }

    @Override
    public String javaName() {
        return name();
    }
}
