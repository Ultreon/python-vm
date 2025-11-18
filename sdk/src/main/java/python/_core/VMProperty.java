package python._core;

public class VMProperty {
    private VMObject value;
    private final String name;
    private final PyObject owner;

    public VMProperty(VMObject value, String name, PyObject owner) {
        this.value = value;
        this.name = name;
        this.owner = owner;
    }

    public VMObject get() {
        return this.value;
    }

    public void set(VMObject value) {
        this.value = value;
    }

    public void delete() {
        this.value = null;
        owner.delAttr(name);
    }
}
