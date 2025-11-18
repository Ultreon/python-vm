package python.builtins;

import python.Builtins;
import python._core.*;

import java.math.BigDecimal;
import java.math.BigInteger;

@PyClassMeta(name = "float", bases = {Object.class}, module = Builtins.class)
public class Float implements VMObject {
    private final BigDecimal value;
    private final PyObject py;

    public Float(int jvmInt) {
        this.value = BigDecimal.valueOf(jvmInt);
        py = new PyObject(this);
    }

    public Float(long jvmLong) {
        this.value = BigDecimal.valueOf(jvmLong);
        py = new PyObject(this);
    }

    public Float(double jvmDouble) {
        this.value = BigDecimal.valueOf(jvmDouble);
        py = new PyObject(this);
    }

    public Float(float jvmFloat) {
        this.value = BigDecimal.valueOf(jvmFloat);
        py = new PyObject(this);
    }

    public Float(BigInteger jvmBigInt) {
        this.value = new BigDecimal(jvmBigInt);
        py = new PyObject(this);
    }

    public Float(BigDecimal value) {
        this.value = value;
        py = new PyObject(this);
    }

    public Float(String jvmString) {
        this.value = new BigDecimal(jvmString);
        py = new PyObject(this);
    }

    @PyConstructor(args = {"value"})
    public Float(VMObject value) {
        if (value instanceof Int) this.value = new BigDecimal(((Int) value).$java());
        else if (value instanceof Float) this.value = ((Float) value).$java();
        else if (value instanceof Str) this.value = new BigDecimal(((Str) value).$java());
        else if (value instanceof Bool) this.value = new BigDecimal(((Bool) value).$java() ? 1 : 0);
        else throw Py.createTypeError("Cannot convert " + value + " to float");
        py = new PyObject(this);
    }

    public BigDecimal $java() {
        return this.value;
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
