package dev.ultreon.pythonc

import org.jetbrains.annotations.Nullable
import org.objectweb.asm.Type

final class PyAlias {
    private final String name
    private final @Nullable String alias
    private final Location location

    PyAlias(String name, @Nullable String alias, Location location) {
        this.name = name
        this.alias = alias
        this.location = location
    }

    PyAlias(String name, Location location) {
        this(name, null, location)
    }

    Type asType(ModulePath path) {
        return path.getClass(name).asType()
    }

    String getName() {
        return name
    }

    @Nullable String getAlias() {
        return alias
    }

    Location getLocation() {
        return location
    }

    @Override
    boolean equals(Object obj) {
        if (obj == this) return true
        if (obj == null || obj.class != this.class) return false
        var that = (PyAlias) obj
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.alias, that.alias)
    }

    @Override
    int hashCode() {
        return Objects.hash(name, alias)
    }

    @Override
    String toString() {
        StringBuilder builder = new StringBuilder()

        if (alias != null) {
            builder.append(Location.ANSI_RED).append("<:").append(Location.ANSI_PURPLE).append(name).append(Location.ANSI_RESET).append(":").append(Location.ANSI_PURPLE).append(alias).append(Location.ANSI_RESET).append(":>")
        } else {
            builder.append(Location.ANSI_PURPLE).append(name).append(Location.ANSI_RESET)
        }
    }

}
