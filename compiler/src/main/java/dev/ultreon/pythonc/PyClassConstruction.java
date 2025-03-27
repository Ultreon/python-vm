package dev.ultreon.pythonc;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PyClassConstruction implements PyExpr {
    private final List<PyExpr> arguments;
    private final JvmClass jvmClass;
    private final Location location;

    public PyClassConstruction(List<PyExpr> arguments, JvmClass jvmClass, Location location) {
        this.arguments = arguments;
        this.jvmClass = jvmClass;
        this.location = location;
    }

    @Override
    public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        return null;
    }

    @Override
    public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
        Type[] paramTypes = arguments.stream().map(expr -> expr.type(compiler)).toArray(Type[]::new);
        JvmConstructor constructor = jvmClass.constructor(compiler, paramTypes);
        if (constructor == null) {
            throw new CompilerException("No constructor found for " + jvmClass + " with parameters " + Arrays.stream(paramTypes).map(Type::getClassName).collect(Collectors.joining(", ")), location);
        }
        compiler.writer.newInstance(jvmClass, arguments);
    }

    @Override
    public void expectReturnType(PythonCompiler compiler, JvmClass returnType, Location location) {
        if (!jvmClass.doesInherit(compiler, returnType)) {
            throw new CompilerException("Expected " + returnType + " but got " + jvmClass, location);
        }
    }

    @Override
    public Type type(PythonCompiler compiler) {
        return jvmClass.type(compiler);
    }

    @Override
    public Location location() {
        return location;
    }

    public JvmClass cls(PythonCompiler pythonCompiler) {
        return jvmClass;
    }
}
