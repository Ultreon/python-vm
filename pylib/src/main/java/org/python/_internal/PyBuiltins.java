package org.python._internal;

import dev.ultreon.pythonc.CompilerException;
import dev.ultreon.pythonc.PythonCompiler;
import org.python.builtins.SyntaxError;
import org.python.builtins.TypeError;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Map;
import java.util.Scanner;
import java.util.StringJoiner;

public class PyBuiltins {
    public static Object print(Object[] args, Map<String, Object> kwargs) {
        Object file = kwargs.getOrDefault("file", null);
        Object end = kwargs.getOrDefault("end", "\n");
        Object sep = kwargs.getOrDefault("sep", " ");
        if (file != null) throw new UnsupportedOperationException("Not implemented!");
        StringJoiner joiner = new StringJoiner((String) sep);
        String[] strings = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            strings[i] = ClassUtils.stringify(args[i]);
        }
        System.out.print(String.join(" ", strings) + end);
        return null;
    }

    private static final Scanner SCANNER = new Scanner(System.in);

    public static Object input(Object[] args, Map<String, Object> kwargs) {
        Object file = kwargs.getOrDefault("file", null);
        Object prompt = kwargs.getOrDefault("prompt", "? ");
        if (file != null) throw new UnsupportedOperationException("Not implemented!");
        System.out.print(prompt);
        return SCANNER.nextLine();
    }

    public static Object asc(Object[] args, Map<String, Object> kwargs) {
        Object text = args.length == 1 ? args[0] : kwargs.get("text");
        if (text instanceof String) {
            return ((String) text).charAt(0);
        }
        throw new UnsupportedOperationException("Not implemented!");
    }

    public static Object ord(Object[] args, Map<String, Object> kwargs) {
        Object text = args.length == 1 ? args[0] : kwargs.get("text");
        if (text instanceof String) {
            return ((String) text).charAt(0);
        }
        throw new UnsupportedOperationException("Not implemented!");
    }

    public static Object chr(Object[] args, Map<String, Object> kwargs) {
        Object i = args.length == 1 ? args[0] : kwargs.get("i");
        if (i instanceof Integer) {
            return String.valueOf((char) i);
        }
        throw new UnsupportedOperationException("Not implemented!");
    }

    public static Object hex(Object[] args, Map<String, Object> kwargs) {
        Object i = args.length == 1 ? args[0] : kwargs.get("i");
        if (i instanceof Integer) {
            return Integer.toHexString((int) i);
        }
        throw new UnsupportedOperationException("Not implemented!");
    }

    public static Object oct(Object[] args, Map<String, Object> kwargs) {
        Object i = args.length == 1 ? args[0] : kwargs.get("i");
        if (i instanceof Integer) {
            return Integer.toOctalString((int) i);
        }
        throw new UnsupportedOperationException("Not implemented!");
    }

    public static Object bin(Object[] args, Map<String, Object> kwargs) {
        Object i = args.length == 1 ? args[0] : kwargs.get("i");
        if (i instanceof Integer) {
            return Integer.toBinaryString((int) i);
        }
        throw new UnsupportedOperationException("Not implemented!");
    }

    public static Object len(Object[] args, Map<String, Object> kwargs) throws Exception {
        Object o = args.length == 1 ? args[0] : kwargs.get("o");
        return ClassUtils.getLength(o);
    }

    public static Object max(Object[] args, Map<String, Object> kwargs) {
        Object iterable = args.length == 1 ? args[0] : get(kwargs, "iterable");
        return ClassUtils.getMax((Collection<?>) iterable);
    }

    public static Object getattr(Object[] args, Map<String, Object> kwargs) {
        Object obj = args.length == 2 ? args[0] : get(kwargs, "obj");
        Object name = args.length == 2 ? args[1] : get(kwargs, "name");
        try {
            return ClassUtils.getAttribute(obj, (String) name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object setattr(Object[] args, Map<String, Object> kwargs) {
        Object obj = args.length == 3 ? args[0] : get(kwargs, "obj");
        Object name = args.length == 3 ? args[1] : get(kwargs, "name");
        Object value = args.length == 3 ? args[2] : get(kwargs, "value");
        try {
            ClassUtils.setAttribute(obj, (String) name, value);
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object delattr(Object[] args, Map<String, Object> kwargs) {
        Object obj = args.length == 2 ? args[0] : get(kwargs, "obj");
        Object name = args.length == 2 ? args[1] : get(kwargs, "name");
        try {
            ClassUtils.delAttribute(obj, (String) name);
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object hasattr(Object[] args, Map<String, Object> kwargs) throws Exception {
        Object obj = args.length == 2 ? args[0] : get(kwargs, "obj");
        Object name = args.length == 2 ? args[1] : get(kwargs, "name");
        return ClassUtils.hasAttribute(obj, (String) name);
    }

    public static Object hash(Object[] args, Map<String, Object> kwargs) throws Exception {
        Object o = args.length == 1 ? args[0] : get(kwargs, "o");
        return ClassUtils.getHash(o);
    }

    public static Object abs(Object[] args, Map<String, Object> kwargs) throws Exception {
        Object o = args.length == 1 ? args[0] : get(kwargs, "o");
        return ClassUtils.getAbs(o);
    }

    private static Object get(Map<String, Object> kwargs, String name) {
        if (!kwargs.containsKey(name)) {
            throw new IllegalArgumentException("Missing argument: " + name);
        }

        return kwargs.get(name);
    }

    public static Object pow(Object[] args, Map<String, Object> kwargs) {
        Object base = args.length == 2 ? args[0] : kwargs.get("base");
        Object exp = args.length == 2 ? args[1] : kwargs.get("exp");
        return Math.pow((Double) base, (Double) exp);
    }

    public static Object round(Object[] args, Map<String, Object> kwargs) {
        Object number = args.length == 1 ? args[0] : kwargs.get("number");
        return Math.round((Double) number);
    }

    public static Object ceil(Object[] args, Map<String, Object> kwargs) {
        Object number = args.length == 1 ? args[0] : kwargs.get("number");
        return Math.ceil((Double) number);
    }

    public static Object floor(Object[] args, Map<String, Object> kwargs) {
        Object number = args.length == 1 ? args[0] : kwargs.get("number");
        return Math.floor((Double) number);
    }

    public static Object sum(Object[] args, Map<String, Object> kwargs) {
        Object iterable = args.length == 1 ? args[0] : kwargs.get("iterable");
        return ClassUtils.getSum((Collection<?>) iterable);
    }

    public static Object eval(Object[] args, Map<String, Object> kwargs) {
        class EvalClassLoader extends ClassLoader {
            public Class<?> define(byte[] data) {
                try {
                    return super.defineClass(null, data, 0, data.length);
                } catch (Exception e) {
                    throw new TypeError("Compiled Python code is invalid: " + e.getMessage());
                }
            }
        }

        String code = args.length == 1 ? (String) args[0] : (String) kwargs.get("code");
        PythonCompiler compiler = new PythonCompiler();
        byte[] bytes = compiler.evalCompile(code);

        EvalClassLoader loader = new EvalClassLoader();
        return loader.define(bytes);
    }

    public static Object exec(Object[] args, Map<String, Object> kwargs) {
        class ExecClassLoader extends ClassLoader {
            public Class<?> define(byte[] data) {
                try {
                    return super.defineClass(null, data, 0, data.length);
                } catch (Exception e) {
                    throw new TypeError("Compiled Python code is invalid: " + e.getMessage());
                }
            }
        }

        String code = args.length == 1 ? (String) args[0] : (String) kwargs.get("code");
        PythonCompiler compiler = new PythonCompiler();
        byte[] compile;
        try {
            compile = compiler.compile(code);
        } catch (CompilerException e) {
            try {
                throw new SyntaxError(e.toAdvancedString());
            } catch (SyntaxError | IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        ExecClassLoader loader = new ExecClassLoader();
        Class<?> define = loader.define(compile);
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            lookup.ensureInitialized(define);
        } catch (IllegalAccessException e) {
            throw new PythonVMBug(e);
        }
        return null;
    }

    public static Object repr(Object[] args, Map<String, Object> kwargs) {
        Object o = args.length == 1 ? args[0] : kwargs.get("o");
        return ClassUtils.getRepr(o);
    }

    public static Object type(Object[] args, Map<String, Object> kwargs) {
        Object o = args.length == 1 ? args[0] : kwargs.get("o");
        try {
            return ClassUtils.getType(o);
        } catch (Exception e) {
            throw new TypeError("object of type '" + o.getClass().getSimpleName() + "' has no __class__ attribute");
        }
    }

    public static Object dir(Object[] args, Map<String, Object> kwargs) {
        Object o = args.length == 1 ? args[0] : kwargs.get("o");
        return ClassUtils.getDir(o);
    }

    public static Object open(Object[] args, Map<String, Object> kwargs) {
        throw new UnsupportedOperationException("Not implemented!");
    }

    public static Object zip(Object[] args, Map<String, Object> kwargs) {
        Object iterable = args.length == 1 ? args[0] : kwargs.get("iterable");
        return ClassUtils.zip(iterable);
    }

    public static Object enumerate(Object[] args, Map<String, Object> kwargs) {
        Object iterable = args.length == 1 ? args[0] : kwargs.get("iterable");
        return ClassUtils.enumerate(iterable);
    }
}
