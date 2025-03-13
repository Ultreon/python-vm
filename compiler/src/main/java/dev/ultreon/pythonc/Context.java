package dev.ultreon.pythonc;

import org.objectweb.asm.Type;

public interface Context {

    boolean needsPop();
    void push(Type type);
    Type pop();

    Type peek();

    int stackSize();

    default void pop(Type type) {
        Type pop = pop();
        if (!pop.equals(type)) throw new RuntimeException("Expected " + type + " but got " + pop);
    }
}
