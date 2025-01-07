import java.util.HashMap;
import java.util.Map;

public class Tests7<T> {
    public T hello;
    public Class<?> type;
    public Map<String, String> map = new HashMap<>();

    public Tests7(T hello) {
        this.hello = hello;
        this.type = Tests7.class;

        Map<String, String> map = new HashMap<>();
        map.put("hello", "world");

        System.out.println(map.get("hello"));

        this.map = map;
    }

    public static class Hello {
        int yes = 3;
    }
}
