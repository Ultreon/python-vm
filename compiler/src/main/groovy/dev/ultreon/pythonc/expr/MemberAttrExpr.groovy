package dev.ultreon.pythonc.expr

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler
import dev.ultreon.pythonc.classes.JvmClass
import org.objectweb.asm.Type

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
    Type getType() {
        if (parent instanceof JvmClass) {
            return parent.namedField(name)
        }

        return Type.getType(Object)
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

    String toString() {
        StringBuilder builder = new StringBuilder()

        builder.append(Location.ANSI_RED)
        builder.append("MemberAttr ")
        builder.append(Location.ANSI_RESET)
        builder.append("[")
        builder.append(Location.ANSI_PURPLE)
        builder.append(parent.toString())
        builder.append(Location.ANSI_RESET)
        builder.append("] ")
        builder.append(Location.ANSI_PURPLE)
        builder.append(name)
        builder.append(Location.ANSI_RESET)

        return builder.toString()
    }
}
