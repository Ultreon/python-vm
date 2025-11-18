package dev.ultreon.pyvm.compiler.util;

public class PyFunction implements PyClassMember {
    private final String name;

    public PyFunction(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
