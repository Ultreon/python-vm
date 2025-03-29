package dev.ultreon.pythonc.expr;

import dev.ultreon.pythonc.JvmWriter;
import dev.ultreon.pythonc.PythonCompiler;
import dev.ultreon.pythonc.lang.PyAST;

import java.util.List;
import java.util.Map;

public interface PySymbol extends PyAST {
    String name();
    void writeCall(PythonCompiler compiler, JvmWriter writer, List<PyExpression> args, Map<String, PyExpression> kwargs);
}
