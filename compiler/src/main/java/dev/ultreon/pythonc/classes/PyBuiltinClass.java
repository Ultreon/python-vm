package dev.ultreon.pythonc.classes;

import dev.ultreon.pythonc.Location;
import dev.ultreon.pythonc.PythonCompiler;
import org.objectweb.asm.Type;

public class PyBuiltinClass extends JvmClass implements PyBuiltin {
    private final Type jvmType;
    public String pyName;
    public String javaName;

    public PyBuiltinClass(Type jvmType, String pyName, String javaName) {
        this(null, jvmType, pyName, javaName);
    }

    public PyBuiltinClass(Type extType, Type jvmType, String pyName, String javaName) {
        super(jvmType, pyName, Location.BUILTIN);
        this.jvmType = jvmType;
        this.pyName = pyName;
        this.javaName = javaName;
    }

    @Override
    public boolean isInterface() {
        return false;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }

    @Override
    public boolean isEnum() {
        return false;
    }

    @Override
    public boolean isAnnotation() {
        return false;
    }

    @Override
    public boolean isRecord() {
        return false;
    }

    @Override
    public boolean isSealed() {
        return false;
    }

    @Override
    public boolean isModule() {
        return false;
    }

    @Override
    public boolean doesInherit(PythonCompiler compiler, JvmClass type) {
        if (this.equals(type)) return true;
        if (this.type().equals(type.type())) return true;
        for (JvmClass superClass : superClasses()) {
            if (superClass == null) continue;
            if (superClass.equals(type)) return true;
            if (superClass.doesInherit(compiler, type)) {
                return true;
            }
        }
        for (JvmClass javaClass : interfacesClasses()) {
            if (javaClass == null) continue;
            if (javaClass.equals(type)) return true;
            if (javaClass.doesInherit(compiler, type)) {
                return true;
            }
        }
        return false;
    }

    private JvmClass[] interfacesClasses() {
        return new JvmClass[0];
    }

    public JvmClass[] superClasses() {
        return new JvmClass[0];
    }

    @Override
    public String pyName() {
        return pyName;
    }

    @Override
    public String javaName() {
        return javaName;
    }
}
