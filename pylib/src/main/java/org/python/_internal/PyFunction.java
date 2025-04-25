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
                                if (aClass.isPrimitive() && args[0] == null) continue methods;
                                if (!aClass.isPrimitive() && !aClass.isInstance(args[0])) continue methods;
                                if (aClass.isPrimitive()) {
                                    if (aClass == int.class) {
                                        if (!(args[0] instanceof Integer || args[0] instanceof Long)) continue methods;
                                        else if (args[0] instanceof Long) args[0] = ((Long) args[0]).intValue();
                                    } else if (aClass == long.class) {
                                        if (!(args[0] instanceof Integer || args[0] instanceof Long)) continue methods;
                                        else if (args[0] instanceof Integer) args[0] = ((Integer) args[0]).longValue();
                                    } else if (aClass == float.class) {
                                        if (!(args[0] instanceof Float || args[0] instanceof Double)) continue methods;
                                        else if (args[0] instanceof Double) args[0] = ((Double) args[0]).floatValue();
                                    } else if (aClass == double.class) {
                                        if (!(args[0] instanceof Float || args[0] instanceof Double)) continue methods;
                                        else if (args[0] instanceof Float) args[0] = ((Float) args[0]).doubleValue();
                                    } else if (aClass == char.class) {
                                        if (!(args[0] instanceof Character)) continue methods;
                                        else if (args[0] instanceof String) args[0] = ((String) args[0]).charAt(0);
                                    } else if (aClass == boolean.class) {
                                        if (!(args[0] instanceof Boolean)) continue methods;
                                    }
                                }
                            }
                            return method.invoke(owner == ownerClass ? null : owner, args);
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

    public static Object fromMethodsNoJava(Class<?> aClass, Object obj, List<Method> value) {
        Set<String> names = value.stream()
                .filter(method -> !method.getDeclaringClass().equals(Object.class))
                .map(method -> method.getName().isEmpty() ? "<unnamed>" : method.getName())
                .collect(Collectors.toSet());
        if (names.isEmpty()) return null;
        String naming = "'" + String.join("', '", names) + "'";
        String text = (names.size() > 1 ? "<functions " : "<function ") + naming + ">";

        return new PyFunction() {
            @Override
            public Object __call__(Object[] args, Map<String, Object> kwargs) {
                try {
                    methods: for (Method method : value) {
                        if (method.getDeclaringClass() == Object.class) continue;
                        if (PyObject.class.isAssignableFrom(method.getDeclaringClass())) {
                            if (method.getParameterCount() == 2) {
                                Class<?>[] parameterTypes = method.getParameterTypes();
                                if (parameterTypes[0].equals(Object[].class) && parameterTypes[1].equals(Map.class)) {
                                    return method.invoke(obj == aClass ? null : obj, args, kwargs);
                                }
                            }
                        }
                        if (method.getParameterCount() == args.length) {
                            for (Class<?> aClass : method.getParameterTypes()) {
                                if (aClass.isPrimitive() && args[0] == null) continue methods;
                                if (!aClass.isPrimitive() && !aClass.isInstance(args[0])) continue methods;
                                if (aClass.isPrimitive()) {
                                    if (aClass == int.class) {
                                        if (!(args[0] instanceof Integer || args[0] instanceof Long)) continue methods;
                                        else if (args[0] instanceof Long) args[0] = ((Long) args[0]).intValue();
                                    } else if (aClass == long.class) {
                                        if (!(args[0] instanceof Integer || args[0] instanceof Long)) continue methods;
                                        else if (args[0] instanceof Integer) args[0] = ((Integer) args[0]).longValue();
                                    } else if (aClass == float.class) {
                                        if (!(args[0] instanceof Float || args[0] instanceof Double)) continue methods;
                                        else if (args[0] instanceof Double) args[0] = ((Double) args[0]).floatValue();
                                    } else if (aClass == double.class) {
                                        if (!(args[0] instanceof Float || args[0] instanceof Double)) continue methods;
                                        else if (args[0] instanceof Float) args[0] = ((Float) args[0]).doubleValue();
                                    } else if (aClass == char.class) {
                                        if (!(args[0] instanceof Character)) continue methods;
                                        else if (args[0] instanceof String) args[0] = ((String) args[0]).charAt(0);
                                    } else if (aClass == boolean.class) {
                                        if (!(args[0] instanceof Boolean)) continue methods;
                                    }
                                }
                            }
                            return method.invoke(obj, args);
                        }
                    }
                    String collect = Arrays.stream(args).map(o -> o == null ? "None" : o.getClass().getName()).collect(Collectors.joining(", "));

                    throw new TypeError("No matching function for " + text + " in " + aClass.getName() + " " + collect);
                } catch (Exception e) {
                    throw new TypeError("Call to " + text + " failed");
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
