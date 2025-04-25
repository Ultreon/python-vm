package dev.ultreon.pythonc.expr

import dev.ultreon.pythonc.JvmWriter
import dev.ultreon.pythonc.Location
import dev.ultreon.pythonc.PythonCompiler
import org.objectweb.asm.Type

class VariableExpr extends PyExpression implements Settable, PySymbol {
    public int index
    public String name
    Type type

    VariableExpr(int index, String name, Location location) {
        super(location)
        this.index = index
        this.name = name
    }

    int index() {
        return index
    }

    String getName() {
        return name
    }

    @Override
    void writeCall(PythonCompiler compiler, JvmWriter writer, List<PyExpression> args, Map<String, PyExpression> kwargs) {
        writeCode(compiler, writer)
        writer.createArgs(args)
        writer.createKwargs(kwargs)
        writer.dynamicCall()
    }

    @Override
    void writeCode(PythonCompiler compiler, JvmWriter writer) {
        writer.loadValue(index, Type.getType(Object.class))
        compiler.checkNoPop(location)
    }

    void set(PythonCompiler compiler, JvmWriter writer, PyExpression expr) {
        if (compiler.getSymbolToSet(name) == null)
            compiler.setSymbol(name, this)
        expr.write(compiler, writer)
        writer.storeObject(index, Type.getType(Object.class))
        this.type = expr.type
        compiler.checkPop(location)
    }

    void writeSet(PythonCompiler compiler, JvmWriter writer) {
        writer.storeObject(index, Type.getType(Object.class))
    }

    String toString() {
        StringBuilder builder = new StringBuilder()

        builder.append(Location.ANSI_RED)
        builder.append("Variable ")
        builder.append(Location.ANSI_RESET)
        builder.append(Location.ANSI_BRIGHT_CYAN)
        builder.append(name)
        builder.append(Location.ANSI_RESET)
    }
}
