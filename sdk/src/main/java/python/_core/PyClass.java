package python._core;

public class PyClass implements VMObject {
    private final String name;
    private final PyClass[] bases;
    private final PyObject py;

    public PyClass(String name, PyClass[] bases) {
        this.name = name;
        this.bases = bases;
        this.py = new PyObject(Py.createClass(this));
    }

    @PyProperty.Getter(name = "__name__")
    public String $property$getter$__name__() {
        return this.name;
    }

    @PyProperty.Getter(name = "__bases__")
    public PyClass[] $property$getter$__bases__() {
        return this.bases;
    }

    @PyProperty.Getter(name = "__module__")
    public String $property$getter$__module__() {
        return "builtins";
    }

    @Override
    public Object $java() {
        return null;
    }

    @Override
    public PyObject $() {
        return py;
    }

    @Override
    public PyObject[] $bases() {
        return py.getBases();
    }
}
