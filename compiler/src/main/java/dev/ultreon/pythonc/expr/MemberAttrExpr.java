package dev.ultreon.pythonc.expr;

import dev.ultreon.pythonc.JvmWriter;
import dev.ultreon.pythonc.Location;
import dev.ultreon.pythonc.PythonCompiler;
import org.objectweb.asm.Type;

public class MemberAttrExpr extends MemberExpression implements Settable {
    private final String name;

    public MemberAttrExpr(PyExpression parent, String name, Location location) {
        super(parent, location);
        this.name = name;
    }

    public String name() {
        return name;
    }

    @Override
    public void set(PythonCompiler compiler, JvmWriter writer, PyExpression expr) {
        parent().write(compiler, writer);
        expr.writeFull(compiler, writer);
        writer.dynamicSetAttr(name);
        compiler.checkPop(location());
    }

    @Override
    public void writeCode(PythonCompiler compiler, JvmWriter writer) {
        parent().write(compiler, writer);
        writer.dynamicGetAttr(name);
        compiler.checkNoPop(location());
    }

    @Override
    public void writeAttrOnly(PythonCompiler compiler, JvmWriter writer) {
        writeCode(compiler, writer);
    }
}
