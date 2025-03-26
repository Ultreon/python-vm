package org.python._internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MetaData {
    private final Map<String, Object> metadata = new HashMap<>();

    public void set(String key, Object value) {
        metadata.put(key, value);
    }

    public Object get(String key) {
        return metadata.get(key);
    }

    public Object del(String key) {
        return metadata.remove(key);
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
