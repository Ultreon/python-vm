package org.python.builtins;

import org.python._internal.ClassUtils;

import java.util.Map;

public class PyStr {
    public static String __init__() {
        return "";
    }

    public static String __init__(String s) {
        return s;
    }

    public static String __init__(Object o) {
        Object str = ClassUtils.getStr(o);
        if (str instanceof String) return (String) str;
        throw new TypeError("Expected str, got " + str.getClass().getSimpleName());
    }

    public static String __init__(Object[] args, Map<String, Object> kwargs) {
        return __init__(args.length == 1 ? args[0] : kwargs.get("s"));
    }
}
