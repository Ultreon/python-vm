package dev.ultreon.pythonc.lang;

import dev.ultreon.pythonc.JvmWriter;
import dev.ultreon.pythonc.Location;
import dev.ultreon.pythonc.PythonCompiler;
import org.objectweb.asm.Type;

public interface PyAST {
    default Type write(PythonCompiler compiler, JvmWriter writer) {
        writer.lastLocation(location());
        this.writeCode(compiler, writer);
        if (writer.getContext().needsPop()) {
            return writer.getContext().peek();
        }
        return Type.VOID_TYPE;
    }

    void writeCode(PythonCompiler compiler, JvmWriter writer);

    Location location();
}
