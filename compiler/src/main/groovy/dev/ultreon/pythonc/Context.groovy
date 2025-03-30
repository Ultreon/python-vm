package dev.ultreon.pythonc

import org.objectweb.asm.Type

import static java.util.Objects.equals

interface Context {
    boolean isPopNeeded()
    void push(Type type)
    Type pop()
    Type peek()
    int getStackSize()

    default void popContext(PythonCompiler compiler) {
        compiler.popContext()
    }

    default void pop(Type type) {
        Type pop = pop()
        if (!equals(pop.sort, type.sort)) {
            throw new RuntimeException("Expected $type but got $pop")
        }
    }
}
