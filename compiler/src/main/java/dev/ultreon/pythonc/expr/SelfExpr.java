package dev.ultreon.pythonc.expr;

import dev.ultreon.pythonc.JvmWriter;
import dev.ultreon.pythonc.Location;
import dev.ultreon.pythonc.PythonCompiler;
import dev.ultreon.pythonc.classes.JvmClass;
import dev.ultreon.pythonc.classes.LangClass;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;

public class SelfExpr extends PyExpression {
    private final Type type;

    public SelfExpr(Type type, Location location) {
        super(location);
        this.type = type;
    }

    public static PyExpression of(@NotNull JvmClass definingClass, Location location) {
        return new SelfExpr(definingClass.type(), location);
    }

    public Type type() {
        return type;
    }

    @Override
    public void writeCode(PythonCompiler compiler, JvmWriter writer) {
        writer.loadThis((LangClass) PythonCompiler.classCache.require(compiler, type));
        compiler.checkNoPop(location());
    }

    public LangClass typeClass() {
        return (LangClass) PythonCompiler.classCache.require(type);
    }

    public MemberAttrExpr attr(String name, Location location) {
        return new MemberAttrExpr(this, name, location);
    }
}
