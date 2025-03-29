package dev.ultreon.pythonc;

import dev.ultreon.pythonc.expr.SelfExpr;
import dev.ultreon.pythonc.functions.param.PyArgsParameter;
import dev.ultreon.pythonc.functions.param.PyKwargsParameter;
import dev.ultreon.pythonc.functions.param.PyParameter;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record PyParameters(@Nullable SelfExpr self, List<PyParameter> parameters,
                           @Nullable PyArgsParameter argsParameter, @Nullable PyKwargsParameter kwargsParameter) {
}
