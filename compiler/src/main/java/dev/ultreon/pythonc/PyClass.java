package dev.ultreon.pythonc;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class PyClass implements JvmClass {
    public static final String CANNOT_SET = "Cannot set the class '%s' (%s)";

    public final String name;
    public final Type owner;
    public final Map<String, PyField> fields;
    public final Map<String, PyFunction> methods;
    private final int lineNo;
    private final List<? extends JvmClass> superClasses = new ArrayList<>();

    public PyClass(String name, Type owner, int lineNo) {
        this.name = name;
        this.owner = owner;
        this.lineNo = lineNo;
        this.fields = new LinkedHashMap<>();
        this.methods = new LinkedHashMap<>();
    }

    public PyClass(Type owner, int lineNo) {
        this.name = owner.getClassName();
        this.owner = owner;
        this.lineNo = lineNo;
        this.fields = new LinkedHashMap<>();
        this.methods = new LinkedHashMap<>();
    }

    public String owner(PythonCompiler compiler) {
        return PythonCompiler.FMT_CLASS.formatted(owner);
    }

    @Override
    public Object preload(MethodVisitor mv, PythonCompiler compiler, boolean boxed) {
        return null;
    }

    @Override
    public void load(MethodVisitor mv, PythonCompiler compiler, Object preloaded, boolean boxed) {
        throw new UnsupportedOperationException(PythonCompiler.E_NOT_ALLOWED);
    }

    @Override
    public int lineNo() {
        return lineNo;
    }

    @Override
    public String name() {
        String className = owner.getClassName();
        return className.substring(className.lastIndexOf('.') + 1);
    }

    @Override
    public String className() {
        return name;
    }

    @Override
    public Type type(PythonCompiler compiler) {
        return owner;
    }

    @Override
    public void set(MethodVisitor mv, PythonCompiler compiler, PyExpr visit) {
        throw new CompilerException(CANNOT_SET.formatted(name, compiler.getLocation(visit)));
    }

    @Override
    public boolean isInterface() {
        // TODO add interface support
        return false;
    }

    @Override
    public boolean isAbstract() {
        // TODO add abstract class support
        return false;
    }

    @Override
    public boolean isEnum() {
        // TODO add enum support
        return false;
    }

    public boolean doesInherit(Class<?> type) {
        if (type == null) {
            return false;
        }

        if (type == Object.class) {
            return true;
        }

        for (JvmClass jvmClass : superClasses) {
            if (jvmClass.doesInherit(type)) {
                return true;
            }

            if (jvmClass instanceof JClass jClass) {
                if (type.isAssignableFrom(jClass.getType())) {
                    return true;
                }
            }
        }

        return false;
    }
}
