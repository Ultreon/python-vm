package dev.ultreon.pythonc;

import dev.ultreon.pythonc.lang.PyAST;

public record PyNameReference(String name, Location location) implements PyAST {

    @Override
    public void writeCode(PythonCompiler compiler, JvmWriter writer) {
        throw new RuntimeException("Not implemented");
    }
}
