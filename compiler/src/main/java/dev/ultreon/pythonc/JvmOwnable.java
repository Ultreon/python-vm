package dev.ultreon.pythonc;

import org.objectweb.asm.Type;

public interface JvmOwnable {
    Type owner(PythonCompiler compiler);
}
