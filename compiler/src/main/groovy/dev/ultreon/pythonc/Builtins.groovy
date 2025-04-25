package dev.ultreon.pythonc;

import dev.ultreon.pythonc.classes.PyBuiltinClass;
import dev.ultreon.pythonc.functions.PyBuiltinFunction;
import dev.ultreon.pythonc.modules.PyBuiltinModule;
import org.objectweb.asm.Type

class Builtins {
    private final Map<String, PyBuiltinClass> builtinClassMap = new HashMap<>();
    private final Map<String, PyBuiltinFunction> builtinFunctionMap = new HashMap<>();

    private final Map<Type, PyBuiltinClass> builtinClassTypeMap = new HashMap<>();
    private final PythonCompiler compiler;
    private final PyBuiltinModule builtins = new PyBuiltinModule(new ModulePath("builtins"));
    private final PyBuiltinModule typing = new PyBuiltinModule(new ModulePath("typing"));

    Builtins(PythonCompiler compiler) {
        this.compiler = compiler;
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyInt;"), Type.LONG_TYPE, "int", "long"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyFloat;"), Type.DOUBLE_TYPE, "float", "double"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyBool;"), Type.BOOLEAN_TYPE, "bool", "boolean"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyStr;"), Type.getType(String.class), "str", "String"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyBytes;"), Type.getType(byte[].class), "bytes", "byte[]"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyByteArray;"), Type.getType(byte[].class), "bytearray", "byte[]"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyList;"), Type.getType(List.class), "list", "List"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyDict;"), Type.getType(Map.class), "dict", "Map"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PySet;"), Type.getType(Set.class), "set", "Set"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyTuple;"), Type.getType(Object[].class), "Tuple", "Object[]"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyRange;"), "range", "PyRange"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyJvmObject;"), Type.getType(Object.class), "jvmobject", "Object"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyType;"), Type.getType(Class.class), "type", "Class"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyException;"), "Exception", "RuntimeException"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyBaseException;"), "BaseException", "RuntimeException"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyStopIteration;"), "StopIteration", "RuntimeException"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyStopAsyncIteration;"), "StopAsyncIteration", "RuntimeException"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyGeneratorExit;"), "GeneratorExit", "RuntimeException"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PySystemExit;"), "SystemExit", "RuntimeException"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyKeyboardInterrupt;"), "KeyboardInterrupt", "RuntimeException"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyImportError;"), "ImportError", "ClassNotFoundException"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyModuleNotFoundError;"), "ModuleNotFoundError", "ClassNotFoundException"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyIndexError;"), "IndexError", "RuntimeException"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyKeyError;"), "KeyError", "RuntimeException"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyTypeError;"), "TypeError", "RuntimeException"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyNotImplementedError;"), "NotImplementedError", "RuntimeException"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyOverflowError;"), "OverflowError", "StackOverflowError"));

        put(new PyBuiltinFunction("asc", builtins));
        put(new PyBuiltinFunction("ord", builtins));
        put(new PyBuiltinFunction("input", builtins));
        put(new PyBuiltinFunction("print", builtins));
        put(new PyBuiltinFunction("len", builtins));
        put(new PyBuiltinFunction("hash", builtins));
        put(new PyBuiltinFunction("type", builtins));
        put(new PyBuiltinFunction("isinstance", builtins));
        put(new PyBuiltinFunction("issubclass", builtins));
        put(new PyBuiltinFunction("iter", builtins));
        put(new PyBuiltinFunction("next", builtins));
        put(new PyBuiltinFunction("range", builtins));
        put(new PyBuiltinFunction("min", builtins));
        put(new PyBuiltinFunction("max", builtins));
        put(new PyBuiltinFunction("sum", builtins));
        put(new PyBuiltinFunction("abs", builtins));
        put(new PyBuiltinFunction("all", builtins));
        put(new PyBuiltinFunction("any", builtins));
        put(new PyBuiltinFunction("enumerate", builtins));
        put(new PyBuiltinFunction("filter", builtins));
        put(new PyBuiltinFunction("map", builtins));
        put(new PyBuiltinFunction("reversed", builtins));
        put(new PyBuiltinFunction("sorted", builtins));
        put(new PyBuiltinFunction("zip", builtins));
        put(new PyBuiltinFunction("pow", builtins));
        put(new PyBuiltinFunction("round", builtins));
        put(new PyBuiltinFunction("bin", builtins));
        put(new PyBuiltinFunction("oct", builtins));
        put(new PyBuiltinFunction("hex", builtins));
        put(new PyBuiltinFunction("bool", builtins));
        put(new PyBuiltinFunction("dir", builtins));
        put(new PyBuiltinFunction("getattr", builtins));
        put(new PyBuiltinFunction("setattr", builtins));
        put(new PyBuiltinFunction("delattr", builtins));
        put(new PyBuiltinFunction("open", builtins));
        put(new PyBuiltinFunction("compile", builtins));
        put(new PyBuiltinFunction("exec", builtins));
        put(new PyBuiltinFunction("locals", builtins));
        put(new PyBuiltinFunction("globals", builtins));
        put(new PyBuiltinFunction("vars", builtins));
        put(new PyBuiltinFunction("callable", builtins));
        put(new PyBuiltinFunction("classmethod", builtins));
        put(new PyBuiltinFunction("staticmethod", builtins));
        put(new PyBuiltinFunction("super", builtins)); // Hmm
        put(new PyBuiltinFunction("property", builtins));
        put(new PyBuiltinFunction("object", builtins));
    }

    private put(PyBuiltinClass builtinClass) {
        builtinClassMap.put(builtinClass.pyName, builtinClass);
        builtinClassTypeMap.put(builtinClass.type, builtinClass);
    }

    private put(PyBuiltinFunction builtinFunction) {
        builtinFunctionMap.put(builtinFunction.name, builtinFunction);
    }

    PyBuiltinClass getClass(String name) {
        return builtinClassMap.get(name);
    }

    PyBuiltinClass getClass(Type type) {
        return builtinClassTypeMap.get(type);
    }

    PyBuiltinFunction getFunction(String name) {
        return builtinFunctionMap.get(name);
    }

    Collection<PyBuiltinClass> getClasses() {
        return builtinClassMap.values();
    }

    Collection<PyBuiltinFunction> getFunctions() {
        return builtinFunctionMap.values();
    }
}
