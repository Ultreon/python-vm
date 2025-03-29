package dev.ultreon.pythonc.classes;

import dev.ultreon.pythonc.ExpectedClass;
import dev.ultreon.pythonc.PythonCompiler;

public class ModuleExpectations {
    private final Module module;

    public ModuleExpectations(Module module) {
        this.module = module;
    }

    public ExpectedClass expectClass(String name) {
        return PythonCompiler.expectations.expectClass(PythonCompiler.current(), module.path(), name);
    }
}
