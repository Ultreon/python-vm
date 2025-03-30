package dev.ultreon.pythonc

import java.lang.reflect.Method

class ClassUtils {
    static boolean isPrimitive(Class<?> clazz) {
        return clazz.primitive
    }

    static Method[] getMethodsByName(Class<?> type, String name) {
        try {
            def methods = new ArrayList<>()
            for (Method method : (type.declaredMethods)) {
                if (method.name == name) {
                    methods.add(method)
                }
            }
            for (Method method : (type.methods)) {
                if (method.name == name) {
                    if (methods.contains(method)) continue
                    methods.add(method)
                }
            }

            return methods.toArray(Method[]::new)
        } catch (SecurityException e) {
            throw new RuntimeException(e)
        }
    }
}
