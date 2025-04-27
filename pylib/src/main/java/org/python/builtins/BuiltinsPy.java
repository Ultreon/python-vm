//package org.python.builtins;
//
//import org.python._internal.ClassUtils;
//import org.python._internal.PyObject;
//import org.python._internal.PythonException;
//
//import java.lang.reflect.Array;
//import java.lang.reflect.InvocationTargetException;
//import java.lang.reflect.Method;
//import java.util.*;
//import java.util.stream.Collectors;
//
//public class BuiltinsPy {
//    public static int asc(String text) {
//        if (text.length() > 1) throw new RuntimeException("Provided text is too long, requires 1");
//        if (text.isEmpty()) throw new RuntimeException("Provided text is too short, requires 1");
//        return text.charAt(0);
//    }
//
//    public static int ord(String text) {
//        if (text.length() > 1) throw new TypeException("Provided text is too long, requires 1");
//        if (text.isEmpty()) throw new TypeException("Provided text is too short, requires 1");
//        return text.charAt(0);
//    }
//
//    public static String chr(int i) {
//        return String.valueOf((char)i);
//    }
//
//    public static String hex(int i) {
//        return Integer.toHexString(i);
//    }
//
//    public static String oct(int i) {
//        return Integer.toOctalString(i);
//    }
//
//    public static String bin(int i) {
//        return Integer.toBinaryString(i);
//    }
//
//    public static String repr(Object o) {
//        if (o instanceof PyObject) {
//            PyObject pyObject = (PyObject) o;
//            return pyObject.__repr__();
//        }
//        return o.toString();
//    }
//
//    public static String str(Object o) {
//        if (o instanceof PyObject) {
//            PyObject pyObject = (PyObject) o;
//            return pyObject.__str__();
//        }
//        return o.toString();
//    }
//
//    public static String format(Object o, Object... args) {
//        if (o instanceof PyObject) {
//            PyObject pyObject = (PyObject) o;
//            return pyObject.__format__(args);
//        }
//        return o.toString();
//    }
//
//    public static byte[] bytes(Object o) {
//        if (o instanceof PyObject) {
//            PyObject pyObject = (PyObject) o;
//            return pyObject.__bytes__();
//        }
//        return o.toString().getBytes();
//    }
//
//    public static boolean bool(Object o) {
//        if (o instanceof PyObject) {
//            PyObject pyObject = (PyObject) o;
//            return pyObject.__bool__();
//        }
//
//        return o != null;
//    }
//
//    public static int len(Object o) {
//        if (o instanceof PyObject) {
//            PyObject pyObject = (PyObject) o;
//            return pyObject.__len__();
//        }
//        if (o == null) return 0;
//        if (o instanceof String) return ((String) o).length();
//        if (o.getClass().isArray()) return Array.getLength(o);
//        if (o instanceof Collection<?>) return ((Collection<?>) o).size();
//
//        try {
//            return ClassUtils.getLength(o);
//        } catch (PythonException e) {
//            throw e;
//        } catch (Exception e) {
//            throw new TypeException("could not get length");
//        }
//    }
//
//    public static int hash(Object o) {
//        if (o instanceof PyObject) {
//            PyObject pyObject = (PyObject) o;
//            return pyObject.__hash__();
//        }
//        return o.hashCode();
//    }
//
//    public static Object getattr(Object o, String co_name) {
//        if (o instanceof PyObject) {
//            PyObject pyObject = (PyObject) o;
//            return pyObject.__getattr__(co_name);
//        }
//
//        try {
//            return ClassUtils.getAttribute(o, co_name);
//        } catch (AttributeException e) {
//            throw e;
//        } catch (Exception e) {
//            throw new TypeException("could not get attribute '" + co_name + "'");
//        }
//    }
//
//    public static void setattr(Object o, String co_name, Object value) {
//        if (o instanceof PyObject) {
//            PyObject pyObject = (PyObject) o;
//            pyObject.__setattr__(co_name, value);
//            return;
//        }
//        try {
//            ClassUtils.setAttribute(o, co_name, value);
//        } catch (AttributeException e) {
//            throw e;
//        } catch (Exception e) {
//            throw new TypeException("could not set attribute '" + co_name + "'");
//        }
//    }
//
//    public static void delattr(Object o, String co_name) {
//        if (o instanceof PyObject) {
//            PyObject pyObject = (PyObject) o;
//            pyObject.__delattr__(co_name);
//            return;
//        }
//        try {
//            ClassUtils.delAttribute(o, co_name);
//        } catch (AttributeException e) {
//            throw e;
//        } catch (Exception e) {
//            throw new TypeException("could not delete attribute '" + co_name + "'");
//        }
//    }
//
//    public static Object call(Object o, Object[] args, Map<String, Object> kwargs) {
//        if (o instanceof PyObject) {
//            PyObject pyObject = (PyObject) o;
//            return pyObject.__call__(args, kwargs);
//        }
//
//        Class<?> aClass = o.getClass();
//        if (aClass.isInterface() && aClass.isAnnotationPresent(FunctionalInterface.class)) {
//            for (Method method : aClass.getMethods()) {
//                if (!method.isDefault()) {
//                    try {
//                        return method.invoke(null, args);
//                    } catch (IllegalAccessException e) {
//                        throw new TypeException("could not access java function '" + method.getName() + "'");
//                    } catch (InvocationTargetException e) {
//                        throw new TypeException("could not call java function '" + method.getName() + "'");
//                    }
//                }
//            }
//        }
//        throw new TypeException("object of type '" + o.getClass().getSimpleName() + "' is not callable");
//    }
//
//    public static Object getitem(Object o, Object key) {
//        if (o instanceof PyObject) {
//            PyObject pyObject = (PyObject) o;
//            return pyObject.__getitem__(key);
//        }
//        if (o instanceof String) return String.valueOf(((String) o).charAt((int) key));
//        if (o.getClass().isArray()) return Array.get(o, (int) key);
//        if (o instanceof List<?>) {
//            List<?> list = (List<?>) o;
//            return list.get((int) key);
//        }
//        if (o instanceof Map<?, ?>) {
//            Map<?, ?> map = (Map<?, ?>) o;
//            return map.get(key);
//        }
//
//        throw new TypeException("object of type '" + o.getClass().getSimpleName() + "' has no __getitem__()");
//    }
//
//    @SuppressWarnings("unchecked")
//    public static void setitem(Object o, Object key, Object value) {
//        if (o instanceof PyObject) {
//            PyObject pyObject = (PyObject) o;
//            pyObject.__setitem__(key, value);
//            return;
//        }
//        if (o.getClass().isArray()) {
//            Array.set(o, (int) key, value);
//            return;
//        }
//        if (o instanceof List<?>) {
//            List list = (List) o;
//            list.set((int) key, value);
//            return;
//        }
//        if (o instanceof Map<?, ?>) {
//            Map map = (Map) o;
//            map.put(key, value);
//            return;
//        }
//        throw new TypeException("object of type '" + o.getClass().getSimpleName() + "' has no __setitem__()");
//    }
//
//    public static Object delitem(Object o, Object key) {
//        if (o instanceof PyObject) {
//            PyObject pyObject = (PyObject) o;
//            return pyObject.__delitem__(key);
//        }
//        if (o.getClass().isArray()) {
//            throw new TypeException("object of type '" + o.getClass().getSimpleName() + "' has no __delitem__()");
//        }
//        if (o instanceof List<?>) {
//            List list = (List) o;
//            return list.remove((int) key);
//        }
//        if (o instanceof Map<?, ?>) {
//            Map map = (Map) o;
//            return map.remove(key);
//        }
//
//        throw new TypeException("object of type '" + o.getClass().getSimpleName() + "' has no __delitem__()");
//    }
//
//    public static boolean contains(Object o, Object key) {
//        if (o instanceof PyObject) {
//            PyObject pyObject = (PyObject) o;
//            return pyObject.__contains__(key);
//        }
//        if (o.getClass().isArray()) {
//            for (int i = 0; i < Array.getLength(o); i++) {
//                if (Array.get(o, i).equals(key)) {
//                    return true;
//                }
//            }
//            return false;
//        }
//        if (o instanceof List<?>) {
//            List list = (List) o;
//            return list.contains(key);
//        }
//        if (o instanceof Map<?, ?>) {
//            Map map = (Map) o;
//            return map.containsKey(key);
//        }
//
//        throw new TypeException("object of type '" + o.getClass().getSimpleName() + "' has no __contains__()");
//    }
//
//    public static Object iter(Object o) {
//        if (o instanceof PyObject) {
//            PyObject pyObject = (PyObject) o;
//            return pyObject.__iter__();
//        }
//        if (o.getClass().isArray()) {
//            return new ArrayIterator(o);
//        }
//        if (o instanceof Collection<?>) {
//            return ((Collection<?>) o).iterator();
//        }
//        if (o instanceof Map<?, ?>) {
//            return ((Map<?, ?>) o).entrySet().iterator();
//        }
//        throw new TypeException("object of type '" + o.getClass().getSimpleName() + "' has no __iter__()");
//    }
//
//    public static Object next(Object o) {
//        if (o instanceof PyObject) {
//            PyObject pyObject = (PyObject) o;
//            return pyObject.__next__();
//        }
//
//        if (o instanceof Iterator<?>) {
//            return ((Iterator<?>) o).next();
//        }
//        throw new TypeException("object of type '" + o.getClass().getSimpleName() + "' has no next()");
//    }
//
//    public static long $int(Object o) {
//        if (o instanceof PyObject) {
//            PyObject pyObject = (PyObject) o;
//            return pyObject.__int__();
//        }
//        if (o instanceof Number) {
//            return ((Number) o).longValue();
//        }
//        throw new TypeException("object of type '" + o.getClass().getSimpleName() + "' has no __int__()");
//    }
//
//    private static final Scanner SCANNER = new Scanner(System.in);
//
//    public static String input(String prompt) {
//        System.out.print(prompt);
//        return SCANNER.nextLine();
//    }
//
//    public static String input() {
//        return SCANNER.nextLine();
//    }
//
//    public static void print(Object o) {
//        System.out.println(o);
//    }
//
//    public static void print() {
//        System.out.println();
//    }
//
//    public static void print(Object... o) {
//        System.out.println(Arrays.stream(o).map(o1 -> {
//            if (o1 == null) {
//                return "None";
//            }
//            return o1.toString();
//        }).collect(Collectors.joining(" ")));
//    }
//
//    public static void print(String s) {
//        System.out.println(s);
//    }
//}