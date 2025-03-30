//package org.python.builtins;
//
//public class PyInt {
//    public static long __init__() {
//        return 0;
//    }
//
//    public static long __init__(Object o) {
//        return BuiltinsPy.$int(o);
//    }
//
//    public static long __add__(long a, long b) {
//        return a + b;
//    }
//
//    public static long __sub__(long a, long b) {
//        return a - b;
//    }
//
//    public static long __mul__(long a, long b) {
//        return a * b;
//    }
//
//    public static long __div__(long a, long b) {
//        return a / b;
//    }
//
//    public static long __mod__(long a, long b) {
//        return a % b;
//    }
//
//    public static long __pow__(long a, long b) {
//        return (long) Math.pow(a, b);
//    }
//
//    public static long __neg__(long a) {
//        return -a;
//    }
//
//    public static long __pos__(long a) {
//        return +a;
//    }
//
//    public static long __abs__(long a) {
//        return Math.abs(a);
//    }
//
//    public static long __invert__(long a) {
//        return ~a;
//    }
//
//    public static long __lshift__(long a, long b) {
//        return a << b;
//    }
//
//    public static long __rshift__(long a, long b) {
//        return a >> b;
//    }
//
//    public static long __and__(long a, long b) {
//        return a & b;
//    }
//
//    public static long __or__(long a, long b) {
//        return a | b;
//    }
//
//    public static long __xor__(long a, long b) {
//        return a ^ b;
//    }
//
//    public static long __floordiv__(long a, long b) {
//        return Math.floorDiv(a, b);
//    }
//
//    public static long __truediv__(long a, long b) {
//        return a / b;
//    }
//
//    public static long __matmul__(long a, long b) {
//        return Math.multiplyExact(a, b);
//    }
//
//    public static long __rmatmul__(long a, long b) {
//        return Math.multiplyExact(b, a);
//    }
//
//    public static boolean __bool__(long a) {
//        return a != 0;
//    }
//
//    public static long __index__(long a) {
//        return a;
//    }
//
//    public static long __int__(long a) {
//        return a;
//    }
//
//    public static double __float__(long a) {
//        return (double) a;
//    }
//
//    public static long __complex__(long a) {
//        return a;
//    }
//
//    public static long __jlong__(long a) {
//        return a;
//    }
//
//    public static int __jint__(long a) {
//        return (int) a;
//    }
//
//    public static byte __jbyte__(long a) {
//        return (byte) a;
//    }
//
//    public static short __jshort__(long a) {
//        return (short) a;
//    }
//
//    public static char __jchar__(long a) {
//        return (char) a;
//    }
//
//    public static float __jfloat__(long a) {
//        return (float) a;
//    }
//
//    public static double __jdouble__(long a) {
//        return (double) a;
//    }
//
//    public static long __hash__(long a) {
//        return a;
//    }
//
//    public static boolean __eq__(long a, long b) {
//        return a == b;
//    }
//
//    public static boolean __ne__(long a, long b) {
//        return a != b;
//    }
//
//    public static boolean __lt__(long a, long b) {
//        return a < b;
//    }
//
//    public static boolean __le__(long a, long b) {
//        return a <= b;
//    }
//
//    public static boolean __gt__(long a, long b) {
//        return a > b;
//    }
//
//    public static boolean __ge__(long a, long b) {
//        return a >= b;
//    }
//
//    public static boolean __contains__(long a, long b) {
//        return a == b;
//    }
//
//    public static String __str__(long a) {
//        return String.valueOf(a);
//    }
//
//    public static String __repr__(long a) {
//        return String.valueOf(a);
//    }
//
//    public static byte[] __bytes__(long a) {
//        throw new IllegalStateException("not implemented");
//    }
//}
