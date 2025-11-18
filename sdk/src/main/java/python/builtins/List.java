package python.builtins;

import python.Builtins;
import python._core.*;

import java.util.Map;

@PyClassMeta(name = "list", bases = {Object.class}, module = Builtins.class)
public class List implements VMObject {
    private final java.util.List<? extends VMObject> values;
    private final PyObject py;
    private PyObject[] supers;

    @PyConstructor(args = {})
    public List() {
        this.values = new java.util.ArrayList<>();
        this.py = new PyObject(this);
        this.py.superCall(Py.importAttr("builtins", "object"), new Tuple(), new Dict());
    }

    public List(int length) {
        this.values = new java.util.ArrayList<>(length);
        this.py = new PyObject(this);
        this.py.superCall(Py.importAttr("builtins", "object"), new Tuple(), new Dict());
    }

    public List(java.util.List<? extends VMObject> values) {
        this.values = values;
        this.py = new PyObject(this);
        this.py.superCall(Py.importAttr("builtins", "object"), new Tuple(), new Dict());
    }

    @Override
    public java.util.List<? extends VMObject> $java() {
        return this.values;
    }

    @Override
    public PyObject $() {
        return py;
    }

    @Override
    public PyObject[] $bases() {
        return supers;
    }

    @Override
    public java.lang.Object call(java.lang.Object[] args, Map<String, java.lang.Object> kwargs) {
        return null;
    }
}
