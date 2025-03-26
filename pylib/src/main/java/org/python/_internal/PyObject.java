package org.python._internal;

import org.python.builtins.AttributeError;
import org.python.builtins.TypeError;

import java.lang.reflect.Field;
import java.util.Map;

public interface PyObject {
    default void __init__() {
        
    }

    default int __hash__() {
        return this.hashCode();
    }

    default String __str__() {
        return "<object " + getClass().getName() + " at 0x" + Integer.toHexString(System.identityHashCode(this)) + ">";
    }

    default String __repr__() {
        return __str__();
    }

    default String __format__(Object[] args) {
        return __str__();
    }

    default byte[] __bytes__() {
        return __str__().getBytes();
    }

    default boolean __bool__() {
        return true;
    }


    default long __int__() {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __int__()");
    }

    default double __float__() {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __float__()");
    }

    default boolean __nonzero__() {
        return true;
    }

    default Object __neg__() {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __neg__()");
    }

    default Object __pos__() {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __pos__()");
    }

    default Object __invert__() {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __invert__()");
    }

    default Object __abs__() {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __abs__()");
    }

    default Object __round__() {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __round__()");
    }

    default Object __floor__() {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __floor__()");
    }

    default Object __ceil__() {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __ceil__()");
    }

    default Object __trunc__() {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __trunc__()");
    }

    default Object __complex__() {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __complex__()");
    }

    default Object __oct__() {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __oct__()");
    }

    default Object __hex__() {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __hex__()");
    }

    default Object __index__() {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __index__()");
    }

    default Object __lshift__(Object other) {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __lshift__()");
    }

    default Object __rshift__(Object other) {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __rshift__()");
    }

    default Object __and__(Object other) {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __and__()");
    }

    default Object __or__(Object other) {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __or__()");
    }

    default Object __xor__(Object other) {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __xor__()");
    }

    default Object __pow__(Object other) {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __pow__()");
    }

    default Object __mod__(Object other) {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __mod__()");
    }

    default Object __divmod__(Object other) {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __divmod__()");
    }

    default Object __rdivmod__(Object other) {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __rdivmod__()");
    }

    default int __len__() throws TypeError {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no len()");
    }

    default Object __iter__() throws TypeError {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __iter__()");
    }

    default Object __next__() throws TypeError {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __next__()");
    }

    default Object __getitem__(Object key) throws TypeError {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __getitem__()");
    }

    default Object __setitem__(Object key, Object value) throws TypeError {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __setitem__()");
    }

    default Object __delitem__(Object key) throws TypeError {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __delitem__()");
    }

    @SuppressWarnings("unchecked")
    default Object __getattr__(String name) throws AttributeError {
        Object value;
        try {
            try {
                return ClassUtils.getAttribute(this, name);
            } catch (AttributeError e) {
                Class<? extends PyObject> aClass = getClass();
                Field field = aClass.getField("__dict__");
                field.setAccessible(true);
                value = ((java.util.Map<String, Object>) field.get(this)).get(name);
            }
        } catch (Exception ex) {
            throw new RuntimeError("Failed to set attribute '" + name + "'", ex);
        }

        if (value == null) {
            throw new AttributeError(name);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    default void __setattr__(String name, Object value) {
        ClassUtils.setAttribute(this, name, value);
    }

    @SuppressWarnings("unchecked")
    default void __delattr__(String name) throws AttributeError {
        Object value = null;
        try {
            try {
                ClassUtils.delAttribute(this, name);
            } catch (AttributeError e) {
                Class<? extends PyObject> aClass = getClass();
                Field field = aClass.getField("__dict__");
                field.setAccessible(true);
                value = ((Map<String, Object>) field.get(this)).remove(name);
            }
        } catch (Exception ex) {
            throw new RuntimeError("Failed to delete attribute '" + name + "'", ex);
        }

        if (value == null) {
            throw new AttributeError(name);
        }
    }

    default void __setstate__(Object state) {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __setstate__()");
    }

    default Object __getstate__() {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __getstate__()");
    }

    default Object __reduce__() {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __reduce__()");
    }

    default Object __reduce_ex__() {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __reduce_ex__()");
    }

    default Object __getnewargs__() {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __getnewargs__()");
    }

    default Object __getinitargs__() {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __getinitargs__()");
    }

    default Map<String, Object> __getinitkwargs__() {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __getinitkwargs__()");
    }

    default boolean __contains__(Object key) {
        return false;
    }

    default Object __call__(Object[] args, Map<String, Object> kwargs) {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' is not callable");
    }

    default boolean __lt__(Object o) {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' isn't orderable");
    }

    default boolean __le__(Object o) {
        return __lt__(o) || __eq__(o);
    }

    default boolean __gt__(Object o) {
        return !__le__(o);
    }

    default boolean __ge__(Object o) {
        return !__lt__(o);
    }

    default boolean __eq__(Object o) {
        return this == o;
    }

    default boolean __ne__(Object o) {
        return !__eq__(o);
    }

    default Object __dir__() {
        return ClassUtils.getDir(this);
    }

    default Object __add__(Object other) {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __add__()");
    }

    default Object __sub__(Object other) {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __sub__()");
    }

    default Object __mul__(Object other) {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __mul__()");
    }

    default Object __truediv__(Object other) {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __truediv__()");
    }

    default Object __floordiv__(Object other) {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __floordiv__()");
    }

    default Object __is__(Object other) {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __is__()");
    }

    default Object __not__() {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __not__()");
    }

    default Object __div__(Object other) {
        throw new TypeError("object of type '" + getClass().getSimpleName() + "' has no __div__()");
    }
}
