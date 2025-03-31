package org.python._internal;

import org.python.builtins.AttributeError;
import org.python.builtins.TypeError;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class PyFunction implements PyObject {
    public static Object fromMethods(Class<?> ownerClass, Object owner, List<Method> value) {
        Set<String> names = value.stream().map(method -> method.getName().isEmpty() ? "<unnamed>" : method.getName()).collect(Collectors.toSet());
        if (names.isEmpty()) throw new PythonVMBug("No matching functions for: " + value);
        String naming = "'" + String.join("', '", names) + "'";
        String text = (names.size() > 1 ? "<functions " : "<function ") + naming + ">";

        return new PyFunction() {
            @Override
            public Object __call__(Object[] args, Map<String, Object> kwargs) {
                try {
                    methods: for (Method method : value) {
                        if (method.getParameterCount() == args.length) {
                            for (Class<?> aClass : method.getParameterTypes()) {
                                if (!aClass.isInstance(args[0])) continue methods;
                            }
                            Object invoke = method.invoke(owner == ownerClass ? null : owner, args);
                            return invoke;
                        }
                    }
                    String collect = Arrays.stream(args).map(o -> o == null ? "None" : o.getClass().getName()).collect(Collectors.joining(", "));
                    String collect2 = kwargs.entrySet().stream().map(o -> o.getValue() == null ? o.getKey() + " = None" : o.getKey() + " = " + o.getClass().getName()).collect(Collectors.joining(", "));
                    throw new TypeError("No matching function for " + __str__() + " in " + ownerClass.getName() + " " + collect + " : " + collect2);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public String __str__() {
                return text;
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

            @Override
            public String __str__() {
                return "<function '" + method.getName() + "'>";
            }
        };
    }
}
