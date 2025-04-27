package dev.ultreon.pythonc.expr

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler
import dev.ultreon.pythonc.classes.JvmClass
import dev.ultreon.pythonc.classes.PyClass
import org.jetbrains.annotations.NotNull
import org.objectweb.asm.Type

class SelfExpr extends PyExpression implements PySymbol {
    private final Type type

    SelfExpr(Type type, Location location) {
        super(location)
        this.type = type
    }

    static PyExpression of(@NotNull JvmClass definingClass, Location location) {
        return new SelfExpr(definingClass.type, location)
    }

    Type getType() {
        return type
    }

    @Override
    void writeCode(PythonCompiler compiler, JvmWriter writer) {
        writer.loadThis((PyClass) PythonCompiler.classCache.require(compiler, type))
        compiler.checkNoPop(location)
    }

    PyClass typeClass() {
        return (PyClass) PythonCompiler.classCache.require(type)
    }

    MemberAttrExpr attr(String name, Location location) {
        return new MemberAttrExpr(this, name, location)
    }

    String toString() {
        StringBuilder builder = new StringBuilder()

        builder.append(Location.ANSI_RED)
        builder.append("Self")
        builder.append(Location.ANSI_RESET)
    }

    @Override
    String getName() {
        return "self"
    }

    @Override
    void writeCall(PythonCompiler compiler, JvmWriter writer, List<PyExpression> args, Map<String, PyExpression> kwargs) {
        writer.loadThis((PyClass) PythonCompiler.classCache.require(compiler, type))
        writer.createArgs(args)
        writer.createKwargs(kwargs)
        writer.dynamicCall()
        compiler.checkNoPop(location)
    }

    @Override
    void delete(PythonCompiler pythonCompiler, JvmWriter jvmWriter) {
        jvmWriter.newInstance(PythonCompiler.classCache.require(pythonCompiler, Type.getObjectType("java/lang/UnsupportedOperationException")), List.of(
                new ConstantExpr("Stop! Do not try to delete yourself! It's not healthy!", location),
        ))
        jvmWriter.throwObject()
    }
}
