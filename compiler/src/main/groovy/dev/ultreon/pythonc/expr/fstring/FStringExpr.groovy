package dev.ultreon.pythonc.expr.fstring

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler
import dev.ultreon.pythonc.classes.JvmClass
import dev.ultreon.pythonc.expr.PyExpression

class FStringExpr extends PyExpression {
    private final List<FStringElement> fStringElements

    FStringExpr(List<FStringElement> fStringElements, Location location) {
        super(location)
        this.fStringElements = fStringElements
    }

    @Override
    void writeCode(PythonCompiler compiler, JvmWriter writer) {
        writer.dynamicCall JvmClass.of(StringBuilder, location), []
        def startSize = writer.context.stackSize
        for (def i = 0; i < fStringElements.size(); i++) {
            writer.dup()
            writer.dynamicGetAttr("append")
            writer.createArgs([fStringElements[i as Number]])
            writer.createKwargs([:])
            writer.dynamicCall()
            writer.pop()
            if (writer.context.stackSize != startSize)
                throw new RuntimeException("Failed to properly align with start stack size, started with $startSize currently: $writer.context.stackSize")
        }
        writer.createArgs([])
        writer.createKwargs([:])
        writer.dynamicCall("toString")
        compiler.checkNoPop(location)
    }

    @Override
    String toString() {
        StringBuilder builder = new StringBuilder()

        builder.append(Location.ANSI_RED)
        builder.append("FString ")
        builder.append(Location.ANSI_RESET)
        builder.append(fStringElements.toString())

        builder.append(Location.ANSI_RESET)

        return builder.toString()
    }
}
