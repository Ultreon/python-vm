package dev.ultreon.pythonc;

public interface JvmField extends JvmOwnable, JvmClassMember {
    JvmClass cls(PythonCompiler pythonCompiler);
}
