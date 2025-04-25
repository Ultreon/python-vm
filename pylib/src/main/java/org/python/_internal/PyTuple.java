package org.python._internal;

import org.python.builtins.TypeError;

import java.util.Arrays;
import java.util.Map;

public class PyTuple implements PyObject {
    private final Object[] items;

    public PyTuple(Object[] args, Map<String, Object> kwargs) {
        if (!kwargs.isEmpty()) throw new NotImplementedError();
        this.items = args;
    }

    @Override
    public Object __getitem__(Object key) throws TypeError {
        if (key instanceof Integer || key instanceof Long) return items[((Number) key).intValue()];
        throw new TypeError("tuple indices must be integers");
    }

    @Override
    public String toString() {
        return "(" + String.join(", ", Arrays.stream(items).map(ClassUtils::getStr).toArray(String[]::new)) + ")";
    }

    @Override
    public String __str__() {
        return toString();
    }
}
