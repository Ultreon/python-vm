package dev.ultreon.pythonc;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PyFunctionCall implements PyExpr {
    private final List<PyExpr> arguments;
    private final JvmFunction jvmFunction;
    private final Location location;

    public PyFunctionCall(List<PyExpr> arguments, JvmFunction jvmFunction, Location location) {
        this.arguments = arguments;
        this.jvmFunction = jvmFunction;
        this.location = location;
    }

    @Override
    public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        return null;
    }

    @Override
    public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
        if (jvmFunction.isStatic()) {
            if (jvmFunction instanceof PyBuiltinFunction) {
                compiler.writer.createArray(arguments, Type.getType(Object.class));
                compiler.writer.invokeStatic("java/util/Map", "of", "()Ljava/util/Map;", true);
                compiler.writer.dynamicBuiltinCall(jvmFunction.name(), "([Ljava/lang/Object;Ljava/util/Map;)Ljava/lang/Object;");
                return;
            }
            if (jvmFunction.isDynamicCall()) {
                compiler.writer.createArray(arguments, Type.getType(Object.class));
                compiler.writer.invokeStatic("java/util/Map", "of", "()Ljava/util/Map;", true);

                compiler.writer.invokeStatic(jvmFunction.owner(compiler).type(compiler).getInternalName(), jvmFunction.name(), "([Ljava/lang/Object;Ljava/util/Map;)" + jvmFunction.returnType(compiler).getDescriptor(), boxed);
                return;
            }
            for (PyExpr argument : arguments) {
                argument.load(mv, compiler, argument.preload(mv, compiler, boxed), boxed);
            }
            compiler.writer.invokeStatic(jvmFunction.owner(compiler).type(compiler).getInternalName(), jvmFunction.name(), "(" + Arrays.stream(jvmFunction.parameterTypes(compiler)).map(Type::getDescriptor).collect(Collectors.joining("")) + ")" + jvmFunction.returnType(compiler).getDescriptor(), boxed);
        } else {
            throw new CompilerException("Function is not static", location);
        }
    }

    @Override
    public void expectReturnType(PythonCompiler compiler, JvmClass returnType, Location location) {

    }

    @Override
    public Type type(PythonCompiler compiler) {
        return jvmFunction.returnType(compiler);
    }

    @Override
    public Location location() {
        return location;
    }
}
