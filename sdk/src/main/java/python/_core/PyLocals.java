package python._core;

import java.util.*;

public class PyLocals extends PyGlobals {
    private final Set<String> globalized = new HashSet<>();
    private final Set<String> nonlocalized = new HashSet<>();
    private final PyGlobals globals;
    private final PyLocals parent;

    public PyLocals() {
        this.globals = null;
        this.parent = null;
    }

    public PyLocals(Map<String, VMObject> locals) {
        super.variables.putAll(locals);
        this.globals = null;
        this.parent = null;
    }

    public PyLocals(PyGlobals globals) {
        super.variables.putAll(globals.variables);
        this.globals = globals;
        this.parent = null;
    }

    public PyLocals(PyLocals parent, PyGlobals globals) {
        this.parent = parent;
        this.globals = globals;
    }

    @Override
    public VMObject get(String name) {
        if (globalized.contains(name) && globals != null) {
            return globals.get(name);
        } else if (nonlocalized.contains(name) && parent != null) {
            return parent.get(name);
        }
        return super.get(name);
    }

    @Override
    public void set(String name, VMObject value) {
        if (globalized.contains(name) && globals != null) {
            globals.set(name, value);
            return;
        } else if (nonlocalized.contains(name) && parent != null) {
            parent.set(name, value);
            return;
        }
        super.set(name, value);
    }

    public void globalize(String name) {
        this.globalized.add(name);
    }

    public void nonlocalize(String name) {
        this.nonlocalized.add(name);
    }
}
