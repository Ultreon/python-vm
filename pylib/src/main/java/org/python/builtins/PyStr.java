package org.python.builtins;

public class PyStr {
    public static String __init__() {
        return "";
    }

    public static String __init__(String s) {
        return s;
    }

    public static String __init__(Object o) {
        return o.toString();
    }
}
