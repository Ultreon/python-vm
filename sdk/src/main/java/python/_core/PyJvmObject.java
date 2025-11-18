package python._core;

import python.builtins.NotImplementedError;
import python.builtins.Str;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class PyJvmObject implements VMObject {
    private final Object __jvmobject__;
    private final Map<String, VMObject> attributes = new HashMap<>();
    private PyObject py;

    public PyJvmObject(Object jvmObject) {
        Class<?> clazz = jvmObject.getClass();
        this.__jvmobject__ = jvmObject;

        for (Method method : clazz.getMethods()) {
            attributes.put(method.getName(), new PyJvmFunction(jvmObject, method));
        }
    }

    @PyFunction(name = "__getattr__", args = {"name"})
    public VMObject __getattr__(VMObject name) {
        synchronized (attributes) {
            String strName = ((Str) name).$java();
            if (!attributes.containsKey(strName)) {
                throw Py.createAttributeError(strName);
            }
            return attributes.get(strName);
        }
    }

    @Override
    public VMObject call(VMObject args, VMObject kwargs) {
        throw new NotImplementedError(new Str("JVM objects aren't callable yet"));
    }

    @Override
    public Object $java() {
        return this.__jvmobject__;
    }

    @Override
    public PyObject $() {
        return py;
    }

    @Override
    public PyObject[] $bases() {
        return py.getBases();
    }

    @PyProperty.Getter(name = "__jvmobject__")
    public Object $property$getter$__jvmobject__() {
        return this.__jvmobject__;
    }
}
