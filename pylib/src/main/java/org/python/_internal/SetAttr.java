package org.python._internal;

import org.jetbrains.annotations.NotNull;
import org.python.builtins.TypeError;

import java.util.Map;

public class SetAttr extends PyFunction {
    private final Class<?> type;
    private final Object instance;
    private final boolean isStatic;

    private SetAttr(Class<?> type, Object instance, boolean isStatic) {
        this.type = type;
        this.instance = instance;
        this.isStatic = isStatic;
    }

    public static SetAttr forStatic(Class<?> type) {
        return new SetAttr(type, null, true);
    }

    public static SetAttr forInstance(@NotNull Object instance) {
        return new SetAttr(instance.getClass(), instance, false);
    }

    @Override
    public Object __call__(Object[] args, Map<String, Object> kwargs) {
        Object arg = args[0];
        if (!(arg instanceof String))
            throw new TypeError("attribute co_name should be a str");

        MetaData meta = MetaDataManager.meta(isStatic ? instance : type);
        meta.set((String) arg, args[1]);
        return null;
    }
}
