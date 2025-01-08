package dev.ultreon.pythonc;

interface JvmClass extends Symbol {
    String className();

    boolean isInterface();

    boolean isAbstract();

    boolean isEnum();

    boolean doesInherit(Class<?> type);
}
