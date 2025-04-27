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

    private JvmClass[] interfacesClasses() {
        def name = Class.forName(jvmType.className)
        def list = new ArrayList<JvmClass>()
        def interfaces = name.getInterfaces()
        for (Class<?> anInterface : interfaces) {
            list.add(PythonCompiler.classCache.require(anInterface, Location.JAVA))
        }
        return list.toArray(new JvmClass[0])
    }

    def JvmClass[] superClasses() {
        def name = Class.forName(jvmType.className)
        def list = new ArrayList<JvmClass>()
        def superclass = name.getSuperclass()
        if (superclass == null && type == Type.getObjectType("java/lang/Object")) return new JvmClass[]{}
        return new JvmClass[]{superclass == null ? PythonCompiler.classCache.object(PythonCompiler.current) : PythonCompiler.classCache.require(superclass, Location.JAVA)}
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
