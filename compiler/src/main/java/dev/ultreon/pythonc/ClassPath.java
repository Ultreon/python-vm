package dev.ultreon.pythonc;

import com.google.common.base.CaseFormat;
import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.StringJoiner;

public record ClassPath(ModulePath path, String name) {
    public static ClassPath of(Type type) {
        String internalName = type.getInternalName();
        String[] split = internalName.split("/");
        ModulePath modulePath = new ModulePath(Arrays.copyOf(split, split.length - 1));
        return modulePath.getClass(split[split.length - 1]);
    }

    @Override
    public String toString() {
        return path + "." + name;
    }

    public Type asType() {
        String internalPath = path.toString().replace(".", "/");
        return Type.getObjectType(internalPath + "/" + name);
    }
}
