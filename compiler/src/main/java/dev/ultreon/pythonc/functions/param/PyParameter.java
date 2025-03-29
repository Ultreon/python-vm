package dev.ultreon.pythonc.functions.param;

import dev.ultreon.pythonc.JvmWriter;
import dev.ultreon.pythonc.PythonCompiler;
import dev.ultreon.pythonc.classes.JvmClass;
import dev.ultreon.pythonc.expr.PyExpression;
import dev.ultreon.pythonc.expr.PySymbol;
import dev.ultreon.pythonc.lang.PyAST;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.Map;

public abstract class PyParameter implements PyAST, PySymbol {
    public int index;
    private final String name;

    protected PyParameter(String name) {
        this.name = name;
    }

    public int index() {
        return index;
    }

    public String name() {
        return name;
    }

    @Override
    public abstract void writeCode(PythonCompiler compiler, JvmWriter writer);

    public abstract @Nullable Type type();

    public final @Nullable JvmClass typeClass() {
        Type type = type();
        if (type == null) return null;
        return PythonCompiler.classCache.require(PythonCompiler.current(), type);
    }

    public abstract SpecialParameterType specialType();

    @Override
    public void writeCall(PythonCompiler compiler, JvmWriter writer, List<PyExpression> args, Map<String, PyExpression> kwargs) {
        write(compiler, writer);
        writer.createArgs(args);
        writer.createKwargs(kwargs);
        writer.dynamicCall();
    }
}
