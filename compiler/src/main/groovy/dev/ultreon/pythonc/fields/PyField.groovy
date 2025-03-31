package dev.ultreon.pythonc.fields

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler
import dev.ultreon.pythonc.classes.JvmClassCompilable
import dev.ultreon.pythonc.classes.PyClass
import org.objectweb.asm.Type

import static org.objectweb.asm.Type.getType as typeOf

class PyField implements JvmField {
    private JvmClassCompilable pyClass
    private String name;
    private Type type;
    private boolean isStatic
    private Location location

    PyField(PyClass pyClass, String name, Type type, boolean isStatic, Location location) {
        this.pyClass = pyClass
        this.name = name
        this.type = type
        this.isStatic = isStatic
        this.location = location
    }

    String getName() {
        return name
    }

    JvmClassCompilable getPyClass() {
        return pyClass
    }

    boolean isStatic() {
        return isStatic
    }

    @Override
    void writeCode(PythonCompiler compiler, JvmWriter writer) {
        writer.dynamicGetAttr(name)
    }

    @Override
    Location getLocation() {
        return location
    }

    @Override
    Type getType() {
        return type == null ? typeOf(Object) : type
    }
}
