package python.builtins;

import python._core.Py;
import python._core.PyConstructor;
import python._core.PyObject;
import python._core.VMObject;

import java.util.Map;
import java.util.Objects;

public class Str implements VMObject {
    private final String value;
    private final PyObject py;

    public Str(String value) {
        this.value = value;
        py = new PyObject(this);
    }

    @PyConstructor(args = {"value"})
    public Str(VMObject value) {
        VMObject strFunc = value.$().getAttr("__str__");
        if (strFunc == null) {
            throw Py.createTypeError("Cannot convert " + value + " to string");
        }
        VMObject call = strFunc.$().call(new Object[0], new Dict());
        if (!(call instanceof Str)) {
            throw Py.createTypeError("Cannot convert " + value + " to string");
        }
        this.value = ((Str) call).$java();
        py = new PyObject(this);
    }

    @Override
    public String $java() {
        return value;
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
    public java.lang.Object call(java.lang.Object[] args, Map<String, java.lang.Object> kwargs) {
        return null;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Str str = (Str) o;
        return Objects.equals(value, str.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
