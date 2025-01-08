package dev.ultreon.pythonc;

import org.objectweb.asm.Type;

public interface Ownable {
    Type owner(PythonCompiler compiler);
}
