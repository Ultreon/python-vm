package org.python._internal;

import jdk.internal.misc.Unsafe;
import org.python.builtins.AttributeError;
import org.python.builtins.TypeError;

import java.lang.reflect.*;
import java.util.*;

public class ClassUtils {
    public static Object initialize(Object obj) {
        MetaData metaData = MetaDataManager.meta(obj);

        if (obj instanceof PyObject) ((PyObject) obj).__init__();

        return obj;
    }

    public static Object getAttribute(Object obj, String name) {
        MetaData metaData = MetaDataManager.meta(obj);
        if (metaData.has(name)) return metaData.get(name);

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
                        if (obj instanceof PyObject) throw new AttributeError(name, obj);
                        try {
                            return obj.getClass().getMethod("__getattribute__", String.class).invoke(obj, name);
                        } catch (Exception e4) {
                            if (e4 instanceof AttributeError) return getFallbackAttribute(obj, name);
                            else if (e4 instanceof InvocationTargetException && e4.getCause() instanceof AttributeError)
                                throw (AttributeError) e4.getCause();
                            else if (e4 instanceof NoSuchMethodException) throw new AttributeError(name, obj);
                            else throw new TypeError(e4.getCause().getMessage());
                        }
                    }
                }
            }
        }
    }

    private static Object getFallbackAttribute(Object obj, String name) throws AttributeError, TypeError {
        try {
            return obj.getClass().getMethod("__getattr__", String.class).invoke(obj, name);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new TypeError(e.getCause().getMessage());
        } catch (NoSuchMethodException e) {
            Map<String, Object> dict;
            try {
                dict = (Map<String, Object>) obj.getClass().getField("__dict__").get(obj);
            } catch (Exception e4) {
                if (e4 instanceof AttributeError) throw (AttributeError) e4;
                throw new AttributeError(name, obj);
            }
            Object o = dict.get(name);
            if (o == null) throw new AttributeError(name);
            return o;
        }
    }

    @SuppressWarnings("unchecked")
    public static void setAttribute(Object obj, String name, Object value) {
        try {
            obj.getClass().getMethod("set" + name.substring(0, 1).toUpperCase() + name.substring(1), value.getClass()).invoke(obj, value);
        } catch (Exception e) {
            if (obj instanceof PyObject) {
                MetaData metaData = MetaDataManager.meta(obj);
                Collection<String> slots = ClassUtils.getSlots(obj.getClass());
                if (slots == null || slots.contains(name)) {
                    metaData.set(name, value);
                    return;
                }
                throw new AttributeError(name);
            }
            try {
                obj.getClass().getField(name).set(obj, value);
            } catch (Exception e1) {
                try {
                    obj.getClass().getMethod("__setattr__", String.class, value.getClass()).invoke(obj, name, value);
                } catch (Exception e2) {
                    MetaData metaData = MetaDataManager.meta(obj);
                    Collection<String> slots = ClassUtils.getSlots(obj.getClass());
                    if (slots == null || slots.contains(name)) metaData.set(name, value);
                }
            }
        }
    }

    public static boolean hasAttribute(Object obj, String name) throws Exception {
        MetaData meta = MetaDataManager.meta(obj);
        if (meta.has(name)) return true;

        try {
            getAttribute(obj, name);
            return true;
        } catch (AttributeError e) {
            return false;
        }
    }

    public static void delAttribute(Object obj, String name) {
        if (obj instanceof PyObject) throw new AttributeError(name);

        MetaData meta = MetaDataManager.meta(obj);
        if (meta.has(name)) {
            meta.del(name);
            return;
        }

        try {
            obj.getClass().getMethod("__delattr__", String.class).invoke(obj, name);
        } catch (Exception e) {
            Map<String, Object> dict;
            try {
                dict = (Map<String, Object>) obj.getClass().getField("__dict__").get(obj);
            } catch (Exception e4) {
                throw new AttributeError(name);
            }
            Object remove = dict.remove(name);
            if (remove == null) throw new AttributeError(name);
        }
    }

    public static Class<?> getType(Object obj) throws Exception {
        return obj.getClass();
    }

    public static Set<String> getDir(Object arg) {
        MetaData meta = MetaDataManager.meta(arg);
        if (meta.has("__dir__")) return (Set<String>) meta.get("__dir__");

        return meta.dir();
    }

    public static Object getDict(Object arg) {
        MetaData meta = MetaDataManager.meta(arg);
        if (meta.has("__dict__")) return meta.get("__dict__");

        return meta.dict();
    }

    public static int getLength(Object obj) {
        if (obj.getClass().isArray()) return Array.getLength(obj);
        if (obj instanceof String) return ((String) obj).length();
        if (obj instanceof Collection<?>) return ((Collection<?>) obj).size();
        throw new TypeError("object of type '" + obj.getClass().getSimpleName() + "' has no len()");
    }

    public static Object getMax(Collection<?> iterable) {
        Object max = iterable.iterator().next();
        for (Object o : iterable)
            if (o instanceof Comparable<?>) {
                Comparable comparable = (Comparable) o;
                try {
                    if (comparable.compareTo(max) > 0) max = o;
                } catch (RuntimeException e) {
                    // ignore
                }
            } else if (o instanceof PyObject) {
                PyObject pyObject = (PyObject) o;
                try {
                    if (pyObject.__gt__(max)) max = o;
                } catch (RuntimeException e) {
                    // ignore
                }
            } else throw new TypeError("object of type '" + o.getClass().getSimpleName() + "' has no __gt__()");
        return max;
    }

    public static Object getMin(Collection<?> iterable) {
        Object min = iterable.iterator().next();
        for (Object o : iterable)
            if (o instanceof Comparable<?>) {
                Comparable comparable = (Comparable) o;
                try {
                    if (comparable.compareTo(min) < 0) min = o;
                } catch (RuntimeException e) {
                    // ignore
                }
            } else if (o instanceof PyObject) {
                PyObject pyObject = (PyObject) o;
                try {
                    if (pyObject.__lt__(min)) min = o;
                } catch (RuntimeException e) {
                    // ignore
                }
            } else throw new TypeError("object of type '" + o.getClass().getSimpleName() + "' has no __lt__()");
        return min;
    }

    public static Object getSum(Collection<?> iterable) {
        Object sum = 0;
        for (Object o : iterable)
            if (o instanceof Number) sum = ((Number) sum).doubleValue() + ((Number) o).doubleValue();
            else throw new TypeError("object of type '" + o.getClass().getSimpleName() + "' has no __add__()");
        return sum;
    }

    public static String stringify(Object arg) {
        if (arg instanceof PyObject) {
            PyObject pyObject = (PyObject) arg;
            return pyObject.__str__();
        }
        if (arg == null) return "None";

        return arg.toString();
    }

    public static Object getHash(Object o) {
        if (o instanceof PyObject) return ((PyObject) o).__hash__();
        return o.hashCode();
    }

    public static Object getAbs(Object o) {
        if (o instanceof PyObject) return ((PyObject) o).__abs__();
        if (o instanceof Number) return ((Number) o).doubleValue();
        throw new TypeError("object of type '" + o.getClass().getSimpleName() + "' has no __abs__()");
    }

    public static Object getBool(Object o) {
        if (o instanceof PyObject) return ((PyObject) o).__bool__();
        if (o instanceof Boolean) return (Boolean) o;
        return o != null;
    }

    public static Object getRepr(Object o) {
        if (o instanceof PyObject) return ((PyObject) o).__repr__();
        return "<java object '" + o.getClass().getName() + "' at 0x" + Integer.toHexString(System.identityHashCode(o)) + ">";
    }

    public static Object getStr(Object o) {
        if (o instanceof PyObject) return ((PyObject) o).__str__();
        return o.toString();
    }

    public static Collection<String> getSlots(Class<?> aClass) {
        if (!PyObject.class.isAssignableFrom(aClass)) return null;

        try {
            Field field = aClass.getField("__slots__");
            if (!Modifier.isStatic(field.getModifiers())) return null;
            Object o = field.get(null);

            if (o instanceof String[]) return Arrays.asList((String[]) o);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // ignore
        }
        return null;
    }

    public static Object getInt(Object arg) {
        if (arg instanceof PyObject) return ((PyObject) arg).__int__();
        if (arg instanceof Number) return ((Number) arg).longValue();
        MetaData metaData = MetaDataManager.meta(arg);
        return call(metaData.get("__int__"), new Object[]{arg}, Collections.emptyMap());
    }

    public static Object getFloat(Object arg) {
        if (arg instanceof PyObject) return ((PyObject) arg).__float__();
        if (arg instanceof Number) return ((Number) arg).doubleValue();
        MetaData metaData = MetaDataManager.meta(arg);
        return call(metaData.get("__float__"), new Object[]{arg}, Collections.emptyMap());
    }

    public static Object call(Object value, Object[] args, Map<String, Object> kwargs) {
        MetaData metaData = MetaDataManager.meta(value);
        if (metaData.has("__call__")) {
            Object o = metaData.get("__call__");
            if (o instanceof PyFunction) return ((PyFunction) o).__call__(args, kwargs);
        }

        if (value instanceof PyObject) return ((PyObject) value).__call__(args, kwargs);
        if (value instanceof Class<?>) {
            Class<?> aClass = (Class<?>) value;
            constructors: for (Constructor<?> constructor : aClass.getConstructors())
                if (constructor.getParameterCount() == args.length) {
                    for (int i = 0; i < args.length; i++)
                        if (!constructor.getParameterTypes()[i].isInstance(args[i])) continue constructors;
                    try {
                        return constructor.newInstance(args);
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                        throw new PythonException("Error calling constructor: " + e.getMessage(), e);
                    }
                }

            call_methods: for (Method method : aClass.getMethods())
                if (method.getName().equals("__call__") && method.getParameterCount() == args.length) {
                    for (int i = 0; i < args.length; i++)
                        if (!method.getParameterTypes()[i].isInstance(args[i])) {
                            continue call_methods;
                        }

                    if (Modifier.isStatic(method.getModifiers())) try {
                        return method.invoke(null, args);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new PythonException("Error calling method: " + e.getMessage(), e);
                    }
                }

            throw new TypeError("no matching constructor found for type '" + aClass.getName() + "'");
        }
        throw new TypeError("object of type '" + value.getClass().getSimpleName() + "' has no __call__()");
    }

    public static Map<String, List<Method>> methodsByName(Class<?> aClass) {
        Map<String, List<Method>> methodsByName = new HashMap<>();

        for (Method method : aClass.getMethods())
            methodsByName.computeIfAbsent(method.getName(), k -> new ArrayList<>()).add(method);

        return methodsByName;
    }

    public static Object add(Object self, Object other) {
        if (self instanceof PyObject) return ((PyObject) self).__add__(other);
        if (self instanceof Number) {
            if (isInt(self) && isInt(other)) return ((Number) self).longValue() + ((Number) other).longValue();
            return ((Number) self).doubleValue() + ((Number) other).doubleValue();
        }
        throw new TypeError("object of type '" + self.getClass().getSimpleName() + "' has no __add__()");
    }
    
    public static Object sub(Object self, Object other) {
        if (self instanceof PyObject) return ((PyObject) self).__sub__(other);
        if (self instanceof Number) {
            if (isInt(self) && isInt(other)) return ((Number) self).longValue() - ((Number) other).longValue();
            return ((Number) self).doubleValue() - ((Number) other).doubleValue();
        }
        throw new TypeError("object of type '" + self.getClass().getSimpleName() + "' has no __sub__()");
    }
    
    public static Object mul(Object self, Object other) {
        if (self instanceof PyObject) return ((PyObject) self).__mul__(other);
        if (self instanceof Number) {
            if (isInt(self) && isInt(other)) return ((Number) self).longValue() * ((Number) other).longValue();
            return ((Number) self).doubleValue() * ((Number) other).doubleValue();
        }
        throw new TypeError("object of type '" + self.getClass().getSimpleName() + "' has no __mul__()");
    }
    
    public static Object div(Object self, Object other) {
        if (self instanceof PyObject) return ((PyObject) self).__div__(other);
        if (self instanceof Number) {
            if (isInt(self) && isInt(other)) return ((Number) self).longValue() / ((Number) other).longValue();
            return ((Number) self).doubleValue() / ((Number) other).doubleValue();
        }
        throw new TypeError("object of type '" + self.getClass().getSimpleName() + "' has no __div__()");
    }
    
    public static Object mod(Object self, Object other) {
        if (self instanceof PyObject) return ((PyObject) self).__mod__(other);
        if (self instanceof Number) {
            if (isInt(self) && isInt(other)) return ((Number) self).longValue() % ((Number) other).longValue();
            return ((Number) self).doubleValue() % ((Number) other).doubleValue();
        }
        throw new TypeError("object of type '" + self.getClass().getSimpleName() + "' has no __mod__()");
    }

    private static boolean isInt(Object self) {
        return self instanceof Long || self instanceof Integer || self instanceof Short || self instanceof Byte || self instanceof Character;
    }

    private static boolean isFloat(Object self) {
        return self instanceof Double || self instanceof Float;
    }

    public static Object neg(Object self) {
        if (self instanceof PyObject) return ((PyObject) self).__neg__();
        if (isInt(self)) return -((Number) self).longValue();
        if (self instanceof Number) return -((Number) self).doubleValue();
        throw new TypeError("object of type '" + self.getClass().getSimpleName() + "' has no __neg__()");
    }

    public static Object pos(Object self) {
        if (self instanceof PyObject) return ((PyObject) self).__pos__();
        if (isInt(self)) return +((Number) self).longValue();
        if (self instanceof Number) return +((Number) self).doubleValue();
        throw new TypeError("object of type '" + self.getClass().getSimpleName() + "' has no __pos__()");
    }

    public static Object pow(Object self, Object other) {
        if (self instanceof PyObject) return ((PyObject) self).__pow__(other);
        if (self instanceof Number) {
            if (isInt(self) && isInt(other))
                return (long) Math.pow(((Number) self).longValue(), ((Number) other).longValue());
            return Math.pow(((Number) self).doubleValue(), ((Number) other).doubleValue());
        }
        throw new TypeError("object of type '" + self.getClass().getSimpleName() + "' has no __pow__()");
    }

    public static Object floordiv(Object self, Object other) {
        if (self instanceof PyObject) return ((PyObject) self).__floordiv__(other);
        if (self instanceof Number) {
            return Math.floorDiv(((Number) self).longValue(), ((Number) other).longValue());
        }
        throw new TypeError("object of type '" + self.getClass().getSimpleName() + "' has no __floordiv__()");
    }

    public static Object truediv(Object self, Object other) {
        if (self instanceof PyObject) return ((PyObject) self).__truediv__(other);
        if (self instanceof Number) {
            if (isInt(self) && isInt(other)) return ((Number) self).longValue() / ((Number) other).longValue();
            return ((Number) self).doubleValue() / ((Number) other).doubleValue();
        }
        throw new TypeError("object of type '" + self.getClass().getSimpleName() + "' has no __truediv__()");
    }

    public static Object lshift(Object self, Object other) {
        if (self instanceof PyObject) return ((PyObject) self).__lshift__(other);
        if (self instanceof Number) {
            if (!(isInt(self) || !(isInt(other)))) throw new TypeError("object of type '" + self.getClass().getSimpleName() + "' has no __lshift__()");
            return ((Number) self).longValue() << ((Number) other).intValue();
        }
        throw new TypeError("object of type '" + self.getClass().getSimpleName() + "' has no __lshift__()");
    }

    public static Object rshift(Object self, Object other) {
        if (self instanceof PyObject) return ((PyObject) self).__rshift__(other);
        if (self instanceof Number) {
            if (!(isInt(self)) || !(isInt(other))) throw new TypeError("object of type '" + self.getClass().getSimpleName() + "' has no __rshift__()");
            return ((Number) self).longValue() >> ((Number) other).intValue();
        }
        throw new TypeError("object of type '" + self.getClass().getSimpleName() + "' has no __rshift__()");
    }

    public static Object and(Object self, Object other) {
        if (self instanceof PyObject) return ((PyObject) self).__and__(other);
        if (self instanceof Number) {
            if (!(isInt(self)) || !(isInt(other))) throw new TypeError("object of type '" + self.getClass().getSimpleName() + "' has no __and__()");
            return ((Number) self).longValue() & ((Number) other).intValue();
        }
        throw new TypeError("object of type '" + self.getClass().getSimpleName() + "' has no __and__()");
    }

    public static Object or(Object self, Object other) {
        if (self instanceof PyObject) return ((PyObject) self).__or__(other);
        if (self instanceof Number) {
            if (!(isInt(self)) || !(isInt(other))) throw new TypeError("object of type '" + self.getClass().getSimpleName() + "' has no __or__()");
            return ((Number) self).longValue() | ((Number) other).intValue();
        }
        throw new TypeError("object of type '" + self.getClass().getSimpleName() + "' has no __or__()");
    }

    public static Object xor(Object self, Object other) {
        if (self instanceof PyObject) return ((PyObject) self).__xor__(other);
        if (self instanceof Number) {
            if (!(isInt(self)) || !(isInt(other))) throw new TypeError("object of type '" + self.getClass().getSimpleName() + "' has no __xor__()");
            return ((Number) self).longValue() ^ ((Number) other).intValue();
        }
        throw new TypeError("object of type '" + self.getClass().getSimpleName() + "' has no __xor__()");
    }

    public static Object is_(Object self, Object other) {
        if (self instanceof PyObject) return ((PyObject) self).__is__(other);
        if (other instanceof Class<?>) {
            return ((Class<?>) other).isInstance(self);
        }
        return self.equals(other);
    }

    public static Object is_not(Object self, Object other) {
        return not(is_(self, other));
    }

    public static Object not(Object self) {
        if (self instanceof PyObject) return ((PyObject) self).__not__();
        if (self instanceof Boolean) return !((Boolean) self);
        if (isInt(self)) return ~((Number) self).longValue();
        throw new TypeError("object of type '" + self.getClass().getSimpleName() + "' has no __not__()");
    }

    public static Object in(Object self, Object other) {
        if (other instanceof PyObject) return ((PyObject) other).__contains__(self);
        if (other instanceof Collection<?>) {
            Collection<?> collection = (Collection<?>) other;
            return collection.contains(self);
        }
        if (other == null) return false;
        if (other.getClass().isArray()) {
            for (int i = 0; i < Array.getLength(other); i++) {
                if ((Boolean)getBool(eq(Array.get(other, i), self))) {
                    return true;
                }
            }
            return false;
        }
        throw new TypeError("object of type '" + self.getClass().getSimpleName() + "' has no __contains__()");
    }

    public static Object not_in(Object self, Object other) {
        return not(in(self, other));
    }

    public static Object eq(Object self, Object other) {
        if (self instanceof PyObject) return ((PyObject) self).__eq__(other);
        return self.equals(other);
    }

    public static Object ne(Object self, Object other) {
        return not(eq(self, other));
    }

    public static Object lt(Object self, Object other) {
        if (self instanceof PyObject) return ((PyObject) self).__lt__(other);
        if (self instanceof Comparable) {
            return ((Comparable) self).compareTo(other) < 0;
        }
        throw new TypeError("object of type '" + self.getClass().getSimpleName() + "' has no __lt__()");
    }

    public static Object le(Object self, Object other) {
        return not(gt(self, other));
    }

    public static Object gt(Object self, Object other) {
        if (self instanceof PyObject) return ((PyObject) self).__gt__(other);
        if (self instanceof Comparable) {
            return ((Comparable) self).compareTo(other) > 0;
        }
        throw new TypeError("object of type '" + self.getClass().getSimpleName() + "' has no __gt__()");
    }

    public static Object ge(Object self, Object other) {
        return not(lt(self, other));
    }

    public static Object abs(Object self) {
        if (self instanceof PyObject) return ((PyObject) self).__abs__();
        if (isInt(self)) return ((Number) self).longValue();
        if (self instanceof Number) return Math.abs(((Number) self).doubleValue());
        throw new TypeError("object of type '" + self.getClass().getSimpleName() + "' has no __abs__()");
    }
}
