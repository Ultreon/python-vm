package dev.ultreon.pythonc.expr

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.KvPair
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

import static org.objectweb.asm.Type.getType

class ListExpr extends PyExpression {
    private final List<PyExpression> expressions

    ListExpr(List<PyExpression> expressions, Location location) {
        super(location)
        this.expressions = expressions
    }

    @Override
    void writeCode(PythonCompiler compiler, JvmWriter writer) {
        writer.newObject(getType(ArrayList))
        writer.dup()
        writer.invokeSpecial(getType(ArrayList), "<init>", Type.getMethodType(Type.VOID_TYPE), false)
        writer.context.pop(getType(ArrayList))

        for (PyExpression expr : expressions) {
            writer.mv().visitInsn(Opcodes.DUP)
            writer.context.push(getType(ArrayList))
            expr.write(compiler, writer)
            compiler.checkNoPop(location)
            writer.cast(getType(Object))
            writer.invokeVirtual(getType(ArrayList), "add", Type.getMethodType(Type.BOOLEAN_TYPE, getType(Object)), false)
            writer.pop()
        }

        writer.context.push(getType(ArrayList))
        compiler.checkNoPop(location)
        writer.cast(getType(Object))
    }
}
