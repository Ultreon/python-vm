package python._core;

import java.util.HashMap;
import java.util.Map;

public class PyGlobals {
    final Map<String, VMObject> variables = new HashMap<>();

    public PyGlobals() {
    }

    public PyGlobals(Map<String, VMObject> variables) {
        this.variables.putAll(variables);
    }

    public VMObject get(String name) {
        return this.variables.get(name);
    }
    public void set(String name, VMObject value) {
        this.variables.put(name, value);
    }
}
