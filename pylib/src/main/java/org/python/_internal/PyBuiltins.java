package org.python._internal;

import java.util.Collection;
import java.util.Map;
import java.util.Scanner;
import java.util.StringJoiner;

public class PyBuiltins {
    public static Object print(Object[] args, Map<String, Object> kwargs) {
        Object file = kwargs.getOrDefault("file", null);
        Object end = kwargs.getOrDefault("end", "\n");
        Object sep = kwargs.getOrDefault("sep", " ");
        if (file != null) throw new UnsupportedOperationException("Not implemented!");
        StringJoiner joiner = new StringJoiner((String) sep);
        String[] strings = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            strings[i] = ClassUtils.stringify(args[i]);
        }
        System.out.print(String.join(" ", strings) + end);
        return null;
    }

    private static final Scanner SCANNER = new Scanner(System.in);

    public static Object input(Object[] args, Map<String, Object> kwargs) {
        Object file = kwargs.getOrDefault("file", null);
        Object prompt = kwargs.getOrDefault("prompt", "? ");
        if (file != null) throw new UnsupportedOperationException("Not implemented!");
        System.out.print(prompt);
        return SCANNER.nextLine();
    }

    public static Object asc(Object[] args, Map<String, Object> kwargs) {
        Object text = args.length == 1 ? args[0] : kwargs.get("text");
        if (text instanceof String) {
            return ((String) text).charAt(0);
        }
        throw new UnsupportedOperationException("Not implemented!");
    }

    public static Object ord(Object[] args, Map<String, Object> kwargs) {
        Object text = args.length == 1 ? args[0] : kwargs.get("text");
        if (text instanceof String) {
            return ((String) text).charAt(0);
        }
        throw new UnsupportedOperationException("Not implemented!");
    }

    public static Object chr(Object[] args, Map<String, Object> kwargs) {
        Object i = args.length == 1 ? args[0] : kwargs.get("i");
        if (i instanceof Integer) {
            return String.valueOf((char) i);
        }
        throw new UnsupportedOperationException("Not implemented!");
    }

    public static Object hex(Object[] args, Map<String, Object> kwargs) {
        Object i = args.length == 1 ? args[0] : kwargs.get("i");
        if (i instanceof Integer) {
            return Integer.toHexString((int) i);
        }
        throw new UnsupportedOperationException("Not implemented!");
    }

    public static Object oct(Object[] args, Map<String, Object> kwargs) {
        Object i = args.length == 1 ? args[0] : kwargs.get("i");
        if (i instanceof Integer) {
            return Integer.toOctalString((int) i);
        }
        throw new UnsupportedOperationException("Not implemented!");
    }

    public static Object bin(Object[] args, Map<String, Object> kwargs) {
        Object i = args.length == 1 ? args[0] : kwargs.get("i");
        if (i instanceof Integer) {
            return Integer.toBinaryString((int) i);
        }
        throw new UnsupportedOperationException("Not implemented!");
    }

    public static Object len(Object[] args, Map<String, Object> kwargs) throws Exception {
        Object o = args.length == 1 ? args[0] : kwargs.get("o");
        return ClassUtils.getLength(o);
    }

    public static Object max(Object[] args, Map<String, Object> kwargs) {
        Object iterable = args.length == 1 ? args[0] : get(kwargs, "iterable");
        return ClassUtils.getMax((Collection<?>) iterable);
    }

    public static Object getattr(Object[] args, Map<String, Object> kwargs) {
        Object obj = args.length == 2 ? args[0] : get(kwargs, "obj");
        Object name = args.length == 2 ? args[1] : get(kwargs, "name");
        try {
            return ClassUtils.getAttribute(obj, (String) name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object setattr(Object[] args, Map<String, Object> kwargs) {
        Object obj = args.length == 3 ? args[0] : get(kwargs, "obj");
        Object name = args.length == 3 ? args[1] : get(kwargs, "name");
        Object value = args.length == 3 ? args[2] : get(kwargs, "value");
        try {
            ClassUtils.setAttribute(obj, (String) name, value);
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object delattr(Object[] args, Map<String, Object> kwargs) {
        Object obj = args.length == 2 ? args[0] : get(kwargs, "obj");
        Object name = args.length == 2 ? args[1] : get(kwargs, "name");
        try {
            ClassUtils.delAttribute(obj, (String) name);
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object hasattr(Object[] args, Map<String, Object> kwargs) throws Exception {
        Object obj = args.length == 2 ? args[0] : get(kwargs, "obj");
        Object name = args.length == 2 ? args[1] : get(kwargs, "name");
        return ClassUtils.hasAttribute(obj, (String) name);
    }

    public static Object hash(Object[] args, Map<String, Object> kwargs) throws Exception {
        Object o = args.length == 1 ? args[0] : get(kwargs, "o");
        return ClassUtils.getHash(o);
    }

    public static Object abs(Object[] args, Map<String, Object> kwargs) throws Exception {
        Object o = args.length == 1 ? args[0] : get(kwargs, "o");
        return ClassUtils.getAbs(o);
    }

    private static Object get(Map<String, Object> kwargs, String name) {
        if (!kwargs.containsKey(name)) {
            throw new IllegalArgumentException("Missing argument: " + name);
        }

        return kwargs.get(name);
    }
}
