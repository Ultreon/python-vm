package dev.ultreon.pythonc;

import dev.ultreon.pythonc.classes.PyBuiltinClass;
import dev.ultreon.pythonc.functions.PyBuiltinFunction;
import dev.ultreon.pythonc.modules.PyBuiltinModule;
import org.objectweb.asm.Type;

import java.util.*;

public class Builtins {
    private final Map<String, PyBuiltinClass> builtinClassMap = new HashMap<>();
    private final Map<String, PyBuiltinFunction> builtinFunctionMap = new HashMap<>();

    private final Map<Type, PyBuiltinClass> builtinClassTypeMap = new HashMap<>();
    private final PythonCompiler compiler;
    private final PyBuiltinModule module = new PyBuiltinModule(new ModulePath("builtins"));

    public Builtins(PythonCompiler compiler) {
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
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyRange"), "range", "PyRange"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyJvmObject"), Type.getType(Object.class), "jvmobject", "Object"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyType"), Type.getType(Class.class), "type", "Class"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyException"), "Exception", "RuntimeException"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyBaseException"), "BaseException", "RuntimeException"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyStopIteration"), "StopIteration", "RuntimeException"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyStopAsyncIteration"), "StopAsyncIteration", "RuntimeException"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyGeneratorExit"), "GeneratorExit", "RuntimeException"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PySystemExit"), "SystemExit", "RuntimeException"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyKeyboardInterrupt"), "KeyboardInterrupt", "RuntimeException"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyImportError"), "ImportError", "ClassNotFoundException"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyModuleNotFoundError"), "ModuleNotFoundError", "ClassNotFoundException"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyIndexError"), "IndexError", "RuntimeException"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyKeyError"), "KeyError", "RuntimeException"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyTypeError"), "TypeError", "RuntimeException"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyNotImplementedError"), "NotImplementedError", "RuntimeException"));
        put(new PyBuiltinClass(Type.getType("Lorg/python/builtins/PyOverflowError"), "OverflowError", "StackOverflowError"));

        put(new PyBuiltinFunction("asc", module));
        put(new PyBuiltinFunction("ord", module));
        put(new PyBuiltinFunction("input", module));
        put(new PyBuiltinFunction("print", module));
        put(new PyBuiltinFunction("len", module));
        put(new PyBuiltinFunction("hash", module));
        put(new PyBuiltinFunction("type", module));
    }

    private void put(PyBuiltinClass builtinClass) {
        builtinClassMap.put(builtinClass.pyName, builtinClass);
        builtinClassTypeMap.put(builtinClass.type(), builtinClass);
    }

    private void put(PyBuiltinFunction builtinFunction) {
        builtinFunctionMap.put(builtinFunction.name(), builtinFunction);
    }

    public PyBuiltinClass getClass(String name) {
        return builtinClassMap.get(name);
    }

    public PyBuiltinClass getClass(Type type) {
        return builtinClassTypeMap.get(type);
    }

    public PyBuiltinFunction getFunction(String name) {
        return builtinFunctionMap.get(name);
    }

    public Collection<PyBuiltinClass> getClasses() {
        return builtinClassMap.values();
    }

    public Collection<PyBuiltinFunction> getFunctions() {
        return builtinFunctionMap.values();
    }
}
