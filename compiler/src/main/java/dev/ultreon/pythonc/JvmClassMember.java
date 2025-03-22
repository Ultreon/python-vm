package dev.ultreon.pythonc;

public interface JvmClassMember extends Symbol {
    JvmClass ownerClass(PythonCompiler compiler);

    JvmClass typeClass(PythonCompiler compiler);
}
