package dev.ultreon.pythonc.expr

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler
import org.jetbrains.annotations.Nullable

abstract class MemberExpression extends PyExpression {
    private final PyExpression parent

    MemberExpression(PyExpression parent, Location location) {
        super(location)
        this.parent = parent
    }

    PyExpression getParent() {
        return parent
    }

    abstract @Nullable String getName();

    @Override
    void writeCode(PythonCompiler compiler, JvmWriter writer) {
        parent.write(compiler, writer)
    }

    abstract void writeAttrOnly(PythonCompiler compiler, JvmWriter writer);
}
