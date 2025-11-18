package python._core;

import python.builtins.*;
import python.types.Type;

import java.lang.Object;
import java.util.Map;

public interface VMObject {
    static VMObject[] toVMObjectArray(Object[] args) {
        VMObject[] vmObjects = new VMObject[args.length];
        for (int i = 0; i < args.length; i++) {
            vmObjects[i] = new PyJvmObject(args[i]);
        }
        return vmObjects;
    }

    Object $java();

    PyObject $();

    PyObject[] $bases();

    @PyFunction(name = "__eq__")
    default Bool __eq__(VMObject other) {
        return new Bool(this == other);
    }

    @PyFunction(name = "__ne__")
    default Bool __ne__(VMObject other) {
        return new Bool(this != other);
    }

    @PyFunction(name = "__hash__")
    default Int __hash__() {
        return new Int(this.hashCode());
    }

    @PyFunction(name = "__repr__")
    default Str __repr__() {
        return new Str("<object '" + this.__class__().__name__() + "'>");
    }

    @PyFunction(name = "__str__")
    default Str __str__() {
        return this.__repr__();
    }

    @PyProperty.Getter(name = "__class__")
    default Type __class__() {
        return Py.getCls(this.getClass(), this);
    }

    default Object call(Object[] args, Map<String, Object> kwargs) {
        return null;
    }

    default VMObject call(VMObject args, VMObject kwargs) {
        throw Py.createTypeError("Object is not callable");
    }

    default void __delitem__(VMObject name) {
        throw Py.createTypeError("Object does not support item deletion");
    }

    default VMObject getAttr(String stdin) {
        return $().getAttr(stdin);
    }

    default void setAttr(String stdin, VMObject value) {
        $().setAttr(stdin, value);
    }

    default void delAttr(String stdin) {
        $().delAttr(stdin);
    }
}
