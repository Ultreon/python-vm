package dev.ultreon.pythonc;

import com.google.common.base.CaseFormat;
import org.objectweb.asm.Type;

import java.util.*;

public class ModulePath {
    private final String[] path;

    public ModulePath(String path) {
        this.path = path.split("\\.");
    }

    ModulePath(String[] path) {
        List<String> list = new ArrayList<>();
        for (String s : path) {
            if (s.isEmpty()) continue;
            list.add(s);
        }
        this.path = list.toArray(new String[0]);
    }

    public ModulePath() {
        this.path = new String[0];
    }

    public String[] getPath() {
        return Arrays.copyOf(path, path.length);
    }

    public String toString() {
        if (path.length == 0) return "<root>";
        return String.join(".", path);
    }

    public String getName() {
        if (path.length == 0) return "<root>";
        return path[path.length - 1];
    }

    public ModulePath getParent() {
        if (path.length == 0) return null;
        return new ModulePath(Arrays.copyOf(path, path.length - 1));
    }

    public boolean isRoot() {
        return path.length == 0;
    }

    public boolean isChildOf(ModulePath parent) {
        return Arrays.equals(parent.path, Arrays.copyOf(path, parent.path.length));
    }

    public boolean isParentOf(ModulePath child) {
        return child.isChildOf(this);
    }

    public int getDepth() {
        return path.length;
    }

    public ModulePath getChild(String name) {
        String[] path = Arrays.copyOf(this.path, this.path.length + 1);
        path[path.length - 1] = name;
        return new ModulePath(String.join(".", path));
    }

    public Type asType() {
        if (path.length == 0) return Type.getObjectType("MainPy");
        StringJoiner joiner = new StringJoiner("/");
        boolean added = false;
        for (int i = 0; i < path.length; i++) {
            String element = path[i];
            if (element.isEmpty()) continue;
            added = true;
            if (i == path.length - 1) joiner.add(CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, element) + "Py");
            else joiner.add(element);
        }

        if (!added) return Type.getObjectType("MainPy");

        return Type.getObjectType(joiner.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ModulePath that = (ModulePath) o;
        return Objects.deepEquals(getPath(), that.getPath());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getPath());
    }

    public ClassPath getClass(String name) {
        return new ClassPath(this, name);
    }
}
