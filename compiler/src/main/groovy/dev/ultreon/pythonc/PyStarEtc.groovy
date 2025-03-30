package dev.ultreon.pythonc

import dev.ultreon.pythonc.functions.param.PyArgsParameter
import dev.ultreon.pythonc.functions.param.PyParameter

final class PyStarEtc {
    private final PyArgsParameter argsParameter
    private final List<PyParameter> parameters

    PyStarEtc(PyArgsParameter argsParameter, List<PyParameter> parameters) {
        this.argsParameter = argsParameter
        this.parameters = parameters
    }

    PyArgsParameter getArgsParameter() {
        return argsParameter
    }

    List<PyParameter> getParameters() {
        return parameters
    }

    @Override
    boolean equals(Object obj) {
        if (obj == this) return true
        if (obj == null || obj.class != this.class) return false
        var that = (PyStarEtc) obj
        return Objects.equals(this.argsParameter, that.argsParameter) &&
                Objects.equals(this.parameters, that.parameters)
    }

    @Override
    int hashCode() {
        return Objects.hash(argsParameter, parameters)
    }

    @Override
    String toString() {
        return "PyStarEtc[" +
                "argsParameter=" + argsParameter + ", " +
                "parameters=" + parameters + ']'
    }

}
