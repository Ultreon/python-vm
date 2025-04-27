package org.python._internal;

import org.python.builtins.AttributeError;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MetaData {
    private final Map<String, Object> metadata = new HashMap<>();

    public void set(String key, Object value) {
        metadata.put(key, value);
    }

    public Object get(String key) {
        Object o = metadata.get(key);
        if (o instanceof PyFunction) {
            ((PyFunction) o).__init__(new Object[0], Map.of());
        }
        return o;
    }

    public void del(String key) {
        if (metadata.containsKey("#" + key + ":protect")) {
            throw new AttributeError("cannot delete protected attribute " + key);
        }
        metadata.remove(key);
    }

    public boolean has(String key) {
        return metadata.containsKey(key);
    }

    public Set<String> dir() {
        return metadata.keySet();
    }

    public Map<String, Object> dict() {
        return metadata;
    }
}
