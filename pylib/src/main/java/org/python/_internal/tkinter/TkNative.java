package org.python._internal.tkinter;

import java.util.ServiceLoader;

public interface TkNative {
    static TkNative factory() {
        synchronized (TkNative.class) {
            if (TkNativeHolder.instance != null) {
                return TkNativeHolder.instance;
            }
            ServiceLoader<TkNative> loader = ServiceLoader.load(TkNative.class);
            return TkNativeHolder.instance = loader.iterator().next();
        }
    }
}
