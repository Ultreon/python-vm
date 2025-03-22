package dev.ultreon.pythonc;

interface JvmCallable extends Symbol {
    JvmClass owner(PythonCompiler compiler);
}
