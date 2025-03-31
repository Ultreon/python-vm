package org.python._internal;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("t")
public class DynamicCalls {

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String attrName, MethodType type, String name, String attrDesc) {
        Class<?> aClass = lookup.lookupClass();

        if (name.equals("__builtincall__")) {
            try {
                return new ConstantCallSite(MethodHandles.lookup().findStatic(PyBuiltins.class, attrName, type));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new PythonVMBug(e);
            }
        } else {
            throw new PythonVMBug("Unknown bootstrap: " + name);
        }
    }

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String attrName, MethodType type, String name) {
        Class<?> aClass = lookup.lookupClass();

        if (name.equals("__getattr__")) {
            try {
                return new ConstantCallSite(MethodHandles.insertArguments(MethodHandles.lookup().findStatic(Py.class, "__getattr__", MethodType.methodType(Object.class, Object.class, String.class)), 1, attrName));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new PythonVMBug(e);
            }
        } else if (name.equals("__getattribute__")) {
            try {
                return new ConstantCallSite(MethodHandles.insertArguments(MethodHandles.lookup().findStatic(Py.class, "__getattribute__", MethodType.methodType(Object.class, Object.class, String.class)), 1, attrName));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new PythonVMBug(e);
            }
        } else if (name.equals("__setattr__")) {
            try {
                return new ConstantCallSite(MethodHandles.insertArguments(MethodHandles.lookup().findStatic(Py.class, "__setattr__", MethodType.methodType(void.class, Object.class, String.class, Object.class)), 1, attrName));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new PythonVMBug(e);
            }
        } else if (name.equals("__delattr__")) {
            try {
                return new ConstantCallSite(MethodHandles.insertArguments(MethodHandles.lookup().findStatic(Py.class, "__delattr__", MethodType.methodType(void.class, Object.class, String.class)), 1, attrName));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new PythonVMBug(e);
            }
        } else if (name.equals("__dir__")) {
            try {
                return new ConstantCallSite(MethodHandles.insertArguments(MethodHandles.lookup().findStatic(Py.class, "__dir__", MethodType.methodType(Set.class, Object.class)), 1, attrName));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new PythonVMBug(e);
            }
        } else if (name.equals("__call__")) {
            if (attrName.equals("__call__")) {
                try {
                    return new ConstantCallSite(MethodHandles.lookup().findStatic(Py.class, "__call__", MethodType.methodType(Object.class, Object.class, Object[].class, Map.class)));
                } catch (NoSuchMethodException | IllegalAccessException e) {
                    throw new PythonVMBug(e);
                }
            }
            try {
                return new ConstantCallSite(MethodHandles.insertArguments(MethodHandles.lookup().findStatic(Py.class, "__callmember__", MethodType.methodType(Object.class, Object.class, String.class, Object[].class, Map.class)), 1, attrName));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new PythonVMBug(e);
            }
        } if (name.equals("__builtincall__")) {
            try {
                return new ConstantCallSite(MethodHandles.lookup().findStatic(PyBuiltins.class, attrName, type));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new PythonVMBug(e);
            }
        } else {
            throw new PythonVMBug("Unknown bootstrap: " + name);
        }
    }

    public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type) {
        Class<?> aClass = lookup.lookupClass();

        if (name.equals("__add__")) {
            try {
                return new ConstantCallSite(MethodHandles.lookup().findStatic(Py.class, "__add__", MethodType.methodType(Object.class, Object.class, Object.class)));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new PythonVMBug(e);
            }
        } else if (name.equals("__sub__")) {
            try {
                return new ConstantCallSite(MethodHandles.lookup().findStatic(Py.class, "__sub__", MethodType.methodType(Object.class, Object.class, Object.class)));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new PythonVMBug(e);
            }
        } else if (name.equals("__mul__")) {
            try {
                return new ConstantCallSite(MethodHandles.lookup().findStatic(Py.class, "__mul__", MethodType.methodType(Object.class, Object.class, Object.class)));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new PythonVMBug(e);
            }
        } else if (name.equals("__mod__")) {
            try {
                return new ConstantCallSite(MethodHandles.lookup().findStatic(Py.class, "__mod__", MethodType.methodType(Object.class, Object.class, Object.class)));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new PythonVMBug(e);
            }
        } else if (name.equals("__pow__")) {
            try {
                return new ConstantCallSite(MethodHandles.lookup().findStatic(Py.class, "__pow__", MethodType.methodType(Object.class, Object.class, Object.class)));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new PythonVMBug(e);
            }
        } else if (name.equals("__truediv__")) {
            try {
                return new ConstantCallSite(MethodHandles.lookup().findStatic(Py.class, "__truediv__", MethodType.methodType(Object.class, Object.class, Object.class)));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new PythonVMBug(e);
            }
        } else if (name.equals("__floordiv__")) {
            try {
                return new ConstantCallSite(MethodHandles.lookup().findStatic(Py.class, "__floordiv__", MethodType.methodType(Object.class, Object.class, Object.class)));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new PythonVMBug(e);
            }
        } else if (name.equals("__div__")) {
            try {
                return new ConstantCallSite(MethodHandles.lookup().findStatic(Py.class, "__div__", MethodType.methodType(Object.class, Object.class, Object.class)));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new PythonVMBug(e);
            }
        } else if (name.equals("__lshift__")) {
            try {
                return new ConstantCallSite(MethodHandles.lookup().findStatic(Py.class, "__lshift__", MethodType.methodType(Object.class, Object.class, Object.class)));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new PythonVMBug(e);
            }
        } else if (name.equals("__rshift__")) {
            try {
                return new ConstantCallSite(MethodHandles.lookup().findStatic(Py.class, "__rshift__", MethodType.methodType(Object.class, Object.class, Object.class)));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new PythonVMBug(e);
            }
        } else if (name.equals("__and__")) {
            try {
                return new ConstantCallSite(MethodHandles.lookup().findStatic(Py.class, "__and__", MethodType.methodType(Object.class, Object.class, Object.class)));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new PythonVMBug(e);
            }
        } else if (name.equals("__or__")) {
            try {
                return new ConstantCallSite(MethodHandles.lookup().findStatic(Py.class, "__or__", MethodType.methodType(Object.class, Object.class, Object.class)));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new PythonVMBug(e);
            }
        } else if (name.equals("__xor__")) {
            try {
                return new ConstantCallSite(MethodHandles.lookup().findStatic(Py.class, "__xor__", MethodType.methodType(Object.class, Object.class, Object.class)));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new PythonVMBug(e);
            }
        } else if (name.equals("__lt__")) {
            try {
                return new ConstantCallSite(MethodHandles.lookup().findStatic(Py.class, "__lt__", MethodType.methodType(boolean.class, Object.class, Object.class)));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new PythonVMBug(e);
            }
        } else if (name.equals("__le__")) {
            try {
                return new ConstantCallSite(MethodHandles.lookup().findStatic(Py.class, "__le__", MethodType.methodType(boolean.class, Object.class, Object.class)));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new PythonVMBug(e);
            }
        } else if (name.equals("__eq__")) {
            try {
                return new ConstantCallSite(MethodHandles.lookup().findStatic(Py.class, "__eq__", MethodType.methodType(boolean.class, Object.class, Object.class)));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new PythonVMBug(e);
            }
        } else if (name.equals("__ne__")) {
            try {
                return new ConstantCallSite(MethodHandles.lookup().findStatic(Py.class, "__ne__", MethodType.methodType(boolean.class, Object.class, Object.class)));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new PythonVMBug(e);
            }
        } else if (name.equals("__gt__")) {
            try {
                return new ConstantCallSite(MethodHandles.lookup().findStatic(Py.class, "__gt__", MethodType.methodType(boolean.class, Object.class, Object.class)));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new PythonVMBug(e);
            }
        } else if (name.equals("__ge__")) {
            try {
                return new ConstantCallSite(MethodHandles.lookup().findStatic(Py.class, "__ge__", MethodType.methodType(boolean.class, Object.class, Object.class)));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new PythonVMBug(e);
            }
        } else if (name.equals("__call__")) {
            try {
                return new ConstantCallSite(MethodHandles.lookup().findStatic(Py.class, "__call__", MethodType.methodType(Object.class, Object.class, Object[].class, Map.class)));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new PythonVMBug(e);
            }
        } else if (name.equals("__not__")) {
            try {
                return new ConstantCallSite(MethodHandles.lookup().findStatic(Py.class, "__not__", MethodType.methodType(Object.class, Object.class)));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new PythonVMBug(e);
            }
        } else if (name.equals("__iter__")) {
            try {
                return new ConstantCallSite(MethodHandles.lookup().findStatic(Py.class, "__iter__", MethodType.methodType(Object.class, Object.class)));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new PythonVMBug(e);
            }
        } else if (name.equals("__next__")) {
            try {
                return new ConstantCallSite(MethodHandles.lookup().findStatic(Py.class, "__next__", MethodType.methodType(Object.class, Object.class)));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new PythonVMBug(e);
            }
        } else if (name.equals("__hasnext__")) {
            try {
                return new ConstantCallSite(MethodHandles.lookup().findStatic(Py.class, "__hasnext__", MethodType.methodType(boolean.class, Object.class)));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new PythonVMBug(e);
            }
        } else {
            throw new PythonVMBug("Unknown bootstrap: " + name);
        }
    }
}
