package org.python._internal;

import java.util.Map;
import java.util.Set;

public class Py {
    public static Object __getattr__(Object self, String name) {
        return ClassUtils.getAttribute(self, name);
    }

    public static Object __getattribute__(Object self, String name) {
        return ClassUtils.getAttribute(self, name);
    }

    public static void __setattr__(Object self, String name, Object value) {
        ClassUtils.setAttribute(self, name, value);
    }

    public static void __delattr__(Object self, String name) {
        ClassUtils.delAttribute(self, name);
    }

    public static Set<String> __dir__(Object self) {
        return ClassUtils.getDir(self);
    }

    public static Object __call__(Object self, Object[] args, Map<String, Object> kwargs) {
        return ClassUtils.call(self, args, kwargs);
    }

    public static Object __callmember__(Object self, String name, Object[] args, Map<String, Object> kwargs) {
        Object attribute = ClassUtils.getAttribute(self, name);
        return ClassUtils.call(attribute, args, kwargs);
    }

    public static Object __not__(Object self) {
        return ClassUtils.not(self);
    }

    public static Object __neg__(Object self) {
        return ClassUtils.neg(self);
    }

    public static Object __pos__(Object self) {
        return ClassUtils.pos(self);
    }

    public static Object __abs__(Object self) {
        return ClassUtils.abs(self);
    }

    public static Object __bool__(Object self) {
        return ClassUtils.getBool(self);
    }

    public static Object __doc__(Object[] args, Map<String, Object> kwargs) {
        return "";
    }

    public static Object __class__(Object[] args, Map<String, Object> kwargs) {
        return args[0].getClass();
    }

    public static Object __module__(Object[] args, Map<String, Object> kwargs) {
        return args[0].getClass().getPackage().getName();
    }

    public static Object __name__(Object[] args, Map<String, Object> kwargs) {
        return args[0].getClass().getSimpleName();
    }

    public static Object __qualname__(Object[] args, Map<String, Object> kwargs) {
        return args[0].getClass().getName();
    }

    public static Object __init__(Object[] args, Map<String, Object> kwargs) {
        return ClassUtils.initialize(args[0]);
    }

    public static Object __repr__(Object[] args, Map<String, Object> kwargs) {
        return ClassUtils.getRepr(args[0]);
    }

    public static Object __str__(Object[] args, Map<String, Object> kwargs) {
        return ClassUtils.stringify(args[0]);
    }

    public static Object __hash__(Object[] args, Map<String, Object> kwargs) {
        return ClassUtils.getHash(args[0]);
    }

    public static Object __abs__(Object[] args, Map<String, Object> kwargs) {
        return ClassUtils.getAbs(args[0]);
    }

    public static Object __bool__(Object[] args, Map<String, Object> kwargs) {
        return ClassUtils.getBool(args[0]);
    }

    public static Object __int__(Object[] args, Map<String, Object> kwargs) {
        return ClassUtils.getInt(args[0]);
    }

    public static Object __float__(Object[] args, Map<String, Object> kwargs) {
        return ClassUtils.getFloat(args[0]);
    }

    public static Object __add__(Object self, Object other) {
        return ClassUtils.add(self, other);
    }

    public static Object __sub__(Object self, Object other) {
        return ClassUtils.sub(self, other);
    }

    public static Object __mul__(Object self, Object other) {
        return ClassUtils.mul(self, other);
    }

    public static Object __div__(Object self, Object other) {
        return ClassUtils.div(self, other);
    }

    public static Object __floordiv__(Object self, Object other) {
        return ClassUtils.floordiv(self, other);
    }

    public static Object __truediv__(Object self, Object other) {
        return ClassUtils.truediv(self, other);
    }

    public static Object __mod__(Object self, Object other) {
        return ClassUtils.mod(self, other);
    }

    public static Object __pow__(Object self, Object other) {
        return ClassUtils.pow(self, other);
    }

    public static Object __lshift__(Object self, Object other) {
        return ClassUtils.lshift(self, other);
    }

    public static Object __rshift__(Object self, Object other) {
        return ClassUtils.rshift(self, other);
    }

    public static Object __and__(Object self, Object other) {
        return ClassUtils.and(self, other);
    }

    public static Object __or__(Object self, Object other) {
        return ClassUtils.or(self, other);
    }

    public static Object __xor__(Object self, Object other) {
        return ClassUtils.xor(self, other);
    }

    public static boolean __lt__(Object self, Object other) {
        return (boolean) ClassUtils.lt(self, other);
    }

    public static boolean __le__(Object self, Object other) {
        return (boolean) ClassUtils.le(self, other);
    }

    public static boolean __gt__(Object self, Object other) {
        return (boolean) ClassUtils.gt(self, other);
    }

    public static boolean __ge__(Object self, Object other) {
        return (boolean) ClassUtils.ge(self, other);
    }

    public static boolean __eq__(Object self, Object other) {
        return (boolean) ClassUtils.eq(self, other);
    }

    public static boolean __ne__(Object self, Object other) {
        return (boolean) ClassUtils.ne(self, other);
    }

    public static Object __iter__(Object self) {
        return ClassUtils.iter(self);
    }

    public static Object __next__(Object self) {
        return ClassUtils.next(self);
    }

    public static boolean __hasnext__(Object self) {
        return (boolean) ClassUtils.hasNext(self);
    }

    public static boolean __contains__(Object self, Object other) {
        return (boolean) ClassUtils.in(self, other);
    }
}
