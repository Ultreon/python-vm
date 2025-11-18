package python._core;

import python.builtins.*;
import python.types.Type;

import java.lang.Object;
import java.util.List;

public final class PyObject {
    final VMObject object;
    private VMObject attributes = new Dict();
    private final Dict properties = new Dict();
    private VMObject __dict__;
    private List<VMObject> bases;
    private Class<?>[] baseClasses;

    public PyObject(PyObject parent, VMObject object) {
        this.object = object;

        ((Dict) attributes).set("__class__", parent.object);
        ((Dict) attributes).set("__dict__", attributes);
    }

    public PyObject(VMObject object) {
        this.object = object;
        ((Dict) this.attributes).set("__class__", Py.getCls(object.getClass(), object));
        ((Dict) this.attributes).set("__dict__", attributes);
    }

    public PyObject() {
        this.object = new python.builtins.Object();
    }

    public VMObject getAttr(String name) {
        if (name.equals("__dict__")) {
            return attributes;
        }
        if (attributes instanceof Dict) {
            if (!((Dict) attributes).contains(name)) {
                throw new KeyError(new Str(name));
            }
            return ((Dict) attributes).get(name);
        }
        throw new AttributeError(new Str(name));
    }

    public void setAttr(String name, VMObject value) {
        if (name.equals("__dict__"))
            attributes = value;
        if (attributes instanceof Dict)
            ((Dict) attributes).set(name, value);
    }

    public VMObject toJava() {
        return this.object;
    }

    public VMObject call(Object[] objects, Dict dict) {
        VMObject callFunc = getAttr("__call__");
        if (callFunc == null)
            throw Py.createTypeError("Cannot call " + this);
        return callFunc.$().call(objects, dict);
    }

    public PyObject[] getBases() {
        return new PyObject[0];
    }

    public Object proxy(Class<?> cls) {
        for (PyObject superObject : object.$bases()) {
            if (cls.isInstance(superObject.toJava())) {
                return Py.createProxy(superObject, cls);
            }
        }
        return null;
    }

    public boolean hasAttr(String methodName) {
        if (attributes instanceof Dict) {
            return ((Dict) attributes).contains(methodName);
        }
        return false;
    }

    public void superCall(Type type, Tuple args, Dict kwargs) {
        bases.add(type.call(args, kwargs));
    }

    public void delAttr(String name) {
        if (attributes instanceof Dict) {
            ((Dict) attributes).remove(name);
        } else {
            throw new AttributeError(new Str(name));
        }
    }
}
