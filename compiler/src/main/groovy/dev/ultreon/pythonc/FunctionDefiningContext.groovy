package dev.ultreon.pythonc

import dev.ultreon.pythonc.functions.PyFunction
import dev.ultreon.pythonc.functions.FunctionDefiner

interface FunctionDefiningContext {
    void addFunction(PyFunction pyFunction)

    FunctionDefiner type()
}
