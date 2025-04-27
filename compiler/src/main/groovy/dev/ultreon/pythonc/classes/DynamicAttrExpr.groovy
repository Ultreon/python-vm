package dev.ultreon.pythonc.classes

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler
import dev.ultreon.pythonc.expr.PyExpression

class DynamicAttrExpr extends PyExpression {
    private String name
    private JvmClass jvmClass

    DynamicAttrExpr(JvmClass pyClass, String name, Location location) {
        super(location)

        this.jvmClass = pyClass
        this.name = name
    }

    JvmClass typeClass() {
        return jvmClass
    }

    String getName() {
        return name
    }

    @Override
    void writeCode(PythonCompiler compiler, JvmWriter writer) {
        writer.loadClass(jvmClass)
        writer.dynamicGetDynAttr(name)
    }
}
