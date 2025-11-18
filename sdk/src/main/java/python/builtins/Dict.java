package python.builtins;

import python.Builtins;
import python._core.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

@PyClassMeta(name = "dict", bases = {Object.class}, module = Builtins.class)
public class Dict implements VMObject {
    private final java.util.List<Str> keys;
    private final java.util.List<VMObject> values;
    private final java.lang.Object lock_ = new java.lang.Object();
    private final PyObject py;

    public Dict() {
        this.keys = new ArrayList<>();
        this.values = new ArrayList<>();
        py = new PyObject(this);
    }

    @SuppressWarnings({"rawtypes"})
    public Dict(Map attributes) {
        this();
        addMap(attributes);
    }

    @PyConstructor(args = {"key", "value"})
    public Dict(VMObject key, VMObject value) {
        this();
        this.keys.add((Str) key);
        this.values.add(value);
    }

    @SuppressWarnings({"rawtypes"})
    @PyConstructor(args = {"value"})
    public Dict(VMObject value) {
        this.keys = new ArrayList<>();
        this.values = new ArrayList<>();
        if (value instanceof Map) {
            addMap((Map) value);
        } else if (value instanceof List) {
            addList((java.util.List) value);
        } else if (value instanceof Dict) {
            addDict((Dict) value);
        } else {
            throw Py.createTypeError("Cannot convert " + value + " to dict");
        }
        py = new PyObject(this);
    }

    public static Dict fromMap(Map<String, java.lang.Object> kwargs) {
        Dict dict = new Dict();
        for (Map.Entry<String, java.lang.Object> entry : kwargs.entrySet()) {
            dict.set(new Str(entry.getKey()), Py.createObject(entry.getValue()));
        }
        return dict;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void addMap(Map value) {
        Set<Map.Entry> entries = value.entrySet();
        for (Map.Entry entry : entries) {
            this.keys.add(entry.getKey() instanceof Str ? (Str) entry.getKey() : new Str(entry.getKey().toString()));
            this.values.add(entry.getValue() instanceof VMObject ? (VMObject) entry.getValue() : new PyJvmObject(entry.getValue()));
        }
    }

    @SuppressWarnings({"rawtypes", "DataFlowIssue"})
    private void addList(java.util.List value) {
        for (int i = 0; i < value.size(); i += 2) {
            this.keys.add((Str) value.get(i));
            Object val = (Object) value.get(i + 1);
            this.values.add(val instanceof VMObject ? (VMObject) val : new PyJvmObject(val));
        }
    }

    private void addDict(Dict value) {
        for (int i = 0; i < value.keys.size(); i++) {
            this.keys.add(value.keys.get(i));
            this.values.add(value.values.get(i));
        }
    }

    public void set(Str key, VMObject value) {
        this.keys.add(key);
        this.values.add(value);
    }

    @PyFunction(name = "__setitem__")
    public void __setitem__(VMObject key, VMObject value) {
        if (key instanceof Str) {
            set((Str) key, value);
            return;
        }
        throw Py.createTypeError("Cannot set dict key to " + key);
    }

    @PyFunction(name = "__getitem__")
    public VMObject __getitem__(VMObject key) {
        if (key instanceof Str) {
            return get((Str) key);
        }
        throw Py.createTypeError("Cannot get dict value for " + key);
    }

    @PyFunction(name = "__delitem__")
    public void __delitem__(VMObject key) {
        if (key instanceof Str) {
            synchronized (lock_) {
                int index = this.keys.indexOf(key);
                if (index == -1) {
                    return;
                }
                this.keys.remove(index);
                this.values.remove(index);
                return;
            }
        }
        throw Py.createTypeError("Cannot delete dict key " + key);
    }

    @PyFunction(name = "get")
    public VMObject get(VMObject key) {
        if (key instanceof Str) {
            synchronized (lock_) {
                int index = this.keys.indexOf((Str) key);
                if (index == -1) {
                    return null;
                }
                return this.values.get(index);
            }
        }
        throw Py.createTypeError("Cannot get dict value for " + key);
    }

    @PyFunction(name = "keys")
    public List keys() {
        synchronized (lock_) {
            return new List(this.keys);
        }
    }

    @PyFunction(name = "__repr__")
    public Str __repr__() {
        synchronized (lock_) {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            for (int i = 0; i < this.keys.size(); i++) {
                sb.append(this.keys.get(i).__repr__().$java());
                sb.append(": ");
                sb.append(this.values.get(i).__repr__().$java());
                if (i < this.keys.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append("}");
            return new Str(sb.toString());
        }
    }

    @Override
    public java.lang.Object call(java.lang.Object[] args, Map<String, java.lang.Object> kwargs) {
        return null;
    }

    @Override
    public VMObject call(VMObject args, VMObject kwargs) {
        return null;
    }

    @PyFunction(name = "__len__")
    public Int __len__() {
        synchronized (lock_) {
            return new Int(this.keys.size());
        }
    }

    public VMObject __contains__(VMObject key) {
        if (!(key instanceof Str)) {
            return new Bool(false);
        }
        synchronized (lock_) {
            return new Bool(this.keys.contains((Str) key));
        }
    }

    @PyFunction(name = "items")
    public VMObject entries() {
        synchronized (lock_) {
            Tuple tuple = new Tuple(this.keys.size());
            for (int i = 0; i < this.keys.size(); i++) {
                tuple.set(i, new Tuple(this.keys.get(i), this.values.get(i)));
            }
            return tuple;
        }
    }

    public Map<String, java.lang.Object> $java() {
        Map<String, java.lang.Object> map = new java.util.HashMap<>();
        for (int i = 0; i < this.keys.size(); i++) {
            map.put(this.keys.get(i).$java(), this.values.get(i).$java());
        }
        return map;
    }

    @Override
    public PyObject $() {
        return py;
    }

    @Override
    public PyObject[] $bases() {
        return py.getBases();
    }

    public void set(String key, VMObject value) {
        this.keys.add(new Str(key));
        this.values.add(value);
    }

    public boolean contains(String name) {
        return this.keys.stream().anyMatch(k -> k.$java().equals(name));
    }

    public VMObject get(String name) {
        int index = this.keys.indexOf(new Str(name));
        if (index == -1) {
            return null;
        }
        return this.values.get(index);
    }

    public void remove(String name) {
        synchronized (lock_) {
            int index = this.keys.indexOf(new Str(name));
            if (index == -1) {
                return;
            }
            this.keys.remove(index);
            this.values.remove(index);
        }
    }
}
