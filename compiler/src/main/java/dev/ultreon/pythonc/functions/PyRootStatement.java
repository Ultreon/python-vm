package dev.ultreon.pythonc.functions;

import dev.ultreon.pythonc.JvmWriter;
import dev.ultreon.pythonc.PythonCompiler;
import dev.ultreon.pythonc.lang.PyAST;

public abstract class PyRootStatement implements PyAST {
    public abstract void writeStatement(PythonCompiler compiler);

    @Override
    public final void writeCode(PythonCompiler compiler, JvmWriter writer) {
        writeStatement(compiler);
    }
}
