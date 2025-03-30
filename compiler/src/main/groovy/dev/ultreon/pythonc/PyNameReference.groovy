package dev.ultreon.pythonc

import dev.ultreon.pythonc.lang.PyAST

import static java.util.Objects.equals

final class PyNameReference implements PyAST {
    private final String name
    private final Location location

    PyNameReference(String name, Location location) {
        this.name = name
        this.location = location
    }

    @Override
    void writeCode(PythonCompiler compiler, JvmWriter writer) {
        throw new RuntimeException("Not implemented")
    }

    String getName() {
        return name
    }

    @Override
    Location getLocation() {
        return location
    }

    @Override
    boolean equals(Object obj) {
        if (obj == this) return true
        if (obj == null || obj.class != this.class) return false
        var that = (PyNameReference) obj
        return equals(this.name, that.name) &&
                equals(this.location, that.location)
    }

    @Override
    int hashCode() {
        return Objects.hash(name, location)
    }

    @Override
    String toString() {
        return "PyNameReference[" +
                "name=" + name + ", " +
                "location=" + location + ']'
    }

}
