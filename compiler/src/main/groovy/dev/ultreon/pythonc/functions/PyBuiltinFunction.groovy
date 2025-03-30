package dev.ultreon.pythonc.functions

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler
import dev.ultreon.pythonc.classes.JvmClass
import dev.ultreon.pythonc.classes.PyBuiltin
import dev.ultreon.pythonc.expr.PyExpression
import dev.ultreon.pythonc.functions.param.PyParameter
import dev.ultreon.pythonc.modules.JvmModule
import org.jetbrains.annotations.Nullable
import org.objectweb.asm.Type

class PyBuiltinFunction extends PyBaseFunction implements PyBuiltin {
    private final JvmClass owner
    private final Location location

    PyBuiltinFunction(String name, JvmClass owner, PyParameter[] parameters, @Nullable JvmClass returnType, Location location) {
        super(name, parameters, returnType)
        this.owner = owner

        this.location = location
    }

    PyBuiltinFunction(String asc, JvmModule module) {
        this(asc, module, PythonCompiler.BUILTIN_FUNCTION_PARAMETER_TYPES, null, new Location())
    }

    @Override
    boolean isStatic() {
        return true
    }

    @Override
    void writeFunction(PythonCompiler compiler, JvmWriter writer) {
        throw new RuntimeException("Builtin function cannot be written")
    }

    @Override
    Location getLocation() {
        return location
    }

    @Override
    void writeCall(PythonCompiler compiler, JvmWriter writer, List<PyExpression> args, Map<String, PyExpression> kwargs) {
        writer.createArray(args, Type.getType(Object.class))
        writer.createKwargs(kwargs)
        writer.dynamicBuiltinCall(name)
    }

    @Override
    String getPyName() {
        return name
    }

    @Override
    String getJavaName() {
        return name
    }
}
