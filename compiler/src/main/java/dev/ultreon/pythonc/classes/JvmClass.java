package dev.ultreon.pythonc.classes;

import dev.ultreon.pythonc.JvmWriter;
import dev.ultreon.pythonc.Location;
import dev.ultreon.pythonc.PythonCompiler;
import dev.ultreon.pythonc.expr.MemberAttrExpr;
import dev.ultreon.pythonc.expr.MemberCallExpr;
import dev.ultreon.pythonc.expr.PyExpression;
import dev.ultreon.pythonc.expr.PySymbol;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.Map;

public abstract class JvmClass extends PyExpression implements PySymbol {
    private final Type type;
    private final String name;

    public JvmClass(Type type, String name, Location location) {
        super(location);
        this.type = type;
        this.name = name;
    }

    public Type type() {
        return type;
    }

    @Override
    @Deprecated
    public final void writeCode(PythonCompiler compiler, JvmWriter writer) {
        writeReference(compiler, writer);
    }


    public void writeReference(PythonCompiler compiler, JvmWriter writer) {
        writer.loadClass(type());
        compiler.checkNoPop(location(), Type.getType(Class.class));
    }

    public MemberCallExpr call(String name, Location location, PyExpression... arguments) {
        return new MemberCallExpr(new MemberAttrExpr(this, name, location), List.of(arguments), location);
    }

    public MemberCallExpr call(String name, List<PyExpression> arguments, Location location) {
        return new MemberCallExpr(new MemberAttrExpr(this, name, location), arguments, location);
    }

    public MemberCallExpr call(Location location, PyExpression... arguments) {
        return new MemberCallExpr(this, List.of(arguments), location);
    }

    public MemberCallExpr call(List<PyExpression> arguments, Location location) {
        return new MemberCallExpr(this, arguments, location);
    }

    public MemberAttrExpr attr(String name, Location location) {
        return new MemberAttrExpr(this, name, location);
    }

    public abstract boolean isInterface();

    public abstract boolean isAbstract();

    public abstract boolean isEnum();

    public abstract boolean isAnnotation();

    public abstract boolean isRecord();

    public abstract boolean isSealed();

    public abstract boolean isModule();

    public abstract boolean doesInherit(PythonCompiler compiler, JvmClass type);

    public String name() {
        return name;
    }

    public String className() {
        return type.getClassName();
    }

    @Deprecated
    public String simpleName() {
        String substring = type().getClassName().substring(type().getClassName().lastIndexOf('.') + 1);
        if (substring.contains("$")) substring = substring.substring(substring.lastIndexOf('$') + 1);
        return substring;
    }

    @Override
    public void writeCall(PythonCompiler compiler, JvmWriter writer, List<PyExpression> args, Map<String, PyExpression> kwargs) {
        writeReference(compiler, writer);
        writer.cast(Type.getType(Object.class));
        writer.createArgs(args);
        writer.createKwargs(kwargs);
        writer.dynamicCall();
    }
}
