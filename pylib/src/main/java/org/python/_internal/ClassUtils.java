package org.python._internal;

import dev.ultreon.pythonc.classes.PyClass;
import org.codehaus.groovy.util.ArrayIterator;
import org.python.builtins.*;
import org.python.builtins.RuntimeError;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

public class ClassUtils {
    private static List<String> modules;

    public static Object initialize(Object obj) {
        MetaData meta = MetaDataManager.meta(obj);

        if (obj instanceof PyObject) ((PyObject) obj).__init__(new Object[0], Map.of());
        if (meta == null) return obj;
        if (obj instanceof PyClass) {
            call(meta.has("__new__"), new Object[]{obj}, Map.of());
        }
        if (meta.has("__init__")) {
            call(meta.get("__init__"), new Object[0], Map.of());
        }

        return obj;
    }

    public static Object getAttribute(Object obj, String name) {
        if (name.startsWith("#")) name = "\\" + name;
        if (obj == null) throw new AttributeError(name, null);
        MetaData meta = MetaDataManager.meta(obj);
        if (meta == null) throw new AttributeError(name, obj);
        if (meta.has(name)) return meta.get(name);
        if (meta.has("#" + name + ":getter")) return call(meta.get("#" + name + ":getter"));
        try {
            if (meta.has("__getattr__")) return call(meta.get("__getattr__"), name);
        } catch (AttributeError e) {
            // ignore
        }
        if (meta.has("__getattribute__")) return call(meta.get("__getattribute__"), name);
        throw new AttributeError(name, obj);
    }

    public static void setAttribute(Object obj, String name, Object value) {
        if (name.startsWith("#")) name = "\\" + name;
        if (obj == null) throw new AttributeError(name, null);
        try {
            obj.getClass().getMethod("set" + name.substring(0, 1).toUpperCase() + name.substring(1), value.getClass()).invoke(obj, value);
        } catch (Exception e) {
            if (obj instanceof PyObject) {
                if (setAttr0(obj, name, value, e)) return;
                throw new AttributeError(name);
            }
            try {
                obj.getClass().getField(name).set(obj, value);
            } catch (Exception e1) {
                try {
                    obj.getClass().getMethod("__setattr__", String.class, value.getClass()).invoke(obj, name, value);
                } catch (Exception e2) {
                    setAttr0(obj, name, value, e2);
                }
            }
        }
    }

    private static boolean setAttr0(Object obj, String name, Object value, Exception e) {
        if (name.startsWith("#")) name = "\\" + name;
        MetaData metaData = MetaDataManager.meta(obj);
        if (metaData == null) throw new AttributeError(name, obj);
        Collection<String> slots = ClassUtils.getSlots(obj.getClass());
        if (slots == null || slots.contains(name)) {
            Method method = (Method) metaData.get("#" + name + ":getter");
            if (method != null) {
                try {
                    method.invoke(obj, value);
                    return true;
                } catch (IllegalAccessException ex) {
                    throw new TypeError("cannot access property '" + name + "'");
                } catch (InvocationTargetException ex) {
                    throw new RuntimeError("failed to set property value for '" + name + "': " + e.getLocalizedMessage());
                }
            }
            metaData.set(name, value);
            return true;
        }
        return false;
    }

    public static boolean hasAttribute(Object obj, String name) {
        if (name.startsWith("#")) name = "\\" + name;
        if (obj == null) return false;
        MetaData meta = MetaDataManager.meta(obj);
        if (meta == null) throw new AttributeError(name, obj);
        if (meta.has(name) || meta.has("#" + name + ":getter") || meta.has("#" + name + ":setter")) return true;

        try {
            getAttribute(obj, name);
            return true;
        } catch (AttributeError e) {
            return false;
        }
    }

    public static void delAttribute(Object obj, String name) {
        if (name.startsWith("#")) name = "\\" + name;
        if (obj == null) throw new AttributeError(name, null);
        if (obj instanceof PyObject) throw new AttributeError(name);

        MetaData meta = MetaDataManager.meta(obj);
        if (meta == null) throw new AttributeError(name, obj);
        if (meta.has(name)) {
            meta.del(name);
            return;
        }

        try {
            obj.getClass().getMethod("__delattr__", String.class).invoke(obj, name);
        } catch (PyException e) {
            throw e;
        } catch (Exception e) {
            throw new AttributeError(name);
        }
    }

    public static Class<?> getType(Object obj) {
        if (obj == null) return void.class;
        return obj.getClass();
    }

    public static Set<String> getDir(Object obj) {
        if (obj == null) throw new AttributeError("__dir__", null);
        MetaData meta = MetaDataManager.meta(obj);
        if (meta == null) throw new AttributeError("__dir__", obj);
        if (meta.has("__dir__")) return (Set<String>) meta.get("__dir__");
        return meta.dir();
    }

    public static Object getDict(Object obj) {
        MetaData meta = MetaDataManager.meta(obj);
        if (meta == null) throw new AttributeError("__dir__", obj);
        if (meta.has("__dict__")) return meta.get("__dict__");

        return meta.dict();
    }

    public static Object getLength(Object obj) {
        MetaData meta = MetaDataManager.meta(obj);
        if (meta == null) throw new AttributeError("__len__", obj);
        if (meta.has("__len__")) return call(meta.get("__len__"));
        if (obj.getClass().isArray()) return Array.getLength(obj);
        if (obj instanceof String) return ((String) obj).length();
        if (obj instanceof Collection<?>) return ((Collection<?>) obj).size();
        throw new TypeError("object of type '" + obj.getClass().getSimpleName() + "' has no len()");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Object getMax(Collection<?> iterable) {
        Object max = iterable.iterator().next();
        for (Object o : iterable)
            if (o instanceof Comparable<?>) {
                Comparable comparable = (Comparable) o;
                try {
                    if (comparable.compareTo(max) > 0) max = o;
                } catch (Exception e) {
                    // Ignore
                }
            } else if (o instanceof PyObject pyObject) {
                try {
                    if (pyObject.__gt__(max)) max = o;
                } catch (RuntimeException e) {
                    // ignore
                }
            } else throw new TypeError("object of type '" + o.getClass().getSimpleName() + "' has no __gt__()");
        return max;
    }

    public static Object getMin(Collection<?> obj) {
        Object min = obj.iterator().next();
        for (Object o : obj)
            if (o instanceof Comparable<?>) {
                Comparable comparable = (Comparable) o;
                try {
                    if (comparable.compareTo(min) < 0) min = o;
                } catch (RuntimeException e) {
                    // ignore
                }
            } else if (o instanceof PyObject pyObject) {
                try {
                    if (pyObject.__lt__(min)) min = o;
                } catch (RuntimeException e) {
                    // ignore
                }
            } else {
                MetaData meta = MetaDataManager.meta(obj);
                if (meta == null) throw new AttributeError("__lt__", obj);
                if (meta.has("__lt__")) {
                    Object lesser = call(meta.get("__lt__"), min);
                    if (!(lesser instanceof Boolean)) throw new TypeError("Function '__lt__' did not return a bool");
                    if ((boolean) lesser) min = o;
                } else throw new AttributeError("__lt__", obj);
            }
        return min;
    }

    public static Object getSum(Collection<?> iterable) {
        Number sum = 0;
        for (Object o : iterable)
            if (o instanceof Number) sum = sum.doubleValue() + ((Number) o).doubleValue();
            else throw new TypeError("object of type '" + o.getClass().getSimpleName() + "' has no __add__()");
        return sum;
    }

    public static String stringify(Object arg) {
        if (arg instanceof PyObject pyObject) {
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
        if (o instanceof Boolean) return o;
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

    public static Object getInt(Object obj) {
        if (obj instanceof PyObject) return ((PyObject) obj).__int__();
        if (obj instanceof Number) return ((Number) obj).longValue();
        MetaData meta = MetaDataManager.meta(obj);
        if (meta == null) throw new AttributeError("__int__", obj);
        return call(meta.get("__int__"), new Object[]{obj}, Collections.emptyMap());
    }

    public static Object getFloat(Object obj) {
        if (obj instanceof PyObject) return ((PyObject) obj).__float__();
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        MetaData meta = MetaDataManager.meta(obj);
        if (meta == null) throw new AttributeError("__float__", obj);
        return call(meta.get("__float__"), new Object[]{obj}, Collections.emptyMap());
    }

    public static Object call(Object value, Object... args) {
        return call(value, args, Map.of());
    }

    public static Object call(Object value, Object[] args, Map<String, Object> kwargs) {
        MetaData metaData = MetaDataManager.meta(value);
        if (value instanceof PyObject) return ((PyObject) value).__call__(args, kwargs);
        if (value instanceof Method method) {
            if (!kwargs.isEmpty()) throw new TypeError("java functions do not support keyword arguments");
            try {
                if (Modifier.isStatic(method.getModifiers())) return method.invoke(null, args);
                else return method.invoke(args[0], Arrays.copyOfRange(args, 1, args.length));
            } catch (Exception e) {
                throw new TypeError(e.toString());
            }
        }
        if (value instanceof Class<?> aClass) {
            constructors:
            for (Constructor<?> constructor : aClass.getConstructors())
                if (constructor.getParameterCount() == args.length) {
                    for (int i = 0; i < args.length; i++) {
                        if (constructor.getParameterTypes()[i].isPrimitive() && args[i] == null) continue constructors;
                        if (!constructor.getParameterTypes()[i].isPrimitive() && !constructor.getParameterTypes()[i].isInstance(args[i]))
                            continue constructors;
                        if (constructor.getParameterTypes()[i].isPrimitive()) {
                            if (constructor.getParameterTypes()[i] == int.class) {
                                if (!(args[i] instanceof Integer || args[i] instanceof Long)) continue constructors;
                                else if (args[i] instanceof Long) args[i] = ((Long) args[i]).intValue();
                            } else if (constructor.getParameterTypes()[i] == long.class) {
                                if (!(args[i] instanceof Integer || args[i] instanceof Long)) continue constructors;
                                else if (args[i] instanceof Integer) args[i] = ((Integer) args[i]).longValue();
                            } else if (constructor.getParameterTypes()[i] == byte.class) {
                                if (!(args[i] instanceof Integer || args[i] instanceof Long)) continue constructors;
                                else if (args[i] instanceof Long) args[i] = ((Long) args[i]).byteValue();
                            } else if (constructor.getParameterTypes()[i] == short.class) {
                                if (!(args[i] instanceof Integer || args[i] instanceof Long)) continue constructors;
                                else if (args[i] instanceof Long) args[i] = ((Long) args[i]).shortValue();
                            } else if (constructor.getParameterTypes()[i] == float.class) {
                                if (!(args[i] instanceof Float || args[i] instanceof Double)) continue constructors;
                                else if (args[i] instanceof Double) args[i] = ((Double) args[i]).floatValue();
                            } else if (constructor.getParameterTypes()[i] == double.class) {
                                if (!(args[i] instanceof Float || args[i] instanceof Double)) continue constructors;
                                else if (args[i] instanceof Float) args[i] = ((Float) args[i]).doubleValue();
                            }
                        }
                    }
                    try {
                        return constructor.newInstance(args);
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                        throw new PythonException("Error calling constructor: " + e, e);
                    }
                }

            call_methods:
            for (Method method : aClass.getMethods())
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

            throw new TypeError("no matching constructor found for type '" + aClass.getName() + "' with arguments: (" + Arrays.stream(args).map(o -> o == null ? "None" : o.getClass().getName()).collect(Collectors.joining(", ")) + ")");
        }
        if (metaData != null && metaData.has("__call__")) {
            Object o = metaData.get("__call__");
            if (o instanceof PyFunction) return ((PyFunction) o).__call__(args, kwargs);
        }

        throw new AttributeError("__call__", value);
    }

    public static Map<String, List<Method>> methodsByName(Class<?> aClass) {
        Map<String, List<Method>> methodsByName = new HashMap<>();

        Stack<Class<?>> stack = new Stack<>();
        stack.push(aClass);
        while (!stack.isEmpty()) {
            for (Method method : aClass.getMethods()) {
                List<Method> methods = methodsByName.computeIfAbsent(method.getName(), _ -> new ArrayList<>());
                if (methods.contains(method)) continue;
                methods.add(method);
            }

            for (Method method : aClass.getDeclaredMethods()) {
                List<Method> methods = methodsByName.computeIfAbsent(method.getName(), _ -> new ArrayList<>());
                if (methods.contains(method)) continue;
                methods.add(method);
            }

            stack.pop();
            Class<?> superclass = aClass.getSuperclass();
            if (superclass != null) {
                stack.push(superclass);
                aClass = superclass;
            }

            for (Class<?> anInterface : aClass.getInterfaces()) {
                stack.push(anInterface);
                aClass = anInterface;
            }
        }

        return methodsByName;
    }

    public static Object add(Object self, Object other) {
        if (self instanceof PyObject) return ((PyObject) self).__add__(other);
        if (self instanceof Number) {
            if (isInt(self) && isInt(other)) return ((Number) self).longValue() + ((Number) other).longValue();
            return ((Number) self).doubleValue() + ((Number) other).doubleValue();
        }
        throw new AttributeError("__add__", self);
    }

    public static Object sub(Object self, Object other) {
        if (self instanceof PyObject) return ((PyObject) self).__sub__(other);
        if (self instanceof Number) {
            if (isInt(self) && isInt(other)) return ((Number) self).longValue() - ((Number) other).longValue();
            return ((Number) self).doubleValue() - ((Number) other).doubleValue();
        }
        throw new AttributeError("__sub__", self);
    }

    public static Object mul(Object self, Object other) {
        if (self instanceof PyObject) return ((PyObject) self).__mul__(other);
        if (self instanceof Number) {
            if (isInt(self) && isInt(other)) return ((Number) self).longValue() * ((Number) other).longValue();
            return ((Number) self).doubleValue() * ((Number) other).doubleValue();
        }
        throw new AttributeError("__mul__", self);
    }

    public static Object div(Object self, Object other) {
        if (self instanceof PyObject) return ((PyObject) self).__div__(other);
        if (self instanceof Number) {
            if (isInt(self) && isInt(other)) return ((Number) self).longValue() / ((Number) other).longValue();
            return ((Number) self).doubleValue() / ((Number) other).doubleValue();
        }
        throw new AttributeError("__div__", self);
    }

    public static Object mod(Object self, Object other) {
        if (self instanceof PyObject) return ((PyObject) self).__mod__(other);
        if (self instanceof Number) {
            if (isInt(self) && isInt(other)) return ((Number) self).longValue() % ((Number) other).longValue();
            return ((Number) self).doubleValue() % ((Number) other).doubleValue();
        }
        throw new AttributeError("__mod__", self);
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
        throw new AttributeError("__pow__", self);
    }

    public static Object floordiv(Object self, Object other) {
        if (self instanceof PyObject) return ((PyObject) self).__floordiv__(other);
        if (self instanceof Number) {
            return Math.floorDiv(((Number) self).longValue(), ((Number) other).longValue());
        }
        throw new AttributeError("__floordiv__", self);
    }

    public static Object truediv(Object self, Object other) {
        if (self instanceof PyObject) return ((PyObject) self).__truediv__(other);
        if (self instanceof Number) {
            if (isInt(self) && isInt(other)) return ((Number) self).longValue() / ((Number) other).longValue();
            return ((Number) self).doubleValue() / ((Number) other).doubleValue();
        }
        throw new AttributeError("__truediv__", self);
    }

    public static Object lshift(Object self, Object other) {
        if (self instanceof PyObject) return ((PyObject) self).__lshift__(other);
        if (self instanceof Number) {
            if (!(isInt(self) || !(isInt(other))))
                throw new TypeError("object of type '" + self.getClass().getSimpleName() + "' has no __lshift__()");
            return ((Number) self).longValue() << ((Number) other).intValue();
        }
        throw new AttributeError("__lshift__", self);
    }

    public static Object rshift(Object self, Object other) {
        if (self instanceof PyObject) return ((PyObject) self).__rshift__(other);
        if (self instanceof Number) {
            if (!(isInt(self)) || !(isInt(other)))
                throw new TypeError("object of type '" + self.getClass().getSimpleName() + "' has no __rshift__()");
            return ((Number) self).longValue() >> ((Number) other).intValue();
        }
        throw new AttributeError("__rshift__", self);
    }

    public static Object and(Object self, Object other) {
        if (self instanceof PyObject) return ((PyObject) self).__and__(other);
        if (self instanceof Number) {
            if (!(isInt(self)) || !(isInt(other)))
                throw new TypeError("object of type '" + self.getClass().getSimpleName() + "' has no __and__()");
            return ((Number) self).longValue() & ((Number) other).intValue();
        }
        throw new TypeError("object of type '" + self.getClass().getSimpleName() + "' has no __and__()");
    }

    public static Object or(Object self, Object other) {
        if (self instanceof PyObject) return ((PyObject) self).__or__(other);
        if (self instanceof Number) {
            if (!(isInt(self)) || !(isInt(other)))
                throw new TypeError("object of type '" + self.getClass().getSimpleName() + "' has no __or__()");
            return ((Number) self).longValue() | ((Number) other).intValue();
        }
        throw new TypeError("object of type '" + self.getClass().getSimpleName() + "' has no __or__()");
    }

    public static Object xor(Object self, Object other) {
        if (self instanceof PyObject) return ((PyObject) self).__xor__(other);
        if (self instanceof Number) {
            if (!(isInt(self)) || !(isInt(other)))
                throw new TypeError("object of type '" + self.getClass().getSimpleName() + "' has no __xor__()");
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
        if (other instanceof Collection<?> collection) {
            for (Object o : collection) {
                if ((Boolean) eq(o, self)) return true;
            }

            return false;
        }
        if (other == null) return false;
        if (other.getClass().isArray()) {
            for (int i = 0; i < Array.getLength(other); i++) {
                if ((Boolean) getBool(eq(Array.get(other, i), self))) {
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
        if (self instanceof Number && other instanceof Number) {
            if (isInt(self) && isInt(other)) return ((Number) self).longValue() == ((Number) other).longValue();
            return ((Number) self).doubleValue() == ((Number) other).doubleValue();
        }
        if (self instanceof Enum<?> && other instanceof String) {
            return ((Enum<?>) self).name().equals(other);
        } else if (other instanceof Enum<?> && self instanceof String) {
            return ((Enum<?>) other).name().equals(self);
        } else if (self instanceof Enum<?> && other instanceof Number) {
            return ((Enum<?>) self).ordinal() == ((Number) other).intValue();
        } else if (other instanceof Enum<?> && self instanceof Number) {
            return ((Enum<?>) other).ordinal() == ((Number) self).intValue();
        }
        if (self instanceof Comparable) {
            return ((Comparable) self).compareTo(other) == 0;
        }
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

    public static Object iter(Object self) {
        if (self instanceof PyObject) return ((PyObject) self).__iter__();
        if (self instanceof Iterable) return ((Iterable<?>) self).iterator();
        if (self != null && self.getClass().isArray()) return new ArrayIterator<Object>((Object[]) self);
        if (self instanceof String) return new StringIterator((String) self);
        if (self instanceof Map) return ((Map<?, ?>) self).entrySet().iterator();
        throw new TypeError("object of type '" + self.getClass().getSimpleName() + "' has no __iter__()");
    }

    public static Object next(Object self) {
        if (self instanceof PyObject) return ((PyObject) self).__next__();
        if (self instanceof Iterator) {
            if (!((Iterator<?>) self).hasNext()) throw new StopIteration();
            return ((Iterator<?>) self).next();
        }
        throw new TypeError("object of type '" + self.getClass().getSimpleName() + "' has no __next__()");
    }

    public static Object hasNext(Object self) {
        if (self instanceof PyObject) return true;
        if (self instanceof Iterator) return ((Iterator<?>) self).hasNext();
        throw new TypeError("object of type '" + self.getClass().getSimpleName() + "' has no __next__()");
    }

    public static Object zip(Object ignoredIterable) {
        throw new NotImplementedError();
    }

    public static Object enumerate(Object iterable) {
        if (iterable instanceof PyObject) return ((PyObject) iterable).__enumerate__();
        if (iterable instanceof Iterable) return new EnumerateIterator((Iterable<?>) iterable);
        if (iterable != null && iterable.getClass().isArray()) {
            return new EnumerateIterator(Arrays.asList((Object[]) iterable));
        }
        throw new TypeError("object of type '" + iterable.getClass().getSimpleName() + "' has no __enumerate__()");
    }

    public static List<String> getModules() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        List<String> modules = new ArrayList<>();
        try {
            Package[] packages = classLoader.getDefinedPackages();
            for (Package pkg : packages) {
                modules.add(pkg.getName());
            }
        } catch (Throwable ignored) {
            throw new OSError("failed to get modules");
        }
        return modules;
    }

    public static Object importModule(String module, String name) {
        ClassLoader classLoader = ClassUtils.class.getClassLoader();
        if (name.equals("*")) return loadClassAttr(module, name, classLoader);
        try {
            Class<?> aClass = classLoader.loadClass(module + "." + name);
            if (aClass == null) throw new OSError("could not import '" + name + "' from '" + module + "'");
            return aClass;
        } catch (ClassNotFoundException e) {
            return loadClassAttr(module, name, classLoader);
        } catch (Throwable e) {
            throw new OSError("failed to import '" + name + "' from '" + module + "'");
        }
    }

    private static Object loadClassAttr(String module, String name, ClassLoader classLoader) {
        try {
            Class<?> aClass = classLoader.loadClass(module);
            if (aClass == null) throw new OSError("could not import module '" + module + "'");
            MetaData meta = MetaDataManager.meta(aClass);
            return meta.get(name);
        } catch (ClassNotFoundException e1) {
            return loadPackage(module, name, classLoader);
        }
    }

    private static Package loadPackage(String module, String name, ClassLoader classLoader) {
        try {
            if (name.equals("*")) {
                Package definedPackage = classLoader.getDefinedPackage(module);
                if (definedPackage == null) throw new ImportError("could not import module '" + module + "'");
                return definedPackage;
            }
            Package definedPackage = classLoader.getDefinedPackage(module + "." + name);
            if (definedPackage == null) throw new ImportError("could not import '" + name + "' from '" + module + "'");
            return definedPackage;
        } catch (ImportError e) {
            throw e;
        } catch (Throwable e2) {
            throw new OSError("failed to import '" + name + "' from '" + module + "'");
        }
    }

    public static Object importModule(String module) {
        return importModule(module, "*");
    }

    public static Object newClass(Class<?> type) {
        MetaData meta = MetaDataManager.meta(type);
        meta.set("__getattr__", PyFunction.dynamic(PyCode.builder()
                        .argCount(1).posOnlyArgCount(0).kwOnlyArgCount(0).nLocals(1)
                        .stackSize(1).flags(0).varNames("key")
                        .filename("_builtins.class").name("__getattr__").firstLineNo(1).build(),
                (args, _) -> {
                    Object arg = args[0];
                    if (!(arg instanceof String))
                        throw new TypeError("attribute co_name should be a str");

                    return meta.get((String) arg);
                }));
        meta.set("__getattribute__", PyFunction.dynamic(PyCode.builder()
                        .argCount(1).posOnlyArgCount(0).kwOnlyArgCount(0).nLocals(1)
                        .stackSize(1).flags(0).varNames("key")
                        .filename("_builtins.class").name("__getattribute__").firstLineNo(1).build(),
                (args, kwargs) -> {
                    Object arg = args[0];
                    if (!(arg instanceof String))
                        throw new TypeError("attribute co_name should be a str");

                    if (!meta.has((String) arg)) {
                        PyObject attr = (PyObject) meta.get("__getattr__");
                        if (attr == null) throw new AttributeError((String) arg, type);
                        return attr.__call__(args, kwargs);
                    }
                    return meta.get((String) arg);
                }));
        meta.set("__setattr__", PyFunction.dynamic(PyCode.builder()
                        .argCount(2).posOnlyArgCount(0).kwOnlyArgCount(0).nLocals(2)
                        .stackSize(2).flags(0).varNames("key", "value")
                        .filename("_builtins.class").name("__setattr__").firstLineNo(1).build(),
                (args, _) -> {
                    Object arg = args[0];
                    if (!(arg instanceof String))
                        throw new TypeError("attribute co_name should be a str");

                    meta.set((String) arg, args[1]);
                    return null;
                }));
        meta.set("__delattr__", PyFunction.dynamic(PyCode.builder()
                        .argCount(1).posOnlyArgCount(0).kwOnlyArgCount(0).nLocals(1)
                        .stackSize(1).flags(0).varNames("key")
                        .filename("_builtins.class").name("__delattr__").firstLineNo(1).build(),
                (args, _) -> {
                    Object arg = args[0];
                    if (!(arg instanceof String))
                        throw new TypeError("attribute co_name should be a str");

                    meta.del((String) arg);
                    return null;
                }));
        meta.set("__init__", PyFunction.dynamic(PyCode.builder()
                        .argCount(0).posOnlyArgCount(0).kwOnlyArgCount(0).nLocals(2)
                        .stackSize(2).flags(PyCode.CO_VARARGS | PyCode.CO_VARKEYWORDS).varNames("args", "kwargs")
                        .filename("_builtins.class").name("__init__").firstLineNo(1).build(),
                (args, _) -> {
                    ClassUtils.initialize(args[0]);
                    return null;
                }));

        return type;
    }
}
