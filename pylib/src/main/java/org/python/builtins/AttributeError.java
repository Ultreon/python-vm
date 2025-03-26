package org.python.builtins;

public class AttributeError extends Exception {
    public AttributeError(String name, Object obj) {
        this((obj == null ? "'NoneType' object" : obj instanceof Class<?> ? "'" + ((Class<?>) obj).getSimpleName() + "' class" : "'" + obj.getClass().getSimpleName() + "' object") + " has no attribute '" + name + "'");
    }

    public AttributeError(String message) {
        super(message);
    }
}
