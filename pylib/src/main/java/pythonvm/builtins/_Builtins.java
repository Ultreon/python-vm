package pythonvm.builtins;

import java.util.Map;

public class _Builtins {
    public static void print(Object[] args, Map<String, Object> kwargs) {
        Object o = kwargs.get("file");
        if (o != null) throw new UnsupportedOperationException("Not implemented!");
        String[] strings = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            strings[i] = args[i].toString();
        }
        System.out.println(String.join(" ", strings));
    }

    public static int asc(String text) {
        if (text.length() > 1) throw new RuntimeException("Provided text is too long, requires 1");
        if (text.isEmpty()) throw new RuntimeException("Provided text is too short, requires 1");
        return text.charAt(0);
    }
}