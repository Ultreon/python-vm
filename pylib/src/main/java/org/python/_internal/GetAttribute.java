package org.python._internal;

import org.jetbrains.annotations.NotNull;
import org.python.builtins.AttributeError;
import org.python.builtins.TypeError;

import java.util.Map;

public class GetAttribute extends PyFunction {
    private final Class<?> type;
    private final Object instance;
    private final boolean isStatic;

    private GetAttribute(Class<?> type, Object instance, boolean isStatic) {
        this.type = type;
        this.instance = instance;
        this.isStatic = isStatic;
    }

    public static GetAttribute forStatic(Class<?> type) {
        return new GetAttribute(type, null, true);
    }

    public static GetAttribute forInstance(@NotNull Object instance) {
        return new GetAttribute(instance.getClass(), instance, false);
    }

    @Override
    public Object __call__(Object[] args, Map<String, Object> kwargs) {
        Object arg = args[0];
        if (!(arg instanceof String))
            throw new TypeError("attribute co_name should be a str");

        MetaData meta = MetaDataManager.meta(isStatic ? instance : type);
        if (!meta.has((String) arg)) {
            PyObject attr = (PyObject) meta.get("__getattr__");
            if (attr == null) throw new AttributeError((String) arg, instance == null ? type : instance);
            return attr.__call__(args, kwargs);
        }
        return meta.get((String) arg);
    }
}
