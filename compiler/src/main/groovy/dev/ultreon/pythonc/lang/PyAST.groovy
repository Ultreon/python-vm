package dev.ultreon.pythonc.lang

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler
import org.objectweb.asm.Type

interface PyAST {
    default Type write(PythonCompiler compiler, JvmWriter writer) {
        writer.lastLocation(location)
        this.writeCode compiler, writer
        if (writer.context.popNeeded) {
            return writer.context.peek()
        }
        return Type.VOID_TYPE
    }

    void writeCode(PythonCompiler compiler, JvmWriter writer);

    Location getLocation();
}
