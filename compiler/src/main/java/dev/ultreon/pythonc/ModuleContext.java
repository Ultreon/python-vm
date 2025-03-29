package dev.ultreon.pythonc;

import dev.ultreon.pythonc.classes.Module;
import dev.ultreon.pythonc.functions.PyFunction;
import dev.ultreon.pythonc.functions.FunctionDefiner;
import org.jetbrains.annotations.Nullable;

public class ModuleContext extends SymbolContext implements FunctionDefiningContext {
    private Module module;

    public ModuleContext() {
        super(null);
    }

    public Module module() {
        return module;
    }

    public static ModuleContext pushContext() {
        ModuleContext context = new ModuleContext();
        PythonCompiler.current().pushContext(context);
        return context;
    }

    @Override
    public void addFunction(PyFunction pyFunction) {
        module.addFunction(pyFunction);
    }

    @Override
    public FunctionDefiner type() {
        return module;
    }

    public void module(@Nullable Module module) {
        this.module = module;
    }
}
