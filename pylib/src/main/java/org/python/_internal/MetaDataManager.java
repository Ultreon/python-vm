package org.python._internal;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class MetaDataManager {
    static Map<Object, MetaData> weakHashMap = new WeakHashMap<>();

    public static MetaData meta(Object obj) {
        MetaData metaData = weakHashMap.get(obj);
        if (metaData == null) {
            metaData = new MetaData();
            metaData.set("__class__", obj.getClass());
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

            Map<String, List<Method>> methods = ClassUtils.methodsByName(obj.getClass());
            for (Map.Entry<String, List<Method>> entry : methods.entrySet()) {
                metaData.set(entry.getKey(), PyFunction.fromMethods(entry.getValue()));
            }

            for (Field field : obj.getClass().getFields()) {
                if (field.isSynthetic()) {
                    continue;
                }
                metaData.set(field.getName(), field);
            }

            weakHashMap.put(obj, metaData);
        }
        return metaData;
    }
}
