package org.python._internal;

import org.python.builtins.AttributeError;
import org.python.builtins.PyException;
import org.python.builtins.TypeError;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public abstract class PyFunction implements PyObject {
    public static PyFunction dynamic(PyCode code, PyLambda o) {
        return new PyDynamicFunction(code, o);
    }

    @Override
    public abstract Object __call__(Object[] args, Map<String, Object> kwargs);

    public static Object fromMethods(Class<?> ownerClass, Object owner, List<Method> value) {
        Set<String> names = value.stream().map(method -> method.getName().isEmpty() ? "<unnamed>" : method.getName()).collect(Collectors.toSet());
        if (names.isEmpty()) throw new PythonVMBug("No matching functions for: " + value);
        String naming = "'" + String.join("', '", names) + "'";
        String text = (names.size() > 1 ? "<functions " : "<function ") + naming + ">";

        return new PyFunction() {
            @Override
            public Object __call__(Object[] args, Map<String, Object> kwargs) {
                try {
                    methods:
                    for (Method method : value) {
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
                } catch (PyException e) {
                    throw e;
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

    public static PyFunction fromMethodsNoJava(Class<?> aClass, Object obj, List<Method> value) {
        Set<String> names = value.stream()
                .filter(method -> !method.getDeclaringClass().equals(Object.class))
                .map(method -> method.getName().isEmpty() ? "<unnamed>" : method.getName())
                .collect(Collectors.toSet());
        if (names.isEmpty()) return null;
        String naming = "'" + String.join("', '", names) + "'";
        String text = (names.size() > 1 ? "<functions " : "<function ") + naming + ">";

        return new PyFunction() {
            private final AtomicBoolean __initialized__ = new AtomicBoolean(false);

            @Override
            public void __init__(Object[] args, Map<String, Object> kwargs) {
                synchronized (__initialized__) {
                    if (__initialized__.get()) return;
                    __initialized__.set(true);
                }

                super.__init__(args, kwargs);

                __setattr__("__name__", (names.size() == 1 ? names.stream().findFirst().orElseThrow() : "<multiple>"));
                __setattr__("__module__", aClass.getPackage().getName());
                __setattr__("__qualname__", aClass.getName());
                __setattr__("__doc__", "");
            }

            @Override
            public String __repr__() {
                return text;
            }

            @Override
            public boolean __eq__(Object other) {
                if (!(other instanceof PyFunction)) return false;
                return this == other;
            }

            @Override
            public int __hash__() {
                return text.hashCode();
            }

            @Override
            public List<String> __dir__() {
                return List.of("__call__", "__str__", "__repr__", "__eq__", "__hash__", "__bool__", "__dir__", "__getattr__", "__setattr__", "__name__", "__module__", "__qualname__", "__doc__");
            }

            @Override
            public Object __getattr__(String name) {
                MetaData meta = MetaDataManager.meta(this);
                if (meta.has(name)) return meta.get(name);
                throw new AttributeError("'" + getClass().getSimpleName() + "' object has no attribute '" + name + "'");
            }

            @Override
            public void __setattr__(String name, Object value) {
                MetaData meta = MetaDataManager.meta(this);
                meta.set(name, value);
            }

            @Override
            public Object __call__(Object[] args, Map<String, Object> kwargs) {
                try {
                    methods:
                    for (Method method : value) {
                        if (Modifier.isStatic(method.getModifiers()) && aClass != obj) continue;
                        if (!Modifier.isStatic(method.getModifiers()) && aClass == obj) continue;
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
                            return method.invoke(obj == aClass ? null : obj, args);
                        }
                    }
                    String collect = Arrays.stream(args).map(o -> o == null ? "None" : o.getClass().getName()).collect(Collectors.joining(", "));

                    throw new TypeError("no matching function for " + text + " in " + aClass.getName() + " " + collect);
                } catch (PyException e) {
                    throw e;
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof RuntimeException) {
                        throw (RuntimeException) cause;
                    } else {
                        throw new TypeError("call to " + text + " failed: " + e.getCause());
                    }
                } catch (Exception e) {
                    if (System.getProperty("dev.ultreon.pythonvm.verbose", "0").equals("1")) {
                        e.printStackTrace();
                    }
                    throw new TypeError("call to " + text + " failed: " + e);
                }
            }

            @Override
            public String __str__() {
                return text;
            }
        };
    }

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

    private static class PyDynamicFunction extends PyFunction {
        private final PyCode code;
        private final PyLambda o;

        public PyDynamicFunction(PyCode code, PyLambda o) {
            this.code = code;
            this.o = o;
        }

        @PyProperty.Getter("__code__")
        public Object $$property$__code__() {
            return code;
        }

        @Override
        public Object __call__(Object[] args, Map<String, Object> kwargs) {
            return o.__call__(args, kwargs);
        }
    }
}
