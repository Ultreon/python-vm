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
        compiler.writer.newInstance(jvmClass.type(compiler).getInternalName(), "<init>", "(" + Arrays.stream(constructor.parameterTypes(compiler)).map(type -> type == null ? "Ljava/lang/Object;" : type.getDescriptor()).collect(Collectors.joining("")) + ")V", false, () -> {
            Type[] types = constructor.parameterTypes(compiler);
            for (int i = 0, argumentsSize = arguments.size(); i < argumentsSize; i++) {
                PyExpr argument = arguments.get(i);
                argument.load(mv, compiler, argument.preload(mv, compiler, boxed), boxed);
                if (types[i] == null || types[i].getSort() == Type.OBJECT) {
                    if (argument.type(compiler).getSort() != Type.ARRAY && argument.type(compiler).getSort() != Type.OBJECT) {
                        compiler.writer.box(argument.type(compiler));
                    }
                }
            }
        });
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
