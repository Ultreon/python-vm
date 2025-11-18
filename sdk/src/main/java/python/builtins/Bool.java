package python.builtins;

import python.Builtins;
import python._core.*;

import java.math.BigDecimal;
import java.math.BigInteger;

@PyClassMeta(name = "bool", bases = {Int.class}, module = Builtins.class)
public class Bool implements VMObject {
    private final boolean value;
    private final PyObject py;

    public Bool(boolean jvmBool) {
        this.value = jvmBool;
        py = new PyObject(this);
    }

    public Bool(int jvmInt) {
        this.value = jvmInt != 0;
        py = new PyObject(this);
    }

    public Bool(long jvmLong) {
        this.value = jvmLong != 0;
        py = new PyObject(this);
    }

    public Bool(String jvmString) {
        if (jvmString.equals("True")) this.value = true;
        else if (jvmString.equals("False")) this.value = false;
        else throw Py.createTypeError("Cannot convert " + jvmString + " to bool");
        py = new PyObject(this);
    }

    @PyConstructor(args = {"value"})
    public Bool(VMObject value) {
        if (value instanceof Int) this.value = ((Int) value).$java().equals(BigInteger.ZERO);
        else if (value instanceof Float) this.value = ((Float) value).$java().compareTo(BigDecimal.ZERO) != 0;
        else if (value instanceof Str) this.value = Boolean.parseBoolean(((Str) value).$java());
        else if (value instanceof Bool) this.value = ((Bool) value).$java();
        else throw Py.createTypeError("Cannot convert " + value + " to bool");
        py = new PyObject(this);
    }

    @PyFunction(name = "__bool__")
    public Bool __bool__() {
        return this;
    }

    @PyFunction(name = "__repr__")
    public Str __repr__() {
        return new Str(this.value ? "True" : "False");
    }

    @PyFunction(name = "__str__")
    public Str __str__() {
        return this.__repr__();
    }

    @PyFunction(name = "__eq__")
    public Bool __eq__(VMObject other) {
        if (other instanceof Bool) {
            return new Bool(this.value == ((Bool) other).value);
        }
        return new Bool(false);
    }

    @Override
    public Boolean $java() {
        return this.value;
    }

    @Override
    public PyObject $() {
        return py;
    }

    @Override
    public PyObject[] $bases() {
        return py.getBases();
    }

    public boolean isValue() {
        return value;
    }
}
