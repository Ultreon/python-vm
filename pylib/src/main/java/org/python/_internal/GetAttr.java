package org.python._internal;

import org.jetbrains.annotations.NotNull;
import org.python.builtins.TypeError;

import java.util.Map;

public class GetAttr extends PyFunction {
    private final Class<?> type;
    private final Object instance;
    private final boolean isStatic;

    private GetAttr(Class<?> type, Object instance, boolean isStatic) {
        this.type = type;
        this.instance = instance;
        this.isStatic = isStatic;
    }

    public static GetAttr forStatic(Class<?> type) {
        return new GetAttr(type, null, true);
    }

    public static GetAttr forInstance(@NotNull Object instance) {
        return new GetAttr(instance.getClass(), instance, false);
    }

    @Override
    public Object __call__(Object[] args, Map<String, Object> kwargs) {
        Object arg = args[0];
        if (!(arg instanceof String))
            throw new TypeError("attribute co_name should be a str");

        MetaData meta = MetaDataManager.meta(isStatic ? instance : type);
        return meta.get((String) arg);
    }
}
