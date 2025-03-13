package dev.ultreon.pythonc;

public interface JvmClass extends Symbol {
    String className();

    boolean isInterface();

    boolean isAbstract();

    boolean isEnum();

    boolean doesInherit(Class<?> type);
}
