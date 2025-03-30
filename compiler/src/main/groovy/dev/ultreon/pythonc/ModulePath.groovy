package dev.ultreon.pythonc

import com.google.common.base.CaseFormat
import org.objectweb.asm.Type

class ModulePath {
    private final String[] path

    ModulePath(String path) {
        this.path = path.split("\\.")
    }

    ModulePath(String[] path) {
        List<String> list = new ArrayList<>()
        for (String s : path) {
            if (s.empty) continue
            list.add(s)
        }
        this.path = list.toArray(new String[0])
    }

    ModulePath() {
        this.path = new String[0]
    }

    def getPath() {
        return Arrays.copyOf(path, path.length)
    }

    String toString() {
        if (path.length == 0) return "<root>"
        return String.join(".", path)
    }

    def getName() {
        if (path.length == 0) return "<root>"
        return path[path.length - 1]
    }

    def getParent() {
        if (path.length == 0) return null
        return new ModulePath(Arrays.copyOf(path, path.length - 1))
    }

    def isRoot() {
        return path.length == 0
    }

    def isChildOf(ModulePath parent) {
        return Arrays.equals(parent.path, Arrays.copyOf(path, parent.path.length))
    }

    def isParentOf(ModulePath child) {
        return child.isChildOf(this)
    }

    def getDepth() {
        return path.length
    }

    def getChild(String name) {
        String[] path = Arrays.copyOf(this.path, this.path.length + 1)
        path[path.length - 1] = name
        return new ModulePath(String.join(".", path))
    }

    def asType() {
        if (path.length == 0) return Type.getObjectType("MainPy")
        StringJoiner joiner = new StringJoiner("/")
        boolean added = false
        for (int i = 0; i < path.length; i++) {
            String element = path[i]
            if (element.empty) continue
            added = true
            if (i == path.length - 1) joiner.add(CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, element) + "Py")
            else joiner.add(element)
        }

        if (!added) return Type.getObjectType("MainPy")

        return Type.getObjectType(joiner.toString())
    }

    @Override
    boolean equals(Object o) {
        if (o == null || getClass() != o.class) return false
        ModulePath that = (ModulePath) o
        return Objects.deepEquals(path, that.path)
    }

    @Override
    int hashCode() {
        return Arrays.hashCode(path)
    }

    def getClass(String name) {
        return new ClassPath(this, name)
    }
}
