package dev.ultreon.pythonc.expr

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler
import dev.ultreon.pythonc.classes.JvmClass

class CallExpr extends PyExpression {
    private final JvmClass owner
    private final String name
    private final List<PyExpression> arguments

    CallExpr(JvmClass owner, String name, List<PyExpression> arguments, Location location) {
        super(location)
        this.owner = owner
        this.name = name
        this.arguments = arguments
    }

    @Override
    void writeCode(PythonCompiler compiler, JvmWriter writer) {
        writer.dynamicCall(owner, name, arguments)
    }
}
