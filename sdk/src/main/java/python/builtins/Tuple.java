package python.builtins;

import python.Builtins;
import python._core.*;

import java.util.Map;

@PyClassMeta(name = "tuple", bases = {Object.class}, module = Builtins.class)
public class Tuple implements VMObject {
    private final VMObject[] values;
    private final PyObject py;

    @PyConstructor(args = {"*values"})
    public Tuple(VMObject... values) {
        this.values = values;
        py = new PyObject(this);
    }

    @PyConstructor(args = {"value"})
    public Tuple(VMObject value) {
        this.values = new VMObject[]{value};
        py = new PyObject(this);
    }

    @PyConstructor(args = {})
    public Tuple() {
        this.values = new VMObject[0];
        py = new PyObject(this);
    }

    public Tuple(int length) {
        this.values = new VMObject[length];
        py = new PyObject(this);
    }

    public Tuple(int length, VMObject value) {
        this.values = new VMObject[length];
        for (int i = 0; i < length; i++) {
            this.values[i] = value;
        }
        py = new PyObject(this);
    }

    void set(int index, VMObject value) {
        this.values[index] = value;
    }

    public int length() {
        return this.values.length;
    }

    public VMObject get(int index) {
        return this.values[index];
    }

    public VMObject[] getValues() {
        return this.values;
    }

    public VMObject[] $java() {
        return this.values;
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
        return this.__repr__().$java();
    }

    @PyFunction(name = "__repr__")
    public Str __repr__() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (int i = 0; i < this.values.length; i++) {
            sb.append(this.values[i].__repr__().$java());
            if (i < this.values.length - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return new Str(sb.toString());
    }

    @Override
    public java.lang.Object call(java.lang.Object[] args, Map<String, java.lang.Object> kwargs) {
        throw Py.createTypeError("Cannot call a tuple");
    }

    @PyFunction(name = "__getitem__")
    public VMObject __getitem__(VMObject index) {
        return this.values[((Int) index).$java().intValue()];
    }

    @PyFunction(name = "__setitem__")
    public void __setitem__(VMObject index, VMObject value) {
        this.values[((Int) index).$java().intValue()] = value;
    }

    @PyFunction(name = "__delitem__")
    public void __delitem__(VMObject index) {
        throw Py.createTypeError("Cannot delete items from a tuple");
    }

    @PyFunction(name = "index")
    public Int index(VMObject value) {
        for (int i = 0; i < this.values.length; i++) {
            if (this.values[i].equals(value)) {
                return new Int(i);
            }
        }
        throw Py.createValueError("tuple.index(x): x not in tuple");
    }

    @PyFunction(name = "__len__")
    public Int __len__() {
        return new Int(this.values.length);
    }
}
