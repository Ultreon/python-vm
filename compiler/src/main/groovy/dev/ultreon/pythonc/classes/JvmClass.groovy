package dev.ultreon.pythonc.classes

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler
import dev.ultreon.pythonc.expr.MemberAttrExpr
import dev.ultreon.pythonc.expr.MemberCallExpr
import dev.ultreon.pythonc.expr.PyExpression
import dev.ultreon.pythonc.expr.PySymbol
import org.objectweb.asm.Type

abstract class JvmClass extends PyExpression implements PySymbol {
    private final Type type
    private final String name

    JvmClass(Type type, String name, Location location) {
        super(location)
        this.type = type
        this.name = name
    }

    static JvmClass of(Class<?> aClass, Location location) {
        return PythonCompiler.classCache.require(aClass, location)
    }

    Type getType() {
        return type
    }

    @Override
    @Deprecated
    final void writeCode(PythonCompiler compiler, JvmWriter writer) {
        writeReference(compiler, writer)
    }


    void writeReference(PythonCompiler compiler, JvmWriter writer) {
        writer.loadClass(type)
        compiler.checkNoPop(location, Type.getType(Class.class))
    }

    MemberCallExpr call(String name, Location location, PyExpression... arguments) {
        return new MemberCallExpr(new MemberAttrExpr(this, name, location), List.of(arguments), location)
    }

    MemberCallExpr call(String name, List<PyExpression> arguments, Location location) {
        return new MemberCallExpr(new MemberAttrExpr(this, name, location), arguments, location)
    }

    MemberCallExpr call(Location location, PyExpression... arguments) {
        return new MemberCallExpr(this, List.of(arguments), location)
    }

    MemberCallExpr call(List<PyExpression> arguments, Location location) {
        return new MemberCallExpr(this, arguments, location)
    }

    MemberAttrExpr attr(String name, Location location) {
        return new MemberAttrExpr(this, name, location)
    }

    abstract boolean isInterface();

    abstract boolean isAbstract();

    abstract boolean isEnum();

    abstract boolean isAnnotation();

    abstract boolean isRecord();

    abstract boolean isSealed();

    abstract boolean isModule();

    abstract boolean doesInherit(PythonCompiler compiler, JvmClass type);

    String getName() {
        return name
    }

    String className() {
        return type.className
    }


    @Override
    void writeCall(PythonCompiler compiler, JvmWriter writer, List<PyExpression> args, Map<String, PyExpression> kwargs) {
        writeReference(compiler, writer)
        writer.cast(Type.getType(Object.class))
        writer.createArgs(args)
        writer.createKwargs(kwargs)
        writer.dynamicCall()
    }
}
