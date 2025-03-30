package dev.ultreon.pythonc

import dev.ultreon.pythonc.classes.Module
import dev.ultreon.pythonc.functions.PyFunction
import dev.ultreon.pythonc.functions.FunctionDefiner
import org.jetbrains.annotations.Nullable

class ModuleContext extends SymbolContext implements FunctionDefiningContext {
    private Module module

    ModuleContext() {
        super(null)
    }

    def module() {
        return module
    }

    static pushContext() {
        ModuleContext context = new ModuleContext()
        PythonCompiler.current.pushContext(context)
        return context
    }

    void addFunction(PyFunction pyFunction) {
        module.addFunction(pyFunction)
    }

    @Override
    FunctionDefiner type() {
        return module
    }

    def module(@Nullable Module module) {
        this.module = module
    }
}
