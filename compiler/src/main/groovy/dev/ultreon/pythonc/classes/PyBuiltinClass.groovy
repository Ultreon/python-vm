package dev.ultreon.pythonc.classes

import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler
import org.objectweb.asm.Type

class PyBuiltinClass extends JvmClass implements PyBuiltin {
    private final Type jvmType
    public String pyName
    public String javaName

    PyBuiltinClass(Type jvmType, String pyName, String javaName) {
        this(null, jvmType, pyName, javaName)
    }

    PyBuiltinClass(Type extType, Type jvmType, String pyName, String javaName) {
        super(jvmType, pyName, Location.BUILTIN)
        this.jvmType = jvmType
        this.pyName = pyName
        this.javaName = javaName
    }

    @Override
    boolean isInterface() {
        return false
    }

    @Override
    boolean isAbstract() {
        return false
    }

    @Override
    boolean isEnum() {
        return false
    }

    @Override
    boolean isAnnotation() {
        return false
    }

    @Override
    boolean isRecord() {
        return false
    }

    @Override
    boolean isSealed() {
        return false
    }

    @Override
    boolean isModule() {
        return false
    }

    @Override
    boolean doesInherit(PythonCompiler compiler, JvmClass type) {
        if (this == type) return true
        if (this.type == type.type) return true
        for (JvmClass superClass : superClasses()) {
            if (superClass == null) continue
            if (superClass == type) return true
            if (superClass.doesInherit(compiler, type)) {
                return true
            }
        }
        for (JvmClass javaClass : interfacesClasses()) {
            if (javaClass == null) continue
            if (javaClass == type) return true
            if (javaClass.doesInherit(compiler, type)) {
                return true
            }
        }
        return false
    }

    private static JvmClass[] interfacesClasses() {
        return new JvmClass[0]
    }

    static JvmClass[] superClasses() {
        return new JvmClass[0]
    }

    @Override
    String getPyName() {
        return pyName
    }

    @Override
    String getJavaName() {
        return javaName
    }
}
