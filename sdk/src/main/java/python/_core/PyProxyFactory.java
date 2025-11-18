package python._core;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;
import python.builtins.Dict;

import java.lang.ref.Cleaner;
import java.lang.reflect.InvocationHandler;

public class PyProxyFactory {

    private static final Cleaner CLEANER = Cleaner.create();

    public static Object createProxy(PyObject superObject, Class<?> javaSuperClass) {
        try {
            // Invocation handler to route Java calls → PyObject
            InvocationHandler handler = (proxy, method, args) -> {
                // Example: call method on PyObject, if supported
                String methodName = method.getName();

                VMObject attr = superObject.getAttr(methodName);
                if (attr != null) {
                    // Call the Python method with the same name
                    PyObject pyMethod = superObject.getAttr(methodName).$();
                    VMObject[] pyArgs = new VMObject[args.length];
                    for (int i = 0; i < args.length; i++) {
                        pyArgs[i] = new PyJvmObject(args[i]);
                    }
                    PyObject result = pyMethod.call(args, new Dict()).$();
                    return result.toJava();
                }

                throw Py.createAttributeError(methodName);
            };

            // Use ByteBuddy to subclass or implement
            DynamicType.Unloaded<?> make = new ByteBuddy()
                    .subclass(
                            javaSuperClass.isInterface() ? Object.class : javaSuperClass
                    )
                    .implement(
                            javaSuperClass.isInterface() ? new Class[]{javaSuperClass} : new Class<?>[0]
                    )
                    .method(ElementMatchers.any())
                    .intercept(InvocationHandlerAdapter.of(handler))
                    .make();
            CLEANER.register(superObject, make::close);
            Class<?> proxyClass = make
                    .load(javaSuperClass.getClassLoader())
                    .getLoaded();

            // Instantiate the proxy
            return proxyClass.getDeclaredConstructor().newInstance();
        } catch (Throwable t) {
            throw new RuntimeException("Failed to create proxy for " + javaSuperClass, t);
        }
    }

    private static Object getDefaultReturnValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0f;
        if (type == double.class) return 0d;
        return null;
    }
}
