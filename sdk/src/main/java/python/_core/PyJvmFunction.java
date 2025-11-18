package python._core;

import python.builtins.Dict;
import python.builtins.Tuple;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;

public class PyJvmFunction implements VMObject {
    private final Object __jvmobject__;
    private final Method method;
    private final PyObject py;

    public PyJvmFunction(Object jvmObject, Method method) {
        this.__jvmobject__ = jvmObject;
        this.method = method;
        this.py = new PyObject(this);
    }

    @Override
    @PyFunction(name = "__call__")
    public VMObject call(VMObject args, VMObject kwargs) {
        if (!(args instanceof Tuple) || !(kwargs instanceof Dict)) {
            throw Py.createTypeError("Expected tuple and dict as arguments");
        }

        Tuple tupleArgs = (Tuple) args;
        Object[] jvmArgs = new Object[tupleArgs.getValues().length];
        for (int i = 0; i < tupleArgs.getValues().length; i++) {
            jvmArgs[i] = tupleArgs.getValues()[i].$java();
        }
        Dict dictArgs = (Dict) kwargs;
        if (dictArgs.__len__().$java().compareTo(BigInteger.ZERO) > 0) {
            throw Py.createTypeError("JVM functions do not support keyword arguments");
        }
        try {
            return new PyJvmObject(method.invoke(__jvmobject__, jvmArgs));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw Py.createRuntimeError(e);
        }
    }

    @Override
    public Object $java() {
        return method;
    }

    @Override
    public PyObject $() {
        return py;
    }

    @Override
    public PyObject[] $bases() {
        return new PyObject[0];
    }

    @PyProperty.Getter(name = "__jvmobject__")
    public Object $property$getter$__jvmobject__() {
        return this.__jvmobject__;
    }
}
