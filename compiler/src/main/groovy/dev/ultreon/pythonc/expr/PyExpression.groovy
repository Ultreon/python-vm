package dev.ultreon.pythonc.expr

import dev.ultreon.pythonc.*
import dev.ultreon.pythonc.classes.JvmClass
import dev.ultreon.pythonc.lang.PyAST
import org.objectweb.asm.Type

abstract class PyExpression implements PyAST {
    private final Location location

    PyExpression(Location location) {
        this.location = location
    }


    void prepare(PythonCompiler compiler, JvmWriter writer) {

    }

    MemberAttrExpr attr(String name, Location location = new Location()) {
        return new MemberAttrExpr(this, name, location)
    }

    static MemberCallExpr call(PyExpression parent, String name, List<PyExpression> arguments, Location location) {
        return new MemberCallExpr(parent.attr(name, location), arguments, location)
    }

    MemberCallExpr call(PyExpression parent, List<PyExpression> arguments, Location location) {
        return new MemberCallExpr(this, arguments, location)
    }

    Type getType() {
        return Type.getType(Object.class)
    }

    JvmClass getTypeClass() {
        return PythonCompiler.classCache.require(type)
    }

    abstract void writeCode(PythonCompiler compiler, JvmWriter writer);

    final Location getLocation() {
        return location
    }

    protected void writeFull(PythonCompiler compiler, JvmWriter writer) {
        this.prepare(compiler, writer)
        this.write(compiler, writer)
    }

    void delete(PythonCompiler pythonCompiler, JvmWriter jvmWriter) {
        // Deletes itself? How useless!
    }
}
