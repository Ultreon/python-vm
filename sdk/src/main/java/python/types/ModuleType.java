package python.types;

import python.Types;
import python._core.*;
import python.builtins.Dict;
import python.builtins.Object;
import python.builtins.Str;
import python.builtins.Tuple;

import java.util.Map;
import java.util.Objects;

@PyClassMeta(name = "ModuleType", bases = {Object.class}, module = Types.class)
public class ModuleType implements VMObject {
    protected final PyObject py = new PyObject(this);
    private final String name = Objects.requireNonNull(this.getClass().getAnnotation(PyModuleMeta.class), "Module must be annotated with @PyModuleMeta").name();

    public ModuleType() {
        py.superCall(Py.importAttr("builtins", "object"), new Tuple(), new Dict());
    }

    @Override
    public java.lang.Object $java() {
        return this;
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
        throw Py.createTypeError("Cannot call a module");
    }

    @Override
    public String toString() {
        return "<module '" + name + "' (built-in)>";
    }

    public Str getName() {
        return new Str(name);
    }
}
