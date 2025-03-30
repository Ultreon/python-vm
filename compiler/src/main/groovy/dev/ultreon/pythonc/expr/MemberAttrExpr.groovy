package dev.ultreon.pythonc.expr

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler

class MemberAttrExpr extends MemberExpression implements Settable {
    private final String name

    MemberAttrExpr(PyExpression parent, String name, Location location) {
        super(parent, location)
        this.name = name
    }

    String getName() {
        return name
    }

    @Override
    void set(PythonCompiler compiler, JvmWriter writer, PyExpression expr) {
        parent.write(compiler, writer)
        expr.writeFull(compiler, writer)
        writer.dynamicSetAttr(name)
        compiler.checkPop(location)
    }

    @Override
    void writeCode(PythonCompiler compiler, JvmWriter writer) {
        parent.write(compiler, writer)
        writer.dynamicGetAttr(name)
        compiler.checkNoPop(location)
    }

    @Override
    void writeAttrOnly(PythonCompiler compiler, JvmWriter writer) {
        writeCode(compiler, writer)
    }
}
