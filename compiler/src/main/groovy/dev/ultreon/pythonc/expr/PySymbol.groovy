package dev.ultreon.pythonc.expr;

import dev.ultreon.pythonc.JvmWriter;
import dev.ultreon.pythonc.PythonCompiler;
import dev.ultreon.pythonc.lang.PyAST

interface PySymbol extends PyAST {
    String getName()
    void writeCall(PythonCompiler compiler, JvmWriter writer, List<PyExpression> args, Map<String, PyExpression> kwargs)
}
