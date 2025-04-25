package dev.ultreon.pythonc.lang

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler
import org.objectweb.asm.Type

interface PyAST {
    default Type write(PythonCompiler compiler, JvmWriter writer) {
        this.writeCode compiler, writer
        writer.lastLocation(location)
        if (writer.context.popNeeded) {
            return writer.context.peek()
        }
        return Type.VOID_TYPE
    }

    abstract void writeCode(PythonCompiler compiler, JvmWriter writer);

    abstract Location getLocation();
}
