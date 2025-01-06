package python.builtins;

public class _Builtins {
    public static void print(Object... s) {
        String[] strings = new String[s.length];
        for (int i = 0; i < s.length; i++) {
            strings[i] = s[i].toString();
        }
        System.out.println(String.join(" ", strings));
    }
}