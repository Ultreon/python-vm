package dev.ultreon.pythonc

import dev.ultreon.pythonc.expr.PyExpression
import org.objectweb.asm.Type

class SliceExpr extends PyExpression {
    private final PyExpression left
    private final PyExpression right

    SliceExpr(PyExpression left, PyExpression right, Location location) {
        super(location)

        this.left = left
        this.right = right
    }

    @Override
    void writeCode(PythonCompiler compiler, JvmWriter writer) {
        writer.newObject(Type.getObjectType("org/ultreon/pythonc/Slice"))
        writer.dup()
        left.write(compiler, writer)
        writer.cast(Type.INT_TYPE)
        right.write(compiler, writer)
        writer.cast(Type.INT_TYPE)
        writer.invokeSpecial(Type.getObjectType("org/ultreon/pythonc/Slice"), "<init>", Type.getMethodType(Type.VOID_TYPE, Type.INT_TYPE, Type.INT_TYPE), false)
        writer.cast(Type.getType(Object))
    }
}
