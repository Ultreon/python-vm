package org.python._internal;

import org.python.builtins.TypeError;

import java.lang.invoke.MethodHandles;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class MetaDataManager {
    static Map<Object, MetaData> metaMap = new WeakHashMap<>();
    static Map<Object, PyClass> classMap = new WeakHashMap<>();

    private MetaDataManager() {
        // No initialization needed.
    }

    public static MetaData meta(Object obj) {
        if (obj == null) return null;
        MetaData metaData = metaMap.get(obj);

        if (metaData == null) {
            PyClass pyClass = PyClass.create(obj.getClass());
            classMap.put(obj, pyClass);

            metaData = new MetaData();
            metaMap.put(obj, metaData);
            init(obj, metaData, pyClass);

            if (obj instanceof Class<?> type) {
                initializeClass(type);
            }

            switch (obj) {
                case Class<?> type when !isPyObject(type) -> jClassInit(obj, type, metaData);
                case Class<?> type when (isPyObject(type) || isPyModule(type)) -> pyModuleInit(obj, type, metaData);
                case Class<?> type -> pyClassInit(obj, type, metaData);
                case PyObject object -> pyObjectInit(obj, object, metaData);
                default -> jObjectInit(obj, metaData);
            }
        }
        return metaData;
    }

    private static boolean isPyModule(Class<?> aClass) {
        return PyModule.class.isAssignableFrom(aClass);
    }

    private static boolean isPyObject(Class<?> type) {
        return PyObject.class.isAssignableFrom(type);
    }

    private static void jObjectInit(Object obj, MetaData metaData) {
        Map<String, List<Method>> methods = ClassUtils.methodsByName(obj.getClass());
        for (Map.Entry<String, List<Method>> entry : methods.entrySet()) {
            metaData.set(entry.getKey(), PyFunction.fromMethods(obj.getClass(), obj, entry.getValue()));
        }

        for (Field field : obj.getClass().getFields()) {
            if (field.isSynthetic()) {
                continue;
            }
            metaData.set(field.getName(), field);
        }
    }

    private static void pyObjectInit(Object obj, PyObject pyObject, MetaData metaData) {
        Map<String, List<Method>> methods = ClassUtils.methodsByName(pyObject.getClass());
        for (Map.Entry<String, List<Method>> entry : methods.entrySet()) {
            if (!entry.getKey().startsWith("-def-")) {
                List<Method> list = entry.getValue().stream().filter(method -> method.getDeclaringClass().isAssignableFrom(PyObject.class)).toList();
                if (list.isEmpty()) continue;

                PyFunction value = PyFunction.fromMethodsNoJava(obj.getClass(), obj, entry.getValue());
                if (value == null) break;
                metaData.set(entry.getKey(), value);
                continue;
            }
            Object value = PyFunction.fromMethodsNoJava(obj.getClass(), obj, entry.getValue());
            if (value == null) break;
            metaData.set(entry.getKey().substring(6), value);
        }
    }

    private static void pyClassInit(Object obj, Class<?> aClass, MetaData metaData) {
        Map<String, List<Method>> methods = ClassUtils.methodsByName(aClass);
        for (Map.Entry<String, List<Method>> entry : methods.entrySet()) {
            metaData.set(entry.getKey(), PyFunction.fromMethods(aClass, obj, entry.getValue()));
        }

        for (Field field : obj.getClass().getFields()) {
            if (field.isSynthetic()) {
                continue;
            }
            metaData.set(field.getName(), field);
        }
    }

    private static void pyModuleInit(Object obj, Class<?> aClass, MetaData metaData) {
        Map<String, List<Method>> methods = ClassUtils.methodsByName(aClass);
        for (String key : methods.keySet()) {
            List<Method> value = methods.get(key);
            System.out.println(key + " = " + value);
            if (!key.startsWith("-def-")) {
                List<Method> list = value.stream().filter(method -> method.getDeclaringClass().isAssignableFrom(PyObject.class)).toList();
                if (list.isEmpty()) continue;

                Object v = PyFunction.fromMethodsNoJava(aClass, obj, value);
                if (v == null) continue;
                metaData.set(key, v);
                continue;
            }
            Object v = PyFunction.fromMethodsNoJava(aClass, obj, value);
            if (v == null) continue;
            metaData.set(key.substring(6), v);
        }
    }

    private static void jClassInit(Object obj, Class<?> aClass, MetaData metaData) {
        Map<String, List<Method>> methods = ClassUtils.methodsByName(aClass);
        for (Map.Entry<String, List<Method>> entry : methods.entrySet()) {
            metaData.set(entry.getKey(), PyFunction.fromMethods(aClass, obj, entry.getValue()));
        }

        for (Field field : obj.getClass().getFields()) {
            if (field.isSynthetic()) {
                continue;
            }

            metaData.set(field.getName(), field);
        }
    }

    @SuppressWarnings("SpellCheckingInspection")
    private static void init(Object obj, MetaData metaData, PyClass pyClass) {
        metaData.set("__class__", pyClass);
        metaData.set("__jclass__", obj.getClass());
        metaData.set("__dict__", metaData.dict());
        metaData.set("__module__", obj.getClass().getPackage().getName());
        metaData.set("__name__", obj.getClass().getSimpleName());
        metaData.set("__qualname__", obj.getClass().getName());
        metaData.set("__doc__", obj.getClass().getPackage().getName());
        metaData.set("__weakref__", new WeakReference<>(obj));
        metaData.set("__dir__", metaData.dir());
        metaData.set("__jpackage__", obj.getClass().getPackage());
        metaData.set("__jclass__", obj.getClass());
        metaData.set("__jname__", obj.getClass().getSimpleName());
        metaData.set("__jqualname__", obj.getClass().getName());
        metaData.set("__jdoc__", "");
        metaData.set("__jmodifiers__", obj.getClass().getModifiers());
        metaData.set("__jannotations__", obj.getClass().getAnnotations());
        metaData.set("__jtypeparameters__", obj.getClass().getTypeParameters());
        metaData.set("__jenclosingclass__", obj.getClass().getEnclosingClass());
        metaData.set("__jenclosingmethod__", obj.getClass().getEnclosingMethod());
        metaData.set("__jenclosingconstructor__", obj.getClass().getEnclosingConstructor());
    }

    private static void initializeClass(Class<?> aClass) {
        try {
            MethodHandles.lookup().ensureInitialized(aClass);
        } catch (IllegalAccessException _) {
            throw new TypeError("cannot access class " + aClass.getName());
        }
    }

    public static void dynamic(PyObject pyClass) {
        MetaData metaData = MetaDataManager.meta(pyClass);
        if (metaData == null) return;

        Method[] methods = pyClass.getClass().getMethods();
        for (Method method : methods) {
            PyProperty.Getter getter = method.getAnnotation(PyProperty.Getter.class);
            if (getter != null) {
                addGetter(metaData, method, getter);
            }

            PyProperty.Setter setter = method.getAnnotation(PyProperty.Setter.class);
            if (setter != null) {
                addSetter(metaData, method, setter);
            }
        }
    }

    private static void addSetter(MetaData data, Method method, PyProperty.Setter annotation) {
        data.set("#" + annotation.value() + ":setter", method);
    }

    private static void addGetter(MetaData data, Method method, PyProperty.Getter annotation) {
        data.set("#" + annotation.value() + ":getter", method);
    }
}
