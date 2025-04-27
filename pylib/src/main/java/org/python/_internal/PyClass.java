package org.python._internal;

public class PyClass implements PyObject {
    private final Class<?> javaClass;

    private PyClass(Class<?> javaClass) {
        this.javaClass = javaClass;
    }

    static PyClass create(Class<?> javaClass) {
        return new PyClass(javaClass);
    }
}
