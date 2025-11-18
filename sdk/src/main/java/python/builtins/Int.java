package python.builtins;

import python.Builtins;
import python._core.*;

import java.math.BigInteger;

@PyClassMeta(name = "int", bases = {Object.class}, module = Builtins.class)
public class Int implements VMObject {
    private final BigInteger value;
    private final PyObject py;

    public Int(int jvmInt) {
        this.value = BigInteger.valueOf(jvmInt);
        py = new PyObject(this);
    }

    @PyConstructor(args = {"value"})
    public Int(VMObject value) {
        if (value instanceof Int) this.value = ((Int) value).$java();
        else if (value instanceof Float) this.value = ((Float) value).$java().toBigInteger();
        else if (value instanceof Str) this.value = new BigInteger(((Str) value).$java());
        else if (value instanceof Bool) this.value = BigInteger.valueOf(((Bool) value).$java() ? 1 : 0);
        else throw Py.createTypeError("Cannot convert " + value + " to int");
        py = new PyObject(this);
    }

    public Int(BigInteger value) {
        this.value = value;
        py = new PyObject(this);
    }

    public BigInteger $java() {
        return this.value;
    }

    @PyFunction(name = "__repr__")
    public Str __repr__() {
        return new Str(this.value.toString());
    }

    @PyFunction(name = "__str__")
    public Str __str__() {
        return this.__repr__();
    }

    @PyFunction(name = "__int__")
    public Int __int__() {
        return this;
    }

    @PyFunction(name = "__float__")
    public Float __float__() {
        return new Float(this.value);
    }

    @PyFunction(name = "__bool__")
    public Bool __bool__() {
        return new Bool(this.value.compareTo(BigInteger.ZERO) != 0);
    }

    @Override
    public PyObject $() {
        return py;
    }

    @Override
    public PyObject[] $bases() {
        return new PyObject[0];
    }

    @Override
    public String toString() {
        return this.value.toString();
    }
}
