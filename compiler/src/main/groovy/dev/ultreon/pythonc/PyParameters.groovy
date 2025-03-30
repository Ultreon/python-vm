package dev.ultreon.pythonc

import dev.ultreon.pythonc.expr.SelfExpr
import dev.ultreon.pythonc.functions.param.PyArgsParameter
import dev.ultreon.pythonc.functions.param.PyKwargsParameter
import dev.ultreon.pythonc.functions.param.PyParameter
import org.jetbrains.annotations.Nullable

final class PyParameters {
    private final @Nullable SelfExpr self
    private final List<PyParameter> parameters
    private final @Nullable PyArgsParameter argsParameter
    private final @Nullable PyKwargsParameter kwargsParameter

    PyParameters(@Nullable SelfExpr self, List<PyParameter> parameters,
                 @Nullable PyArgsParameter argsParameter, @Nullable PyKwargsParameter kwargsParameter) {
        this.self = self
        this.parameters = parameters
        this.argsParameter = argsParameter
        this.kwargsParameter = kwargsParameter
    }

    @Nullable SelfExpr self() {
        return self
    }

    List<PyParameter> parameters() {
        return parameters
    }

    @Nullable PyArgsParameter argsParameter() {
        return argsParameter
    }

    @Nullable PyKwargsParameter kwargsParameter() {
        return kwargsParameter
    }

    @Override
    boolean equals(Object obj) {
        if (obj == this) return true
        if (obj == null || obj.class != this.class) return false
        var that = (PyParameters) obj
        return Objects.equals(this.self, that.self) &&
                Objects.equals(this.parameters, that.parameters) &&
                Objects.equals(this.argsParameter, that.argsParameter) &&
                Objects.equals(this.kwargsParameter, that.kwargsParameter)
    }

    @Override
    int hashCode() {
        return Objects.hash(self, parameters, argsParameter, kwargsParameter)
    }

    @Override
    String toString() {
        return "PyParameters[" +
                "self=" + self + ", " +
                "parameters=" + parameters + ", " +
                "argsParameter=" + argsParameter + ", " +
                "kwargsParameter=" + kwargsParameter + ']'
    }

}
