package dev.ultreon.pythonc.expr

import dev.ultreon.pythonc.*
import dev.ultreon.pythonc.lang.PyAST
import org.objectweb.asm.Type

abstract class PyExpression implements PyAST {
    private final Location location

    PyExpression(Location location) {
        this.location = location
    }


    void prepare(PythonCompiler compiler, JvmWriter writer) {

    }

    Type getType() {
        return Type.getType(Object.class)
    }

    abstract void writeCode(PythonCompiler compiler, JvmWriter writer);

    final Location getLocation() {
        return location
    }

    protected void writeFull(PythonCompiler compiler, JvmWriter writer) {
        this.prepare(compiler, writer)
        this.write(compiler, writer)
    }
}
