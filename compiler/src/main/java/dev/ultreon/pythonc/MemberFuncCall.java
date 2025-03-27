package dev.ultreon.pythonc;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.Arrays;

public class MemberFuncCall implements PyExpr {
    private final PyExpr parent;
    public final String name;
    public final PyExpr[] arguments;
    private final Location location;

    public MemberFuncCall(PyExpr parent, String name, PyExpr[] arguments, Location location) {
        this.parent = parent;
        this.name = name;
        this.arguments = arguments;
        this.location = location;
    }

    @Override
    public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        if (parent instanceof PyObjectRef(String name, Location location1)) {
            Symbol symbol = compiler.symbols.get(name);
            if (symbol == null) {
                throw new CompilerException("Symbol '" + name + "' not found (" + compiler.getLocation(this) + ")");
            }

            if (symbol instanceof PyImport pyImport) {
                if (pyImport.symbol instanceof JvmFunction jvmFunction) {
                    jvmFunction.load(mv, compiler, jvmFunction.preload(mv, compiler, false), boxed);
                } else if (pyImport.symbol instanceof JvmClass jvmClass) {
                    // No need for expression loading
                }
            } else if (symbol instanceof JvmFunction jvmFunction) {
                jvmFunction.load(mv, compiler, jvmFunction.preload(mv, compiler, false), boxed);
            } else if (symbol instanceof JvmClass jvmClass) {
                // No need for expression loading
            }
        } else {
            parent.load(mv, compiler, parent.preload(mv, compiler, false), boxed);
        }

        return null;
    }

    @Override
    public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
        compiler.writer.createArray(Arrays.asList(arguments), Type.getType(Object.class));
        compiler.writer.createKwargs();
        compiler.writer.dynamicCall(name, "([Ljava/lang/Object;Ljava/util/Map;)Ljava/lang/Object;");
    }

    @Override
    public void expectReturnType(PythonCompiler compiler, JvmClass returnType, Location location) {
        // Do nothing
    }

    @Override
    public Type type(PythonCompiler compiler) {
        return Type.getObjectType("java/lang/Object");
    }

    @Override
    public Location location() {
        return location;
    }

    public Type owner(PythonCompiler compiler) {
        return parent.type(compiler);
    }

    public JvmClass ownerClass(PythonCompiler compiler) {
        Type owner = owner(compiler);
        if (!PythonCompiler.classCache.load(compiler, owner)) {
            throw new CompilerException("Class '" + owner.getClassName() + "' not found (" + compiler.getLocation(this) + ")");
        }
        return PythonCompiler.classCache.get(owner);
    }
}
