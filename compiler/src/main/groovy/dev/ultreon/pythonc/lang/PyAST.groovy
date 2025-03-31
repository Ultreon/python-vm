package dev.ultreon.pythonc.lang

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler
import org.objectweb.asm.Type

trait PyAST {
    Type write(PythonCompiler compiler, JvmWriter writer) {
        writer.lastLocation(location)
        this.writeCode compiler, writer
        if (writer.context.popNeeded) {
            return writer.context.peek()
        }
        return Type.VOID_TYPE
    }

    abstract void writeCode(PythonCompiler compiler, JvmWriter writer);

    abstract Location getLocation();
}
