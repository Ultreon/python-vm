package dev.ultreon.pythonc


import org.objectweb.asm.Type

final class ClassPath {
    private final ModulePath path
    private final String name

    ClassPath(ModulePath path, String name) {
        this.path = path
        this.name = name
    }

    static ClassPath of(Type type) {
        String internalName = type.internalName
        String[] split = internalName.split("/")
        ModulePath modulePath = new ModulePath(Arrays.copyOf(split, split.length - 1))
        return modulePath.getClass(split[split.length - 1])
    }

    @Override
    String toString() {
        return path + "." + name
    }

    Type asType() {
        String internalPath = path.toString().replace(".", "/")
        return Type.getObjectType(internalPath + "/" + name)
    }

    ModulePath path() {
        return path
    }

    String name() {
        return name
    }

    @Override
    boolean equals(Object obj) {
        if (obj == this) return true
        if (obj == null || obj.class != this.class) return false
        var that = (ClassPath) obj
        return Objects.equals(this.path, that.path) &&
                Objects.equals(this.name, that.name)
    }

    @Override
    int hashCode() {
        return Objects.hash(path, name)
    }

}
