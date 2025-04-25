package dev.ultreon.pythonc.statement

import dev.ultreon.pythonc.PythonCompiler
import dev.ultreon.pythonc.SymbolContext
import dev.ultreon.pythonc.SymbolProvider
import dev.ultreon.pythonc.classes.PyClassDefinition

class ClassContext extends SymbolContext {
    PyClassDefinition definition

    ClassContext(PyClassDefinition definition, SymbolProvider symbolContext) {
        super(symbolContext)
        this.definition = definition
    }

    void popContext(PythonCompiler compiler) {
        compiler.popContext(this.class)
    }
}
