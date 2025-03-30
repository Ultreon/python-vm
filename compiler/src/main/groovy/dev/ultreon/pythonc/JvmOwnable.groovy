package dev.ultreon.pythonc;

import org.objectweb.asm.Type;

interface JvmOwnable {
    Type owner(PythonCompiler compiler);
}
