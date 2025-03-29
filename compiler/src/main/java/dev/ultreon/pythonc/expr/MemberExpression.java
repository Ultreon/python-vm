package dev.ultreon.pythonc.expr;

import dev.ultreon.pythonc.JvmWriter;
import dev.ultreon.pythonc.Location;
import dev.ultreon.pythonc.PythonCompiler;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

public abstract class MemberExpression extends PyExpression {
    private final PyExpression parent;

    public MemberExpression(PyExpression parent, Location location) {
        super(location);
        this.parent = parent;
    }

    public PyExpression parent() {
        return parent;
    }

    public abstract @Nullable String name();

    @Override
    public void writeCode(PythonCompiler compiler, JvmWriter writer) {
        parent.write(compiler, writer);
    }

    public abstract void writeAttrOnly(PythonCompiler compiler, JvmWriter writer);
}
