package dev.ultreon.pythonc.expr

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.KvPair
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler
import org.objectweb.asm.Type

class ListExpr extends PyExpression {
    private final List<PyExpression> expressions

    ListExpr(List<PyExpression> expressions, Location location) {
        super(location)
        this.expressions = expressions
    }

    @Override
    void writeCode(PythonCompiler compiler, JvmWriter writer) {
        writer.newObject(Type.getType(ArrayList))
        writer.dup()
        writer.invokeSpecial(Type.getType(ArrayList), "<init>", Type.getMethodType(Type.VOID_TYPE), false)
        compiler.checkNoPop(location)

        for (PyExpression kvPair : expressions) {
            kvPair.write(compiler, writer)
            compiler.checkNoPop(location)
            writer.cast(Type.getType(Object))
            writer.invokeVirtual(Type.getType(ArrayList), "add", Type.getMethodType(Type.VOID_TYPE, Type.getType(Object)), false)
        }

        compiler.checkNoPop(location)
        writer.cast(Type.getType(Object))
    }
}
