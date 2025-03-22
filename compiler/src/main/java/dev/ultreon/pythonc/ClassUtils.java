package dev.ultreon.pythonc;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ClassUtils {
    public static boolean isPrimitive(Class<?> clazz) {
        return clazz.isPrimitive();
    }

    public static Method[] getMethodsByName(Class<?> type, String name) {
        try {
            List<Method> methods = new ArrayList<>();
            for (Method method : type.getDeclaredMethods()) {
                if (method.getName().equals(name)) {
                    methods.add(method);
                }
            }
            for (Method method : type.getMethods()) {
                if (method.getName().equals(name)) {
                    if (methods.contains(method)) continue;
                    methods.add(method);
                }
            }

            return methods.toArray(Method[]::new);
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        }
    }
}
