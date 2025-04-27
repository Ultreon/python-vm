package org.python._internal;

import org.python.builtins.AttributeError;

import java.lang.reflect.Field;
import java.util.Map;

@SuppressWarnings("unused")
public interface PyObject {
    default void __init__(Object[] args, Map<String, Object> kwargs) {

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
        throw new PythonVMBug("__format__ isn't implemented");
    }

    default byte[] __bytes__() {
        return __str__().getBytes();
    }

    default boolean __bool__() {
        return true;
    }


    default long __int__() {
        throw new AttributeError("__int__", this);
    }

    default double __float__() {
        throw new AttributeError("__float__", this);
    }

    default boolean __nonzero__() {
        return true;
    }

    default Object __neg__() {
        throw new AttributeError("__neg__", this);
    }

    default Object __pos__() {
        throw new AttributeError("__pos__", this);
    }

    default Object __invert__() {
        throw new AttributeError("__invert__", this);
    }

    default Object __abs__() {
        throw new AttributeError("__abs__", this);
    }

    default Object __round__() {
        throw new AttributeError("__round__", this);
    }

    default Object __floor__() {
        throw new AttributeError("__floor__", this);
    }

    default Object __ceil__() {
        throw new AttributeError("__ceil__", this);
    }

    default Object __trunc__() {
        throw new AttributeError("__trunc__", this);
    }

    default Object __complex__() {
        throw new AttributeError("__complex__", this);
    }

    default Object __oct__() {
        throw new AttributeError("__oct__", this);
    }

    default Object __hex__() {
        throw new AttributeError("__hex__", this);
    }

    default Object __index__() {
        throw new AttributeError("__index__", this);
    }

    default Object __lshift__(Object other) {
        throw new AttributeError("__lshift__", this);
    }

    default Object __rshift__(Object other) {
        throw new AttributeError("__rshift__", this);
    }

    default Object __and__(Object other) {
        throw new AttributeError("__and__", this);
    }

    default Object __or__(Object other) {
        throw new AttributeError("__or__", this);
    }

    default Object __xor__(Object other) {
        throw new AttributeError("__xor__", this);
    }

    default Object __pow__(Object other) {
        throw new AttributeError("__pow__", this);
    }

    default Object __mod__(Object other) {
        throw new AttributeError("__mod__", this);
    }

    default Object __divmod__(Object other) {
        throw new AttributeError("__divmod__", this);
    }

    default Object __rdivmod__(Object other) {
        throw new AttributeError("__rdivmod__", this);
    }

    default int __len__() throws AttributeError {
        throw new AttributeError("object of type '" + getClass().getSimpleName() + "' has no len()");
    }

    default Object __iter__() throws AttributeError {
        throw new AttributeError("__iter__", this);
    }

    default Object __next__() throws AttributeError {
        throw new AttributeError("__next__", this);
    }

    default Object __getitem__(Object key) throws AttributeError {
        throw new AttributeError("__getitem__", this);
    }

    default Object __setitem__(Object key, Object value) throws AttributeError {
        throw new AttributeError("__setitem__", this);
    }

    default Object __delitem__(Object key) throws AttributeError {
        throw new AttributeError("__delitem__", this);
    }

    default Object __getattr__(String name) throws AttributeError {
        Object value;
        try {
            return ClassUtils.getAttribute(this, name);
        } catch (AttributeError e) {
            throw new AttributeError(name, this);
        } catch (Exception ex) {
            throw new RuntimeError("Failed to set attribute '" + name + "'", ex);
        }
    }

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
        throw new AttributeError("__setstate__", this);
    }

    default Object __getstate__() {
        throw new AttributeError("__getstate__", this);
    }

    default Object __reduce__() {
        throw new AttributeError("__reduce__", this);
    }

    default Object __reduce_ex__() {
        throw new AttributeError("__reduce_ex__", this);
    }

    default Object __getnewargs__() {
        throw new AttributeError("__getnewargs__", this);
    }

    default Object __getinitargs__() {
        throw new AttributeError("__getinitargs__", this);
    }

    default Map<String, Object> __getinitkwargs__() {
        throw new AttributeError("__getinitkwargs__", this);
    }

    default boolean __contains__(Object key) {
        return false;
    }

    default Object __call__(Object[] args, Map<String, Object> kwargs) {
        throw new AttributeError("object of type '" + getClass().getSimpleName() + "' is not callable");
    }

    default boolean __lt__(Object o) {
        throw new AttributeError("object of type '" + getClass().getSimpleName() + "' isn't orderable");
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
        throw new AttributeError("__add__", this);
    }

    default Object __sub__(Object other) {
        throw new AttributeError("__sub__", this);
    }

    default Object __mul__(Object other) {
        throw new AttributeError("__mul__", this);
    }

    default Object __truediv__(Object other) {
        throw new AttributeError("__truediv__", this);
    }

    default Object __floordiv__(Object other) {
        throw new AttributeError("__floordiv__", this);
    }

    default Object __is__(Object other) {
        throw new AttributeError("__is__", this);
    }

    default Object __not__() {
        throw new AttributeError("__not__", this);
    }

    default Object __div__(Object other) {
        throw new AttributeError("__div__", this);
    }

    default Object __enumerate__() {
        throw new AttributeError("__enumerate__", this);
    }
}
