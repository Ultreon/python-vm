package python.builtins;

import python.Builtins;
import python._core.*;

import java.util.Map;

@PyClassMeta(name = "object", bases = {}, module = Builtins.class)
public class Object implements VMObject {
    private final PyObject py = new PyObject(this);

    @PyConstructor(args = {})
    public Object() {

    }

    @Override
    public java.lang.Object $java() {
        return this;
    }

    @Override
    public PyObject $() {
        return this.py;
    }

    @Override
    public PyObject[] $bases() {
        return py.getBases();
    }

    @Override
    public java.lang.Object call(java.lang.Object[] args, Map<String, java.lang.Object> kwargs) {
        Tuple argsTuple = new Tuple(args.length);
        for (int i = 0; i < args.length; i++) {
            argsTuple.set(i, Py.createObject(args[i]));
        }
        return call(argsTuple, Dict.fromMap(kwargs));
    }

    @Override
    public String toString() {
        return "<object at 0x" + Integer.toHexString(this.hashCode()) + ">";
    }
}
