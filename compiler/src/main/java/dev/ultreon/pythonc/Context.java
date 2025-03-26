package dev.ultreon.pythonc;

import org.objectweb.asm.Type;

import java.util.Objects;

public interface Context {

    boolean needsPop();
    void push(Type type);
    Type pop();

    Type peek();

    int stackSize();

    default void pop(Type type) {
        Type pop = pop();
        if (!Objects.equals(pop.getSort(), type.getSort())) {
            throw new RuntimeException("Expected " + type + " but got " + pop);
        }
    }
}
