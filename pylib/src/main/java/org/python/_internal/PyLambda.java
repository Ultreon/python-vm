package org.python._internal;

import java.util.Map;

@FunctionalInterface
public interface PyLambda {
    Object __call__(Object[] args, Map<String, Object> kwargs);
}
