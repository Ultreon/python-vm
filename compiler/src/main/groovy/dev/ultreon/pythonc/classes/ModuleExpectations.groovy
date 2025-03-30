package dev.ultreon.pythonc.classes

import dev.ultreon.pythonc.ExpectedClass
import dev.ultreon.pythonc.PythonCompiler

class ModuleExpectations {
    private final Module module

    ModuleExpectations(Module module) {
        this.module = module
    }

    ExpectedClass expectClass(String name) {
        return PythonCompiler.expectations.expectClass(PythonCompiler.current, module.path(), name)
    }
}
