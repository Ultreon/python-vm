package python.types;

import python.Types;
import python._core.*;
import python.builtins.Dict;
import python.builtins.RuntimeError;
import python.builtins.Str;
import python.builtins.Tuple;

import java.lang.reflect.Constructor;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

@PyClassMeta(name = "type", bases = {Object.class}, module = Types.class)
public class Type implements VMObject {
    private final Class<?> jvmClass;
    private Type[] bases;
    private PyObject py;

    public Type(Class<?> jvmClass) {
        this.jvmClass = jvmClass;

        Class<?>[] baseClasses = jvmClass.getAnnotation(PyClassMeta.class).bases();
        this.bases = new Type[baseClasses.length];
        for (int i = 0; i < baseClasses.length; i++) {
            this.bases[i] = TypeMemory.get(baseClasses[i]);
        }

        this.py = new PyObject(this);
    }

    @PyConstructor(args = {"value"})
    public Type(VMObject value) {
        jvmClass = (Class<?>) value.$().getAttr("__class__").$java();
        py = new PyObject(this);
    }

    @PyFunction(name = "__tojava__")
    public Object $java() {
        return this.jvmClass;
    }

    @Override
    public PyObject $() {
        return py;
    }

    @Override
    public PyObject[] $bases() {
        return py.getBases();
    }

    @Override
    public Object call(Object[] args, Map<String, Object> kwargs) {
        return call(new Tuple(VMObject.toVMObjectArray(args)), Dict.fromMap(kwargs)).$java();
    }

    @PyFunction(name = "__name__")
    public String __name__() {
        return jvmClass.getAnnotation(PyClassMeta.class).name();
    }

    @PyFunction(name = "__module__")
    public String __module__() {
        Class<? extends ModuleType> module = jvmClass.getAnnotation(PyClassMeta.class).module();
        if (!module.isAnnotationPresent(PyModuleMeta.class))
            throw new RuntimeError(new Str("Module " + module.getName() + " is not annotated with @PyModuleMeta"));
        PyModuleMeta meta = module.getAnnotation(PyModuleMeta.class);
        return meta.name();
    }

    @PyFunction(name = "__bases__")
    public Type[] __bases__() {
        return this.bases;
    }

    @PyFunction(name = "__call__", args = {"*args", "**kwargs"})
    public VMObject call(VMObject args, VMObject kwargs) {
        Tuple tupleArgs = (Tuple) args;
        Dict dictArgs = (Dict) kwargs;

        BitSet setArgs = new BitSet();
        Constructor<?>[] constructors = jvmClass.getConstructors();
        for (Constructor<?> constructor : constructors) {
            if (constructor.isAnnotationPresent(PyConstructor.class)) {
                PyConstructor annotation = constructor.getAnnotation(PyConstructor.class);
                List<String> argNames = List.of(annotation.args());
                int preArgs = tupleArgs.length() - argNames.size();

                Object[] args2 = new Object[constructor.getParameterCount()];
                for (int i = 0; i < argNames.size(); i++) {
                    args2[i] = tupleArgs.$java()[preArgs++].$java();
                    setArgs.set(i);
                }

                VMObject entries = dictArgs.entries();
                if (!(entries instanceof Tuple)) throw Py.createTypeError("Expected tuple, got " + entries.__class__().__name__());
                for (VMObject o : ((Tuple) entries).$java()) {
                    if (!(o instanceof Tuple)) throw Py.createTypeError("Expected tuple, got " + o.__class__().__name__());
                    Tuple t = (Tuple) o;
                    if (t.length() != 2) throw Py.createTypeError("Expected tuple of length 2, got " + t.length());
                    VMObject vmObject = t.get(0);
                    if (!(vmObject instanceof Str)) throw Py.createTypeError("Expected str, got " + vmObject.__class__().__name__());
                    int idx = argNames.indexOf(((Str) vmObject).$java());
                    if (preArgs > idx) {
                        throw Py.createTypeError("Cannot set keyword argument '" + t.get(0).__repr__().$java() + "', because it was already set as positional argument");
                    }
                    args2[idx] = t.get(1).$java();
                }

                try {
                    constructor.newInstance(args2);
                } catch (Exception e) {
                    throw Py.createRuntimeError(e);
                }
            }
            setArgs.clear(0, constructor.getParameterCount());
        }

        throw Py.createTypeError("No constructor found for " + this.__name__());
    }
}
