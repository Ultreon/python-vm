package dev.ultreon.pythonc.expr;

import dev.ultreon.pythonc.JvmWriter;
import dev.ultreon.pythonc.Location;
import dev.ultreon.pythonc.PythonCompiler;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

public class MemberItemExpr extends MemberExpression {
    private final PyExpression key;

    public MemberItemExpr(PyExpression parent, PyExpression key, Location location) {
        super(parent, location);
        this.key = key;
    }

    public PyExpression key() {
        return key;
    }

    public Type write(PythonCompiler compiler, JvmWriter writer) {
        super.write(compiler, writer);
        key.writeFull(compiler, writer);
        writer.dynamicGetItem();
        return Type.getType(Object.class);
    }

    @Override
    public @Nullable String name() {
        return null;
    }

    @Override
    public void writeAttrOnly(PythonCompiler compiler, JvmWriter writer) {
        super.writeCode(compiler, writer);
    }
}
