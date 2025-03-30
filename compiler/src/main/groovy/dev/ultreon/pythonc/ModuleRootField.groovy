package dev.ultreon.pythonc

import dev.ultreon.pythonc.classes.Module
import dev.ultreon.pythonc.expr.PyExpression
import dev.ultreon.pythonc.expr.PySymbol
import org.objectweb.asm.Type

class ModuleRootField extends PyExpression implements PySymbol, GlobalSettable {
    private final Module module
    private final String name

    ModuleRootField(Module module, String name, Location location) {
        super(location)
        this.module = module
        this.name = name
    }

    @Override
    String getName() {
        return name
    }

    @Override
    void writeCall(PythonCompiler compiler, JvmWriter writer, List<PyExpression> args, Map<String, PyExpression> kwargs) {
        write(compiler, writer)
        writer.createArgs(args)
        writer.createKwargs(kwargs)
        writer.dynamicCall()
    }

    @Override
    Type getType() {
        return module.type
    }

    @Override
    void writeCode(PythonCompiler compiler, JvmWriter writer) {
        module.writeReference(compiler, writer)
        writer.dynamicGetAttr(name)
        compiler.checkNoPop(location)
    }

    @Override
    void set(PythonCompiler compiler, JvmWriter writer, PyExpression expr) {
        module.writeReference(compiler, writer)
        expr.write(compiler, writer)
        writer.dynamicSetAttr(name)
    }
}
