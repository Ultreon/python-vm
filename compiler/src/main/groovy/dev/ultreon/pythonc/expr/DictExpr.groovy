package dev.ultreon.pythonc.expr

import dev.ultreon.pythonc.CompilerException
import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.KvPair
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler
import org.objectweb.asm.Type

class DictExpr extends PyExpression {
    private final List<KvPair> kvPairs

    DictExpr(List<KvPair> kvPairs, Location location) {
        super(location)
        this.kvPairs = kvPairs
    }

    @Override
    void writeCode(PythonCompiler compiler, JvmWriter writer) {
        def stackSize = writer.context.stackSize
        writer.newObject Type.getType(HashMap)
        writer.dup()
        writer.invokeSpecial(Type.getType(HashMap), "<init>", Type.getMethodType(Type.VOID_TYPE), false)
        compiler.checkNoPop(location)

        if (stackSize + 1 != compiler.context.stackSize) {
            throw new CompilerException("Invalid stack size (" + (stackSize + 1) + ") != (" + compiler.context.stackSize + ") after dict key " + i, kvPair.key.location)
        }
        def i = 0
        for (KvPair kvPair in kvPairs) {
            if (i++ < kvPairs.size()) {
                writer.dup()
            }
            if (stackSize + 2 != compiler.context.stackSize) {
                throw new CompilerException("Invalid stack size (" + (stackSize + 2) + ") != (" + compiler.context.stackSize + ") after map dupe " + i, kvPair.key.location)
            }
            kvPair.key.writeCode compiler, writer

            if (stackSize + 3 != compiler.context.stackSize) {
                throw new CompilerException("Invalid stack size (" + (stackSize + 3) + ") != (" + compiler.context.stackSize + ") after dict key " + i, kvPair.key.location)
            }
            compiler.checkNoPop location
            writer.cast Type.getType(Object)
            kvPair.value.writeCode compiler, writer

            if (stackSize + 4 != compiler.context.stackSize) {
                throw new CompilerException("Invalid stack size (" + (stackSize + 4) + ") != (" + compiler.context.stackSize + ") after dict value " + i, kvPair.value.location)
            }
            compiler.checkNoPop location
            writer.cast Type.getType(Object)
            writer.invokeVirtual Type.getType(HashMap), "put", Type.getMethodType(Type.getType(Object), Type.getType(Object), Type.getType(Object)), false

            if (stackSize + 2 != compiler.context.stackSize) {
                throw new CompilerException("Invalid stack size (" + (stackSize + 2) + ") != (" + compiler.context.stackSize + ") after dict pair " + i, kvPair.location)
            }

            writer.pop()

            if (stackSize + 1 != compiler.context.stackSize) {
                throw new CompilerException("Invalid stack size (" + (stackSize + 1) + ") != (" + compiler.context.stackSize + ") after dict pair pop " + i, kvPair.location)
            }
        }

        writer.checkCast Type.getType(HashMap)

        compiler.checkNoPop(location)
        writer.cast(Type.getType(Object))

        if (stackSize + 1 != compiler.context.stackSize) {
            throw new CompilerException("Invalid stack size (" + compiler.context.stackSize + ") after dict expression", location)
        }
    }
}
