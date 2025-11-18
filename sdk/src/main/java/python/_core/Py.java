package python._core;

import python.Builtins;
import python.Sys;
import python.builtins.*;
import python.types.ModuleType;
import python.types.Type;

import java.lang.Exception;
import java.lang.Object;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Py {
    public static Builtins builtins = Builtins.INSTANCE;

    public static Object call(Object owner, Tuple args) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public static Object call(Object owner, Dict kwargs) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public static Object call(Object owner, Tuple args, Dict kwargs) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public static VMObject eq(VMObject left, VMObject right) {
        return left.__eq__(right);
    }

    public static VMObject ne(VMObject left, VMObject right) {
        return left.__ne__(right);
    }

    public static VMObject hash(VMObject object) {
        return object.__hash__();
    }

    public static VMObject add(VMObject left, VMObject right) {
        return left.getAttr("__add__").call(new Tuple(right), new Dict());
    }

    public static VMObject sub(VMObject left, VMObject right) {
        return left.getAttr("__sub__").call(new Tuple(right), new Dict());
    }

    public static VMObject mul(VMObject left, VMObject right) {
        return left.getAttr("__mul__").call(new Tuple(right), new Dict());
    }

    public static VMObject div(VMObject left, VMObject right) {
        return left.getAttr("__div__").call(new Tuple(right), new Dict());
    }

    public static VMObject mod(VMObject left, VMObject right) {
        return left.getAttr("__mod__").call(new Tuple(right), new Dict());
    }

    public static VMObject pow(VMObject left, VMObject right) {
        return left.getAttr("__pow__").call(new Tuple(right), new Dict());
    }

    public static VMObject rshift(VMObject left, VMObject right) {
        return left.getAttr("__rshift__").call(new Tuple(right), new Dict());
    }

    public static VMObject lshift(VMObject left, VMObject right) {
        return left.getAttr("__lshift__").call(new Tuple(right), new Dict());
    }

    public static VMObject bool(VMObject object) {
        return object.getAttr("__bool__").call(new Tuple(), new Dict());
    }

    public static VMObject str(VMObject object) {
        return object.getAttr("__str__").call(new Tuple(), new Dict());
    }

    public static VMObject int_(VMObject object) {
        return object.getAttr("__int__").call(new Tuple(), new Dict());
    }

    public static VMObject float_(VMObject object) {
        return object.getAttr("__float__").call(new Tuple(), new Dict());
    }

    public static VMObject len(VMObject object) {
        return object.getAttr("__len__").call(new Tuple(), new Dict());
    }

    public static VMObject getitem(VMObject object, VMObject index) {
        return object.getAttr("__getitem__").call(new Tuple(index), new Dict());
    }

    public static VMObject setitem(VMObject object, VMObject index, VMObject value) {
        return object.getAttr("__setitem__").call(new Tuple(index, value), new Dict());
    }

    public static VMObject delitem(VMObject object, VMObject index) {
        return object.getAttr("__delitem__").call(new Tuple(index), new Dict());
    }

    public static VMObject iter(VMObject object) {
        return object.getAttr("__iter__").call(new Tuple(), new Dict());
    }

    public static VMObject next(VMObject object) {
        return object.getAttr("__next__").call(new Tuple(), new Dict());
    }

    public static PyJvmObject createObject(Object jvmObject) {
        return new PyJvmObject(jvmObject);
    }

    public static RuntimeException createRuntimeError(Throwable throwable) {
        try {
            Class<?> clazz = Class.forName("python.builtins.RuntimeError");
            return (RuntimeException) clazz.getConstructor(Throwable.class).newInstance(throwable);
        } catch (Exception e) {
            throw new PythonVMBug("Failed to create RuntimeError", e);
        }
    }

    public static RuntimeException createTypeError(String cannotCallAJavaObject) {
        try {
            Class<?> clazz = Class.forName("python.builtins.TypeError");
            return (RuntimeException) clazz.getConstructor(String.class).newInstance(cannotCallAJavaObject);
        } catch (Exception e) {
            throw new PythonVMBug("Failed to create TypeError", e);
        }
    }

    public static RuntimeException createAttributeError(String name) {
        try {
            Class<?> clazz = Class.forName("python.builtins.AttributeError");
            return (RuntimeException) clazz.getConstructor(String.class).newInstance(name);
        } catch (Exception e) {
            throw new PythonVMBug("Failed to create AttributeError", e);
        }
    }

    public static VMObject createDict(Map<String, VMObject> attributes) {
        try {
            return new Dict(attributes);
        } catch (Exception e) {
            throw new PythonVMBug("Failed to create Dict", e);
        }
    }

    public static VMObject createList(VMObject... elements) {
        try {
            return new python.builtins.List(new ArrayList<>(List.of(elements)));
        } catch (Exception e) {
            throw new PythonVMBug("Failed to create List", e);
        }
    }

    public static VMObject createList(List<VMObject> elements) {
        try {
            return new python.builtins.List(elements);
        } catch (Exception e) {
            throw new PythonVMBug("Failed to create List", e);
        }
    }

    public static Object createTuple(Object[] elements) {
        try {
            Class<?> clazz = Class.forName("python.builtins.Tuple");
            return clazz.getMethod("__jvm_init__", Object[].class).invoke(null, elements);
        } catch (Exception e) {
            throw new PythonVMBug("Failed to create Tuple", e);
        }
    }

    public static Type getCls(Class<? extends VMObject> aClass, VMObject object) {
        try {
            return TypeMemory.get(aClass);
        } catch (Exception e) {
            throw new PythonVMBug("Failed to create PyClass", e);
        }
    }

    public static VMObject createClass(VMObject object) {
        return getCls(object.getClass(), object);
    }

    public static Object createProxy(PyObject superObject, Class<?> javaSuperClass) {
        return PyProxyFactory.createProxy(superObject, javaSuperClass);
    }

    public static RuntimeException createValueError(String s) {
        try {
            Class<?> clazz = Class.forName("python.builtins.ValueError");
            return (RuntimeException) clazz.getConstructor(String.class).newInstance(s);
        } catch (Exception e) {
            throw new PythonVMBug("Failed to create ValueError", e);
        }
    }

    public static RuntimeException createNotImplementedError(String exec) {
        try {
            Class<?> clazz = Class.forName("python.builtins.NotImplementedError");
            return (RuntimeException) clazz.getConstructor(String.class).newInstance(exec);
        } catch (Exception e) {
            throw new PythonVMBug("Failed to create NotImplementedError", e);
        }
    }

    public static Type importAttr(String module, String name) {
        try {
            VMObject vmObject = Sys.INSTANCE.getAttr("__pyvm_data__");
            if (!(vmObject instanceof Dict))
                throw new RuntimeError(new Str("Invalid __pyvm_data__"));
            Dict dict = (Dict) vmObject;
        } catch (Exception e) {
            throw new PythonVMBug("Failed to import " + module + "." + name, e);
        }
        throw new ImportError(new Str("Cannot import " + module + "." + name));
    }

    public static void registerModule(ModuleType module) {
        try {
            VMObject vmObject = Sys.INSTANCE.getAttr("__pyvm_data__");
            if (!(vmObject instanceof Dict))
                throw new RuntimeError(new Str("Invalid __pyvm_data__"));
            Dict dict = (Dict) vmObject;
            dict.set(module.getName(), module);
        } catch (Exception e) {
            throw new PythonVMBug("Failed to register module " + module.getName(), e);
        }
    }
}
