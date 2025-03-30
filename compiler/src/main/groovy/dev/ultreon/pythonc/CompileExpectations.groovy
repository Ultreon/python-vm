package dev.ultreon.pythonc

import dev.ultreon.pythonc.classes.Module

class CompileExpectations {
    private final Map<ClassPath, ExpectedClass> expectedClasses = new HashMap<>()
    private final Map<ModulePath, ExpectedModule> expectedModules = new HashMap<>()

    ExpectedClass expectClass(PythonCompiler pythonCompiler, ModulePath module, String name) {
        ExpectedClass expectedClass = new ExpectedClass(module, name)
        expectedClasses.put(new ClassPath(module, name), expectedClass)
        return expectedClass
    }

    ExpectedModule expectModule(PythonCompiler compiler, ModulePath modulePath, Location location) {
        ModulePath parent = modulePath.parent
        ExpectedModule newExpectation = new ExpectedModule(parent == null ? null : PythonCompiler.moduleCache.get(parent, location), modulePath, location)
        ExpectedModule originalExpectation = expectedModules.get(modulePath)
        if (originalExpectation != null) {
            return originalExpectation
        }
        this.expectedModules.put(modulePath, newExpectation)
        return newExpectation
    }

    Module defineModule(ModulePath path, Location location) {
        Module module = Module.create(path, location)
        expectedModules.remove(path)

        return module
    }

    Iterable<CompilerException> getExceptions() {
        List<CompilerException> exceptions = new ArrayList<>()

        for (Map.Entry<ClassPath, ExpectedClass> entry : expectedClasses.entrySet()) {
            ClassPath classPath = entry.key
            ExpectedClass expectedClass = entry.value

            exceptions.add(new CompilerException("Class " + classPath + " not defined", expectedClass.location))
        }

        for (Map.Entry<ModulePath, ExpectedModule> entry : expectedModules.entrySet()) {
            ModulePath modulePath = entry.key
            if (modulePath.isRoot()) continue
            ExpectedModule expectedModule = entry.value

            exceptions.add(new CompilerException("Module " + modulePath + " not defined", expectedModule.location))
        }

        return exceptions
    }
}
