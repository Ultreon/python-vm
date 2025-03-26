package dev.ultreon.pythonc;

import org.objectweb.asm.Type;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Builtins {
    private final Map<String, PyBuiltinClass> builtinClassMap = new HashMap<>();
    private final Map<String, PyBuiltinFunction> builtinFunctionMap = new HashMap<>();

    private final Map<Type, PyBuiltinClass> builtinClassTypeMap = new HashMap<>();
    private PythonCompiler compiler;

    public Builtins(PythonCompiler compiler) {
        this.compiler = compiler;
        put(new PyBuiltinClass("Ljava/lang/Long;", "J", "Lorg/python/builtins/PyInt;", "int"));
        put(new PyBuiltinClass("Ljava/lang/Double;", "D", "Lorg/python/builtins/PyFloat;", "float"));
        put(new PyBuiltinClass("Ljava/lang/Boolean;", "Z", "Lorg/python/builtins/PyBoolean;", "bool"));
        put(new PyBuiltinClass("Ljava/lang/String;", "Lorg/python/builtins/PyStr;", "str"));
        put(new PyBuiltinClass("[B", "[B", "Lorg/python/builtins/PyBytes;", "bytes"));
        put(new PyBuiltinClass("[B", "[B", "Lorg/python/builtins/PyByteArray;", "bytearray"));
        put(new PyBuiltinClass("Ljava/util/List;", "Lorg/python/builtins/PyList;", "list"));
        put(new PyBuiltinClass("Ljava/util/Map;", "Lorg/python/builtins/PyDict;", "dict"));
        put(new PyBuiltinClass("Ljava/util/Set;", "Lorg/python/builtins/PySet;", "set"));
        put(new PyBuiltinClass("[Ljava/util/Object;", "Lorg/python/builtins/PyTuple;", "tuple"));
        put(new PyBuiltinClass("Ljava/util/List;", "Lorg/python/builtins/PyRange;", "range"));
        put(new PyBuiltinClass("Ljava/lang/Object;", "Lorg/python/builtins/PyNone;", "None"));
        put(new PyBuiltinClass("Ljava/lang/Object;", "Lorg/python/builtins/PyObject;", "object"));
        put(new PyBuiltinClass("Ljava/lang/Class;", "Lorg/python/builtins/PyType;", "type"));
        put(new PyBuiltinClass("Ljava/lang/Exception;", "Lorg/python/builtins/PyException;", "Exception"));
        put(new PyBuiltinClass("Ljava/lang/RuntimeException;", "Lorg/python/builtins/PyBaseException;", "BaseException"));
        put(new PyBuiltinClass("Ljava/lang/RuntimeException;", "Lorg/python/builtins/PyStopIteration;", "StopIteration"));
        put(new PyBuiltinClass("Ljava/lang/RuntimeException;", "Lorg/python/builtins/PyStopAsyncIteration;", "StopAsyncIteration"));
        put(new PyBuiltinClass("Ljava/lang/RuntimeException;", "Lorg/python/builtins/PyGeneratorExit;", "GeneratorExit"));
        put(new PyBuiltinClass("Ljava/lang/RuntimeException;", "Lorg/python/builtins/PySystemExit;", "SystemExit"));
        put(new PyBuiltinClass("Ljava/lang/RuntimeException;", "Lorg/python/builtins/PyKeyboardInterrupt;", "KeyboardInterrupt"));
        put(new PyBuiltinClass("Ljava/lang/RuntimeException;", "Lorg/python/builtins/PyImportError;", "ImportError"));
        put(new PyBuiltinClass("Ljava/lang/RuntimeException;", "Lorg/python/builtins/PyModuleNotFoundError;", "ModuleNotFoundError"));
        put(new PyBuiltinClass("Ljava/lang/RuntimeException;", "Lorg/python/builtins/PyIndexError;", "IndexError"));
        put(new PyBuiltinClass("Ljava/lang/RuntimeException;", "Lorg/python/builtins/PyKeyError;", "KeyError"));
        put(new PyBuiltinClass("Ljava/lang/RuntimeException;", "Lorg/python/builtins/PyValueError;", "ValueError"));
        put(new PyBuiltinClass("Ljava/lang/RuntimeException;", "Lorg/python/builtins/PyTypeError;", "TypeError"));
        put(new PyBuiltinClass("Ljava/lang/UnsupportedOperationException;", "Lorg/python/builtins/PyNotImplementedError;", "NotImplementedError"));
        put(new PyBuiltinClass("Ljava/lang/RuntimeException;", "Lorg/python/builtins/PyOverflowError;", "OverflowError"));

        put(new PyBuiltinFunction("java/lang/String", "org/python/builtins/BuiltinsPy", new String[]{"asc(Ljava/lang/String;)V"}, 1, "asc", Type.LONG_TYPE));
        put(new PyBuiltinFunction("java/lang/System", "org/python/builtins/BuiltinsPy", new String[]{"print([Ljava/lang/Object;Ljava/util/Map;)"}, 2, "print", PyBuiltinFunction.Mode.DYN_CTOR, Type.VOID_TYPE));
    }

    private void put(PyBuiltinClass builtinClass) {
        builtinClassMap.put(builtinClass.pyName, builtinClass);
        builtinClassTypeMap.put(builtinClass.type(compiler), builtinClass);
    }

    private void put(PyBuiltinFunction builtinFunction) {
        builtinFunctionMap.put(builtinFunction.name, builtinFunction);
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
