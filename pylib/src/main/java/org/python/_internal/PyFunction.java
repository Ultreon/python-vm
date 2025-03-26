package org.python._internal;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public abstract class PyFunction implements PyObject {
    public static Object fromMethods(List<Method> value) {
        return new PyFunction() {
            @Override
            public Object __call__(Object[] args, Map<String, Object> kwargs) {
                try {
                    methods: for (Method method : value) {
                        if (method.getParameterCount() == args.length) {
                            for (Class<?> aClass : method.getParameterTypes()) {
                                if (!aClass.isInstance(args[0])) continue methods;
                            }
                            return method.invoke(kwargs.get("self"), args);
                        }
                    }
                    return null;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Override
    public abstract Object __call__(Object[] args, Map<String, Object> kwargs);

    public static Object fromMethod(Method method) {
        return new PyFunction() {
            @Override
            public Object __call__(Object[] args, Map<String, Object> kwargs) {
                try {
                    return method.invoke(kwargs.get("self"), args);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
