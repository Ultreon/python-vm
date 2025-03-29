package dev.ultreon.pythonc;

import dev.ultreon.pythonc.functions.PyFunction;
import dev.ultreon.pythonc.functions.FunctionDefiner;

public interface FunctionDefiningContext {
    void addFunction(PyFunction pyFunction);

    FunctionDefiner type();
}
