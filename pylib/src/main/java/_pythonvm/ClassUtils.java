package _pythonvm;

import pythonvm.builtins.AttributeException;
import pythonvm.builtins.TypeException;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;

public class ClassUtils {
    public static Object getAttribute(Object obj, String name) throws Exception {
        try {
            return obj.getClass().getMethod("get" + name.substring(0, 1).toUpperCase() + name.substring(1)).invoke(obj);
        } catch (Exception e) {
            try {
                return obj.getClass().getMethod("is" + name.substring(0, 1).toUpperCase() + name.substring(1));
            } catch (Exception e1) {
                try {
                    return obj.getClass().getMethod(name);
                } catch (Exception e2) {
                    try {
                        return obj.getClass().getField(name).get(obj);
                    } catch (Exception e3) {
                        try {
                            return obj.getClass().getMethod("__getattribute__", String.class).invoke(obj, name);
                        } catch (Exception e4) {
                            if (e4 instanceof AttributeException) {
                                return getFallbackAttribute(obj, name);
                            } else if (e4 instanceof InvocationTargetException && e4.getCause() instanceof AttributeException) {
                                throw (AttributeException) e4.getCause();
                            }
                        }
                    }
                }
            }
        }

        throw new AttributeException(name);
    }

    private static Object getFallbackAttribute(Object obj, String name) throws AttributeException, TypeException {
        try {
            return obj.getClass().getMethod("__getattr__", String.class).invoke(obj, name);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new TypeException(e.getCause().getMessage());
        } catch (NoSuchMethodException e) {
            Map<String, Object> dict;
            try {
                dict = (Map<String, Object>) obj.getClass().getField("__dict__").get(obj);
            } catch (Exception e4) {
                if (e4 instanceof AttributeException) throw (AttributeException) e4;
                throw new AttributeException(name);
            }
            Object o = dict.get(name);
            if (o == null) {
                throw new AttributeException(name);
            }
            return o;
        }
    }

    @SuppressWarnings("unchecked")
    public static void setAttribute(Object obj, String name, Object value) throws Exception {
        try {
            obj.getClass().getMethod("set" + name.substring(0, 1).toUpperCase() + name.substring(1), value.getClass()).invoke(obj, value);
        } catch (Exception e) {
            try {
                obj.getClass().getField(name).set(obj, value);
            } catch (Exception e1) {
                try {
                    obj.getClass().getMethod("__setattr__", String.class, value.getClass()).invoke(obj, name, value);
                } catch (Exception e2) {
                    try {
                        Field field = obj.getClass().getField("__dict__");
                        field.setAccessible(true);
                        ((java.util.Map<String, Object>) field.get(obj)).put(name, value);
                    } catch (Exception e3) {
                        throw new AttributeException(name);
                    }
                }
            }
        }
    }

    public static boolean hasAttribute(Object obj, String name) throws Exception {
        try {
            getAttribute(obj, name);
            return true;
        } catch (AttributeException e) {
            return false;
        }
    }

    public static void delAttribute(Object obj, String name) throws Exception {
        try {
            obj.getClass().getMethod("__delattr__", String.class).invoke(obj, name);
        } catch (Exception e) {
            Map<String, Object> dict;
            try {
                dict = (Map<String, Object>) obj.getClass().getField("__dict__").get(obj);
            } catch (Exception e4) {
                throw new AttributeException(name);
            }
            Object remove = dict.remove(name);
            if (remove == null) {
                throw new AttributeException(name);
            }
        }
    }

    public static Class<?> getType(Object obj) throws Exception {
        return obj.getClass();
    }

    public static int getLength(Object obj) throws Exception {
        if (obj.getClass().isArray()) {
            return Array.getLength(obj);
        }
        if (obj instanceof String) {
            return ((String) obj).length();
        }
        if (obj instanceof Collection<?>) {
            return ((Collection<?>) obj).size();
        }
        throw new TypeException("object of type '" + obj.getClass().getSimpleName() + "' has no len()");
    }
}
