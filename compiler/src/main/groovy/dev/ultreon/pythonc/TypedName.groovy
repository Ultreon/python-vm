package dev.ultreon.pythonc

import dev.ultreon.pythonc.classes.JvmClass
import dev.ultreon.pythonc.statement.PyFromImportStatement
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.objectweb.asm.Type

final class TypedName {
    private final @NotNull String name
    private final @Nullable Type type
    private final @Nullable SymbolReferenceExpr ref

    TypedName(@NotNull String name, @Nullable Type type) {
        this.name = name
        this.type = type
        this.ref = null
    }

    TypedName(@NotNull String name, @Nullable SymbolReferenceExpr type) {
        this.name = name
        this.type = null
        this.ref = type
    }

    JvmClass typeClass(PythonCompiler compiler) {
        if (ref != null) {
            def symbol = ref.symbol()
            if (symbol instanceof JvmClass) return (JvmClass) symbol
            if (symbol instanceof PyFromImportStatement.ImportedSymbol) return (JvmClass) ((PyFromImportStatement.ImportedSymbol) symbol).value
            return (JvmClass) compiler.getClassSymbol(ref.symbol().name)
        }
        return PythonCompiler.classCache.require(compiler, type == null ? Type.getType(Object.class) : type)
    }

    @NotNull String getName() {
        return name
    }

    @Nullable Type getType() {
        return type
    }

    @Override
    boolean equals(Object obj) {
        if (obj == this) return true
        if (obj == null || obj.class != this.class) return false
        var that = (TypedName) obj
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.type, that.type)
    }

    @Override
    int hashCode() {
        return Objects.hash(name, type)
    }

    @Override
    String toString() {
        return "TypedName[" +
                "name=" + name + ", " +
                "type=" + type + ']'
    }

}
