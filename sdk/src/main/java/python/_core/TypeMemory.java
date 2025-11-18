package python._core;

import python.types.Type;

import java.util.HashMap;
import java.util.Map;

public class TypeMemory {
    public static final TypeMemory INSTANCE = new TypeMemory();
    private final Map<Class<?>, Type> types = new HashMap<>();

    private TypeMemory() {

    }

    public static Type get(Class<?> clazz) {
        synchronized (INSTANCE.types) {
            return INSTANCE.types.computeIfAbsent(clazz, Type::new);
        }
    }
}
