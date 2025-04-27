package org.python.builtins;

import org.jetbrains.annotations.NotNull;
import org.python._internal.MetaData;
import org.python._internal.MetaDataManager;
import org.python._internal.PyModule;

public class AttributeError extends PyException {
    public AttributeError(String name, Object obj) {
        this(getMessage(name, obj));
    }

    private static @NotNull String getMessage(String name, Object obj) {
        if (obj == null) return "'NoneType' object" + " has no attribute '" + name + "'";
        if (obj instanceof Class<?>) {
            if (PyModule.class.isAssignableFrom((Class<?>) obj)) {
                String simpleName = ((Class<?>) obj).getSimpleName();
                MetaData meta = MetaDataManager.meta(obj);
                if (meta.has("__module__")) {
                    simpleName = (String) meta.get("__module__");
                    return "'" + simpleName + "' module has no attribute '" + name + "'";
                }
                return "'" + simpleName + "' module has no attribute '" + name + "'";
            }
            return "'" + ((Class<?>) obj).getSimpleName() + "' class" + " has no attribute '" + name + "'";
        }
        return "'" + obj.getClass().getSimpleName() + "' object" + " has no attribute '" + name + "'";
    }

    public AttributeError(String message) {
        super(message);
    }
}
