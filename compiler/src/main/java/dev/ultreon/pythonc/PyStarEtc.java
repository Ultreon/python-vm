package dev.ultreon.pythonc;

import dev.ultreon.pythonc.functions.param.PyArgsParameter;
import dev.ultreon.pythonc.functions.param.PyParameter;

import java.util.List;

public record PyStarEtc(PyArgsParameter argsParameter, List<PyParameter> parameters) {
}
